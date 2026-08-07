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
import dev.doug.globetraveler.domain.Visit
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

val US_STATES_MAP_ID = MapId("us-states")

class MapViewModel(
    private val mapPackRepository: MapPackRepository,
    private val visitRepository: VisitRepository,
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

    val state: StateFlow<MapUiState> = combine(
        pack,
        visitRepository.observeVisits(US_STATES_MAP_ID),
        detailsCode,
        userLocation,
    ) { loaded, visits, details, location ->
        buildState(loaded, visits, details, location)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), MapUiState())

    init {
        viewModelScope.launch {
            pack.value = mapPackRepository.load(US_STATES_MAP_ID)
        }
    }

    fun onRegionTapped(code: String) {
        log.d { "toggling region $code" }
        viewModelScope.launch { visitRepository.toggle(regionId(code)) }
    }

    fun onRegionLongPressed(code: String) {
        log.d { "opening details for $code" }
        detailsCode.value = code
    }

    fun onDetailsSave(visitedAt: LocalDate?, notes: String?) {
        val details = state.value.details ?: return
        val regionId = regionId(details.region.code.value)
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
        viewModelScope.launch {
            visitRepository.toggle(regionId(details.region.code.value))
            detailsCode.value = null
        }
    }

    fun onDetailsDismissed() {
        detailsCode.value = null
    }

    private fun regionId(code: String) = RegionId(US_STATES_MAP_ID, RegionCode(code))

    private fun buildState(
        pack: MapPack?,
        visits: List<Visit>,
        detailsCode: String?,
        userLocation: ApproximateLocation?,
    ): MapUiState {
        if (pack == null) return MapUiState(userLocation = userLocation)
        val descriptor = pack.descriptor
        return MapUiState(
            userLocation = userLocation,
            loading = false,
            mapName = descriptor.name,
            cameraDefaults = descriptor.camera,
            totalCount = descriptor.regions.size,
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
