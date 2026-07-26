package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Test

class MazeMilestoneTest {

    @Test
    fun intervalZero_neverTriggers() {
        assertEquals(false, crossedMazeMilestone(4, 6, 0))
        assertEquals(false, crossedMazeMilestone(1, 100, 0))
    }

    @Test
    fun negativeInterval_neverTriggers() {
        assertEquals(false, crossedMazeMilestone(4, 5, -1))
    }

    @Test
    fun crossingAMultiple_triggers() {
        assertEquals(true, crossedMazeMilestone(4, 5, 5))
        assertEquals(true, crossedMazeMilestone(4, 6, 5))  // skipping straight over 5 still counts
        assertEquals(true, crossedMazeMilestone(9, 10, 5))
    }

    @Test
    fun notCrossingAMultiple_doesNotTrigger() {
        assertEquals(false, crossedMazeMilestone(1, 2, 5))
        assertEquals(false, crossedMazeMilestone(5, 5, 5))  // no level change
        assertEquals(false, crossedMazeMilestone(6, 9, 5))
    }

    @Test
    fun intervalOne_triggersEveryLevel() {
        assertEquals(true, crossedMazeMilestone(1, 2, 1))
        assertEquals(true, crossedMazeMilestone(7, 8, 1))
    }
}
