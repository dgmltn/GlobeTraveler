package dev.doug.globetraveler.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.doug.globetraveler.domain.MapAccent
import dev.doug.globetraveler.domain.RegionCode
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.TrackedMapId
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

class TrackedMapRepositoryImplTest {

    private val database = Room.inMemoryDatabaseBuilder<GlobeDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    private val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            File.createTempFile("globe-test", ".preferences_pb").absolutePath.toPath()
        },
    )
    private val repository = TrackedMapRepositoryImpl(
        dao = database.trackedMapDao(),
        visitDao = database.visitDao(),
        dataStore = dataStore,
        clock = FixedClock(Instant.fromEpochMilliseconds(1_000)),
    )

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun `seeds a default Visited map on first observe`() = runTest {
        val maps = repository.observeMaps().first()

        assertEquals(1, maps.size)
        assertEquals("Visited", maps.single().name)
        assertEquals(MapAccent.Green, maps.single().accent)
    }

    @Test
    fun `active map falls back to the oldest when nothing selected`() = runTest {
        assertEquals("Visited", repository.observeActiveMap().first().name)
    }

    @Test
    fun `create assigns an unused accent and becomes active`() = runTest {
        val plates = repository.create("License plates")

        assertNotEquals(MapAccent.Green, plates.accent)
        assertEquals(plates.id, repository.observeActiveMap().first().id)
        assertEquals(listOf("Visited", "License plates"), repository.observeMaps().first().map { it.name })
    }

    @Test
    fun `setActive switches the active map`() = runTest {
        val plates = repository.create("License plates")
        val visited = repository.observeMaps().first().first()

        repository.setActive(visited.id)
        assertEquals(visited.id, repository.observeActiveMap().first().id)

        repository.setActive(plates.id)
        assertEquals(plates.id, repository.observeActiveMap().first().id)
    }

    @Test
    fun `dangling active selection falls back to the oldest map`() = runTest {
        repository.setActive(TrackedMapId("deleted-map"))

        assertEquals("Visited", repository.observeActiveMap().first().name)
    }

    @Test
    fun `create trims the name`() = runTest {
        assertEquals("Plates", repository.create("  Plates  ").name)
    }

    @Test
    fun `rename persists and trims`() = runTest {
        val plates = repository.create("Plates")

        repository.rename(plates.id, "  License plates ")

        val renamed = repository.observeMaps().first().single { it.id == plates.id }
        assertEquals("License plates", renamed.name)
    }

    @Test
    fun `delete removes the map and its visits`() = runTest {
        val plates = repository.create("Plates")
        val visitRepository = VisitRepositoryImpl(database.visitDao(), FixedClock(Instant.fromEpochMilliseconds(1_000)))
        visitRepository.toggle(RegionId(plates.id, RegionCode("CA")))
        visitRepository.observeVisits(plates.id).first { it.isNotEmpty() }

        repository.delete(plates.id)

        assertEquals(listOf("Visited"), repository.observeMaps().first().map { it.name })
        assertEquals(emptyList(), visitRepository.observeVisits(plates.id).first())
    }

    @Test
    fun `deleting the active map falls back to the oldest`() = runTest {
        val plates = repository.create("Plates")
        assertEquals(plates.id, repository.observeActiveMap().first().id)

        repository.delete(plates.id)

        assertEquals("Visited", repository.observeActiveMap().first().name)
    }

    @Test
    fun `deleting the last map is a no-op`() = runTest {
        val visited = repository.observeMaps().first().single()

        repository.delete(visited.id)

        assertEquals(listOf(visited), repository.observeMaps().first())
    }
}
