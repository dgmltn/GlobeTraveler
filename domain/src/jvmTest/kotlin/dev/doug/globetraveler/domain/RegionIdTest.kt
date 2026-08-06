package dev.doug.globetraveler.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RegionIdTest {

    @Test
    fun `region ids with same map and code are equal`() {
        val a = RegionId(MapId("us-states"), RegionCode("CA"))
        val b = RegionId(MapId("us-states"), RegionCode("CA"))
        assertEquals(a, b)
    }

    @Test
    fun `region ids differ across maps`() {
        val usa = RegionId(MapId("us-states"), RegionCode("CA"))
        val canada = RegionId(MapId("ca-provinces"), RegionCode("CA"))
        assertNotEquals(usa, canada)
    }

    @Test
    fun `value classes expose raw values`() {
        assertEquals("us-states", MapId("us-states").value)
        assertEquals("WY", RegionCode("WY").value)
    }
}
