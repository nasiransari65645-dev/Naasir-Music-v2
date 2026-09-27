package com.example

import com.example.model.SpatialEnvironment
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun spatialEnvironment_presetsMatchSpecifications() {
        val environments = SpatialEnvironment.values()
        assertEquals(7, environments.size)

        val names = environments.map { it.name }
        assertTrue(names.contains("STUDIO_DRY"))
        assertTrue(names.contains("ACOUSTIC_ROOM"))
        assertTrue(names.contains("LIVE_STAGE"))
        assertTrue(names.contains("CONCERT_HALL"))
        assertTrue(names.contains("GREAT_HALL"))
        assertTrue(names.contains("MEGA_STADIUM"))
        assertTrue(names.contains("ECHO_CHAMBER"))

        assertEquals("Studio Dry", SpatialEnvironment.STUDIO_DRY.displayName)
        assertEquals("Acoustic Room", SpatialEnvironment.ACOUSTIC_ROOM.displayName)
        assertEquals("Live Stage", SpatialEnvironment.LIVE_STAGE.displayName)
        assertEquals("Concert Hall", SpatialEnvironment.CONCERT_HALL.displayName)
        assertEquals("Cathedral", SpatialEnvironment.GREAT_HALL.displayName)
        assertEquals("Large Arena", SpatialEnvironment.MEGA_STADIUM.displayName)
        assertEquals("Echo Chamber", SpatialEnvironment.ECHO_CHAMBER.displayName)
    }
}
