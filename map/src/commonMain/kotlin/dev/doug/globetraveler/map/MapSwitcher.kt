package dev.doug.globetraveler.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.doug.globetraveler.design.GlobeTheme
import dev.doug.globetraveler.design.accentColors
import dev.doug.globetraveler.domain.MapAccent
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.TrackedMap
import dev.doug.globetraveler.domain.TrackedMapId
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The top-center title: active map name + count in the map's accent color. Tapping opens
 * the switcher menu with one row per map and a "New map…" entry.
 */
@Composable
fun MapSwitcher(
    activeMap: TrackedMap,
    maps: ImmutableList<MapRow>,
    visitedCount: Int,
    totalCount: Int,
    onMapSelected: (TrackedMapId) -> Unit,
    onMapCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val accent = activeMap.accent.accentColors(isSystemInDarkTheme()).fill

    Box(modifier) {
        Text(
            text = "${activeMap.name} · $visitedCount/$totalCount ▾",
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable { menuOpen = true }
                .padding(horizontal = 8.dp),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            maps.forEach { row ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${row.map.name} · ${row.visitedCount}/$totalCount",
                            fontWeight = if (row.map.id == activeMap.id) FontWeight.Bold else null,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onMapSelected(row.map.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("New map…") },
                onClick = {
                    menuOpen = false
                    creating = true
                },
            )
        }
    }
    if (creating) {
        NewMapDialog(
            onCreate = { name ->
                creating = false
                onMapCreated(name)
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
fun NewMapDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New map") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private val PREVIEW_VISITED = TrackedMap(
    id = TrackedMapId("visited"),
    packId = MapId("us-states"),
    name = "Visited",
    accent = MapAccent.Green,
    createdAt = Instant.DISTANT_PAST,
)

private val PREVIEW_PLATES = TrackedMap(
    id = TrackedMapId("plates"),
    packId = MapId("us-states"),
    name = "License plates",
    accent = MapAccent.Blue,
    createdAt = Instant.DISTANT_PAST,
)

@Preview
@Composable
private fun Preview_MapSwitcher() {
    GlobeTheme {
        MapSwitcher(
            activeMap = PREVIEW_VISITED,
            maps = persistentListOf(MapRow(PREVIEW_VISITED, 12), MapRow(PREVIEW_PLATES, 34)),
            visitedCount = 12,
            totalCount = 50,
            onMapSelected = {},
            onMapCreated = {},
        )
    }
}

@Preview
@Composable
private fun Preview_MapSwitcher_SecondMapActive() {
    GlobeTheme {
        MapSwitcher(
            activeMap = PREVIEW_PLATES,
            maps = persistentListOf(MapRow(PREVIEW_VISITED, 12), MapRow(PREVIEW_PLATES, 34)),
            visitedCount = 34,
            totalCount = 50,
            onMapSelected = {},
            onMapCreated = {},
        )
    }
}

@Preview
@Composable
private fun Preview_NewMapDialog() {
    GlobeTheme {
        NewMapDialog(onCreate = {}, onDismiss = {})
    }
}
