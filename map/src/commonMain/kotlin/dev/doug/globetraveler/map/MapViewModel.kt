package dev.doug.globetraveler.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.DeviceLocationRepository
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.MapPack
import dev.doug.globetraveler.domain.MapPackRepository
import dev.doug.globetraveler.domain.RegionCode
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.TrackedMap
import dev.doug.globetraveler.domain.TrackedMapId
import dev.doug.globetraveler.domain.TrackedMapRepository
import dev.doug.globetraveler.domain.Visit
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

val US_STATES_MAP_ID = MapId("us-states")

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val mapPackRepository: MapPackRepository,
    private val visitRepository: VisitRepository,
    private val trackedMapRepository: TrackedMapRepository,
    deviceLocationRepository: DeviceLocationRepository,
) : ViewModel() {

    private val log = Logger.withTag("MapViewModel")

    private val pack = MutableStateFlow<MapPack?>(null)
    private val detailsCode = MutableStateFlow<String?>(null)

    // combine() waits for every flow's first emission; the location stream may never emit
    // (offline), so it must open with an explicit "unknown" value.
    private val userLocation = deviceLocationRepository.observeLocation()
        .map<ApproximateLocation, ApproximateLocation?> { it }
        .onStart { emit(null) }

    private val activeMap = trackedMapRepository.observeActiveMap()

    // Switching maps re-keys the visits stream; geometry never reloads.
    private val visits = activeMap.flatMapLatest { visitRepository.observeVisits(it.id) }

    private val mapRows = combine(
        trackedMapRepository.observeMaps(),
        visitRepository.observeVisitCounts().onStart { emit(emptyMap()) },
    ) { maps, counts ->
        maps.map { MapRow(it, counts[it.id] ?: 0) }
    }

    val state: StateFlow<MapUiState> = combine(
        pack,
        combine(activeMap, mapRows, ::Pair),
        visits,
        detailsCode,
        userLocation,
    ) { loaded, (active, rows), visits, details, location ->
        buildState(loaded, active, rows, visits, details, location)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), MapUiState())

    init {
        viewModelScope.launch {
            pack.value = mapPackRepository.load(US_STATES_MAP_ID)
        }
    }

    fun onRegionTapped(code: String) {
        val regionId = regionId(code) ?: return
        log.d { "toggling region $code" }
        viewModelScope.launch { visitRepository.toggle(regionId) }
    }

    fun onRegionLongPressed(code: String) {
        log.d { "opening details for $code" }
        detailsCode.value = code
    }

    fun onMapSelected(id: TrackedMapId) {
        log.d { "switching to map ${id.value}" }
        viewModelScope.launch { trackedMapRepository.setActive(id) }
    }

    fun onMapCreated(name: String) {
        log.d { "creating map $name" }
        viewModelScope.launch { trackedMapRepository.create(name) }
    }

    fun onDetailsSave(visitedAt: LocalDate?, notes: String?) {
        val details = state.value.details ?: return
        val regionId = regionId(details.region.code.value) ?: return
        viewModelScope.launch {
            visitRepository.updateDetails(
                Visit(
                    regionId = regionId,
                    visitedAt = visitedAt,
                    notes = notes?.takeIf { it.isNotBlank() },
                    markedAt = details.visit?.markedAt ?: Instant.DISTANT_PAST,
                ),
            )
            detailsCode.value = null
        }
    }

    fun onDetailsRemoveVisit() {
        val details = state.value.details ?: return
        val regionId = regionId(details.region.code.value) ?: return
        viewModelScope.launch {
            visitRepository.toggle(regionId)
            detailsCode.value = null
        }
    }

    fun onDetailsDismissed() {
        detailsCode.value = null
    }

    private fun regionId(code: String): RegionId? =
        state.value.activeMap?.let { RegionId(it.id, RegionCode(code)) }

    private fun buildState(
        pack: MapPack?,
        activeMap: TrackedMap,
        mapRows: List<MapRow>,
        visits: List<Visit>,
        detailsCode: String?,
        userLocation: ApproximateLocation?,
    ): MapUiState {
        if (pack == null) {
            return MapUiState(
                userLocation = userLocation,
                activeMap = activeMap,
                maps = mapRows.toImmutableList(),
            )
        }
        val descriptor = pack.descriptor
        return MapUiState(
            userLocation = userLocation,
            loading = false,
            mapName = descriptor.name,
            cameraDefaults = descriptor.camera,
            totalCount = descriptor.regions.size,
            activeMap = activeMap,
            maps = mapRows.toImmutableList(),
            visitedCodes = visits.map { it.regionId.code.value }.toImmutableSet(),
            geometryGeoJson = pack.geometryGeoJson,
            details = detailsCode?.let { code ->
                descriptor.regions.firstOrNull { it.code.value == code }?.let { region ->
                    RegionDetails(
                        region = region,
                        visit = visits.firstOrNull { it.regionId.code.value == code },
                    )
                }
            },
        )
    }
}
