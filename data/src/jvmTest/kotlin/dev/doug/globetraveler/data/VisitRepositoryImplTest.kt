package dev.doug.globetraveler.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.doug.globetraveler.domain.TrackedMapId
import dev.doug.globetraveler.domain.RegionCode
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.Visit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

private class MutableClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

class VisitRepositoryImplTest {

    private val database = Room.inMemoryDatabaseBuilder<GlobeDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    private val clock = MutableClock(Instant.fromEpochMilliseconds(1_000))
    private val repository = VisitRepositoryImpl(database.visitDao(), clock)

    private val mapId = TrackedMapId("visited")
    private val california = RegionId(mapId, RegionCode("CA"))

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `toggle inserts a visit stamped with clock now`() = runTest {
        repository.toggle(california)

        val visits = repository.observeVisits(mapId).first { it.isNotEmpty() }
        assertEquals(1, visits.size)
        assertEquals(california, visits.single().regionId)
        assertEquals(Instant.fromEpochMilliseconds(1_000), visits.single().markedAt)
        assertNull(visits.single().visitedAt)
        assertNull(visits.single().notes)
    }

    @Test
    fun `toggling twice removes the visit`() = runTest {
        repository.toggle(california)
        repository.toggle(california)

        assertEquals(emptyList(), repository.observeVisits(mapId).first())
    }

    @Test
    fun `updateDetails stores date and notes preserving markedAt`() = runTest {
        repository.toggle(california)
        clock.now = Instant.fromEpochMilliseconds(2_000)

        repository.updateDetails(
            Visit(
                regionId = california,
                visitedAt = LocalDate(2024, 6, 15),
                notes = "Road trip",
                markedAt = clock.now(),
            ),
        )

        val visit = repository.observeVisits(mapId).first { it.isNotEmpty() }.single()
        assertEquals(LocalDate(2024, 6, 15), visit.visitedAt)
        assertEquals("Road trip", visit.notes)
        assertEquals(Instant.fromEpochMilliseconds(1_000), visit.markedAt)
    }

    @Test
    fun `updateDetails on an unvisited region creates the visit`() = runTest {
        repository.updateDetails(
            Visit(
                regionId = california,
                visitedAt = LocalDate(2023, 1, 2),
                notes = null,
                markedAt = clock.now(),
            ),
        )

        val visits = repository.observeVisits(mapId).first { it.isNotEmpty() }
        assertEquals(LocalDate(2023, 1, 2), visits.single().visitedAt)
    }

    @Test
    fun `observeVisits filters by map`() = runTest {
        repository.toggle(california)
        repository.toggle(RegionId(TrackedMapId("plates"), RegionCode("CA")))

        val visits = repository.observeVisits(mapId).first { it.isNotEmpty() }
        assertEquals(listOf(california), visits.map { it.regionId })
    }

    @Test
    fun `observeVisitCounts groups by tracked map`() = runTest {
        repository.toggle(california)
        repository.toggle(RegionId(mapId, RegionCode("OR")))
        repository.toggle(RegionId(TrackedMapId("plates"), RegionCode("CA")))

        val counts = repository.observeVisitCounts().first { it.isNotEmpty() }
        assertEquals(2, counts[mapId])
        assertEquals(1, counts[TrackedMapId("plates")])
    }
}
