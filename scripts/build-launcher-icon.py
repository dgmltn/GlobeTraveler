"""Convert the world-map launcher SVG into Android adaptive-icon vector drawables.

VectorDrawable has no <circle> and no stroke-dasharray, so the map dots and the dashed
route both become explicit path geometry. Group transforms are baked into coordinates,
the artwork is fitted to the 66dp adaptive-icon safe zone, and the dot field is split
across several <path> elements because AAPT2 caps a resource string at 32767 bytes.

Usage: python3 scripts/build-launcher-icon.py [path/to/world-map-icon.svg]
"""

import math
import re
import sys

SRC = sys.argv[1] if len(sys.argv) > 1 else 'resources/world-map-icon.svg'
OUT = 'app-android/src/main/res'
SAFE_R = 33.0        # 66dp guaranteed-safe circle on the 108dp adaptive canvas
DARK = '#1B2130'     # matches the background layer, so "cutout" fills read as holes

svg = open(SRC).read()


def attr(tag, name, default=None):
    m = re.search(r'%s="([^"]*)"' % name, tag)
    return m.group(1) if m else default


# ---- dot group: bake its transform into each circle ---------------------------
gm = re.search(r'<g\b([^>]*)>(.*?)</g>', svg, re.S)
gt = attr(gm.group(1), 'transform', '') or ''
tx, ty = (0.0, 0.0)
gs = 1.0
if (m := re.search(r'translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\s*\)', gt)):
    tx, ty = float(m.group(1)), float(m.group(2))
if (m := re.search(r'scale\(\s*([-\d.]+)', gt)):
    gs = float(m.group(1))

DOT_FILL = attr(gm.group(1), 'fill', '#8B93A3')
DOT_ALPHA = attr(gm.group(1), 'fill-opacity', '1')
dots = [(tx + gs * float(x), ty + gs * float(y), gs * float(r))
        for x, y, r in re.findall(r'<circle cx="([-\d.]+)" cy="([-\d.]+)" r="([-\d.]+)"', gm.group(2))]

# ---- trailing artwork, in document order --------------------------------------
tail = svg[gm.end():]


def cubic(p0, p1, p2, p3, t):
    u = 1 - t
    return (u*u*u*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t*t*t*p3[0],
            u*u*u*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t*t*t*p3[1])


def dash_to_dots(d, period):
    """A dasharray of ~0 with a round linecap draws dots; recover their centers."""
    nums = [float(v) for v in re.findall(r'[-\d.]+', d)]
    start, rest = (nums[0], nums[1]), nums[2:]
    segs, cur = [], start
    for i in range(0, len(rest), 6):
        pts = [(rest[i], rest[i+1]), (rest[i+2], rest[i+3]), (rest[i+4], rest[i+5])]
        segs.append((cur, *pts))
        cur = pts[2]
    samples, acc, prev = [], 0.0, start
    for seg in segs:
        for i in range(1, 2001):
            pt = cubic(*seg, i / 2000)
            acc += math.hypot(pt[0]-prev[0], pt[1]-prev[1])
            samples.append((acc, pt))
            prev = pt
    out, dist, si = [], 0.0, 0
    while dist <= acc:
        while si < len(samples)-1 and samples[si][0] < dist:
            si += 1
        out.append(samples[si][1])
        dist += period
    return out


shapes = []      # (kind, payload, fill, alpha) in draw order
for m in re.finditer(r'<(circle|path)\b([^>]*)>', tail):
    kind, a = m.group(1), m.group(2)
    fill, alpha = attr(a, 'fill', '#000000'), attr(a, 'fill-opacity', '1')
    if kind == 'circle':
        c = (float(attr(a, 'cx')), float(attr(a, 'cy')), float(attr(a, 'r')))
        shapes.append(('glow' if fill.startswith('url(') else 'circle', c, fill, alpha))
    elif attr(a, 'stroke-dasharray'):
        w = float(attr(a, 'stroke-width', '1'))
        gap = [float(v) for v in re.findall(r'[-\d.]+', attr(a, 'stroke-dasharray'))]
        centers = dash_to_dots(attr(a, 'd'), sum(gap))
        shapes.append(('dots', [(x, y, w/2) for x, y in centers],
                       attr(a, 'stroke'), attr(a, 'stroke-opacity', '1')))
    elif fill != 'none':
        pts = [float(v) for v in re.findall(r'[-\d.]+', attr(a, 'd'))]
        shapes.append(('poly', list(zip(pts[0::2], pts[1::2])), fill, alpha))

# ---- fit every solid mark into the safe circle --------------------------------
solid = list(dots)
for kind, payload, _, _ in shapes:
    if kind == 'circle':
        solid.append(payload)
    elif kind == 'dots':
        solid += payload
    elif kind == 'poly':
        solid += [(x, y, 0.0) for x, y in payload]

cx = (min(x-r for x, y, r in solid) + max(x+r for x, y, r in solid)) / 2
cy = (min(y-r for x, y, r in solid) + max(y+r for x, y, r in solid)) / 2
reach = max(math.hypot(x-cx, y-cy) + r for x, y, r in solid)
S = SAFE_R / reach


def n(v):
    return f'{v:.3f}'.rstrip('0').rstrip('.')


def T(x, y):
    return ((x-cx)*S + 54, (y-cy)*S + 54)


def circle_path(x, y, r):
    x, y = T(x, y)
    r *= S
    return f'M{n(x-r)},{n(y)}a{n(r)},{n(r)} 0 1,0 {n(2*r)},0a{n(r)},{n(r)} 0 1,0 {n(-2*r)},0Z'


def poly_path(pts):
    (x0, y0), *rest = [T(x, y) for x, y in pts]
    return f'M{n(x0)},{n(y0)}' + ''.join(f'L{n(x)},{n(y)}' for x, y in rest) + 'Z'


CHUNK = 120


def dot_paths(marks, color, alpha):
    out = []
    for i in range(0, len(marks), CHUNK):
        data = ''.join(circle_path(*d) for d in marks[i:i+CHUNK])
        assert len(data) < 30000, len(data)
        out.append(f'    <path android:fillColor="{color}" android:fillAlpha="{alpha}"\n'
                   f'        android:pathData="{data}" />')
    return '\n'.join(out)


def hex8(color, opacity):
    return '#%02X%s' % (round(float(opacity) * 255), color.lstrip('#'))


stops = re.findall(r'<stop offset="([\d.]+)" stop-color="(#\w+)" stop-opacity="([\d.]*)"', svg)

body = [dot_paths(dots, DOT_FILL, DOT_ALPHA)]
mono = [dot_paths(dots, '#000000', '0.55')]
for kind, payload, fill, alpha in shapes:
    if kind == 'glow':
        gx, gy = T(payload[0], payload[1])
        items = '\n'.join(
            f'                <item android:offset="{o}" android:color="{hex8(c, op or "1")}" />'
            for o, c, op in stops)
        body.append(f'''    <path android:pathData="{circle_path(*payload)}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial"
                android:centerX="{n(gx)}" android:centerY="{n(gy)}"
                android:gradientRadius="{n(payload[2] * S)}">
{items}
            </gradient>
        </aapt:attr>
    </path>''')
    else:
        data = poly_path(payload) if kind == 'poly' else (
            ''.join(circle_path(*d) for d in payload) if kind == 'dots' else circle_path(*payload))
        av = f' android:fillAlpha="{alpha}"' if alpha not in ('1', None) else ''
        body.append(f'    <path android:fillColor="{fill}"{av}\n        android:pathData="{data}" />')
        if fill.upper() != DARK:                       # cutouts vanish in the mono layer
            mono.append(f'    <path android:fillColor="#000000"\n        android:pathData="{data}" />')

HEAD = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/build-launcher-icon.py from {SRC.split('/')[-1]} - do not hand-edit.
     <circle> and stroke-dasharray have no VectorDrawable equivalent, so the map dots and
     the dashed route ship as path geometry, fitted to the 66dp adaptive-icon safe zone. -->
'''


def vector(paths, extra_ns=''):
    return (HEAD + f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{extra_ns}\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n\n'
            + '\n'.join(paths) + '\n</vector>\n')


open(f'{OUT}/drawable/ic_launcher_foreground.xml', 'w').write(
    vector(body, '\n    xmlns:aapt="http://schemas.android.com/aapt"'))
open(f'{OUT}/drawable/ic_launcher_monochrome.xml', 'w').write(vector(mono))
open(f'{OUT}/values/ic_launcher_background.xml', 'w').write(
    '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
    f'    <color name="ic_launcher_background">{attr(re.search(r"<rect[^>]*>", svg).group(0), "fill")}</color>\n'
    '</resources>\n')

print(f'group transform: translate({tx},{ty}) scale({gs})')
print(f'dots={len(dots)} shapes={[k for k, *_ in shapes]}')
print(f'center=({cx:.1f},{cy:.1f}) reach={reach:.1f} scale={S:.5f} '
      f'-> {(max(x+r for x, y, r in solid)-min(x-r for x, y, r in solid))*S:.1f}dp wide')
