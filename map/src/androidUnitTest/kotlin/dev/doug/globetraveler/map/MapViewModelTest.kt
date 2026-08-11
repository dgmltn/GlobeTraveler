package dev.doug.globetraveler.map

import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.map.fakes.FakeDeviceLocationRepository
import dev.doug.globetraveler.map.fakes.FakeMapPackRepository
import dev.doug.globetraveler.map.fakes.FakeTrackedMapRepository
import dev.doug.globetraveler.map.fakes.FakeVisitRepository
import dev.doug.globetraveler.map.fakes.TEST_GEOMETRY
import dev.doug.globetraveler.map.fakes.TEST_VISITED_MAP
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var locationRepository: FakeDeviceLocationRepository
    private lateinit var trackedMapRepository: FakeTrackedMapRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        locationRepository = FakeDeviceLocationRepository()
        trackedMapRepository = FakeTrackedMapRepository()
        viewModel = MapViewModel(
            FakeMapPackRepository(),
            FakeVisitRepository(),
            trackedMapRepository,
            locationRepository,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun awaitState(predicate: (MapUiState) -> Boolean): MapUiState =
        withTimeout(5.seconds) { viewModel.state.first(predicate) }

    @Test
    fun `loads pack into counts, camera, and static geometry`() = runTest {
        val state = awaitState { !it.loading }

        assertEquals("Test States", state.mapName)
        assertEquals(3, state.totalCount)
        assertEquals(0, state.visitedCount)
        assertEquals(emptySet(), state.visitedCodes)
        assertEquals(39.5, state.cameraDefaults?.latitude)
        assertEquals(TEST_GEOMETRY, state.geometryGeoJson)
    }

    @Test
    fun `user location flows into state and can move`() = runTest {
        val loaded = awaitState { !it.loading }
        assertNull(loaded.userLocation)

        locationRepository.emit(ApproximateLocation(latitude = 33.0, longitude = -117.0))
        val located = awaitState { it.userLocation != null }
        assertEquals(ApproximateLocation(latitude = 33.0, longitude = -117.0), located.userLocation)

        locationRepository.emit(ApproximateLocation(latitude = 45.5, longitude = -122.6))
        awaitState { it.userLocation == ApproximateLocation(latitude = 45.5, longitude = -122.6) }
    }

    @Test
    fun `tap toggles a region visited and back`() = runTest {
        awaitState { !it.loading }

        viewModel.onRegionTapped("CA")
        val visited = awaitState { it.visitedCount == 1 }
        assertEquals(setOf("CA"), visited.visitedCodes)

        viewModel.onRegionTapped("CA")
        val reverted = awaitState { it.visitedCount == 0 }
        assertEquals(emptySet(), reverted.visitedCodes)
    }

    @Test
    fun `geometry is not rebuilt when visits change`() = runTest {
        val initial = awaitState { !it.loading }

        viewModel.onRegionTapped("CA")
        val visited = awaitState { it.visitedCount == 1 }

        assertEquals(initial.geometryGeoJson, visited.geometryGeoJson)
    }

    @Test
    fun `long press exposes details for the region`() = runTest {
        awaitState { !it.loading }

        viewModel.onRegionLongPressed("NV")
        val state = awaitState { it.details != null }
        assertEquals("Nevada", state.details?.region?.name)
        assertNull(state.details?.visit)
    }

    @Test
    fun `saving details persists date and notes`() = runTest {
        awaitState { !it.loading }
        viewModel.onRegionLongPressed("NV")
        awaitState { it.details != null }

        viewModel.onDetailsSave(LocalDate(2025, 3, 9), "Reno weekend")

        val state = awaitState { it.visitedCount == 1 }
        assertEquals(setOf("NV"), state.visitedCodes)
        viewModel.onRegionLongPressed("NV")
        val details = awaitState { it.details?.visit != null }.details
        assertEquals(LocalDate(2025, 3, 9), details?.visit?.visitedAt)
        assertEquals("Reno weekend", details?.visit?.notes)
    }

    @Test
    fun `removing a visit from details clears it and dismisses`() = runTest {
        awaitState { !it.loading }
        viewModel.onRegionTapped("OR")
        awaitState { it.visitedCount == 1 }

        viewModel.onRegionLongPressed("OR")
        awaitState { it.details?.visit != null }
        viewModel.onDetailsRemoveVisit()

        val state = awaitState { it.visitedCount == 0 }
        assertNull(state.details)
    }

    @Test
    fun `default map is active with its accent exposed`() = runTest {
        val state = awaitState { it.activeMap != null }

        assertEquals(TEST_VISITED_MAP, state.activeMap)
        assertEquals(1, state.maps.size)
    }

    @Test
    fun `creating a map switches to it and visits are independent`() = runTest {
        awaitState { !it.loading }
        viewModel.onRegionTapped("CA")
        awaitState { it.visitedCount == 1 }

        viewModel.onMapCreated("License plates")
        val switched = awaitState { it.activeMap?.name == "License plates" }
        assertEquals(0, switched.visitedCount)
        assertEquals(2, switched.maps.size)

        viewModel.onRegionTapped("NV")
        awaitState { it.visitedCodes == setOf("NV") }

        viewModel.onMapSelected(TEST_VISITED_MAP.id)
        val back = awaitState { it.activeMap == TEST_VISITED_MAP }
        awaitState { it.visitedCodes == setOf("CA") }
        assertEquals(TEST_VISITED_MAP, back.activeMap)
    }

    @Test
    fun `switcher rows carry per-map counts`() = runTest {
        awaitState { !it.loading }
        viewModel.onRegionTapped("CA")
        viewModel.onRegionTapped("OR")
        awaitState { it.visitedCount == 2 }

        viewModel.onMapCreated("License plates")
        awaitState { it.activeMap?.name == "License plates" }
        viewModel.onRegionTapped("NV")

        val state = awaitState { rowsByName(it)["License plates"] == 1 }
        assertEquals(2, rowsByName(state)["Visited"])
    }

    private fun rowsByName(state: MapUiState): Map<String, Int> =
        state.maps.associate { it.map.name to it.visitedCount }

    @Test
    fun `dismissing details clears them`() = runTest {
        awaitState { !it.loading }
        viewModel.onRegionLongPressed("CA")
        awaitState { it.details != null }

        viewModel.onDetailsDismissed()
        assertNull(awaitState { it.details == null }.details)
    }
}
