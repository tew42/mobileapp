package coredevices.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionTest {

    @Test
    fun locationIsSatisfiedByEitherGrant() {
        assertTrue(Permission.Location.partialGrantSuffices)
    }

    @Test
    fun preciseLocationNeedsItsOwnGrant() {
        assertFalse(Permission.PreciseLocation.partialGrantSuffices)
    }

    @Test
    fun everyOtherPermissionNeedsAllItsGrants() {
        Permission.entries
            .filter { it != Permission.Location }
            .forEach { assertFalse(it.partialGrantSuffices, "$it") }
    }

    /** Approximate location: coarse granted, fine denied. */
    @Test
    fun approximateGrantSatisfiesLocation() {
        assertTrue(Permission.Location.isGrantedBy(listOf(true, false)))
        assertTrue(Permission.Location.isGrantedBy(listOf(false, true)))
    }

    @Test
    fun noLocationGrantAtAllDoesNotSatisfyLocation() {
        assertFalse(Permission.Location.isGrantedBy(listOf(false, false)))
    }

    @Test
    fun aPartialGrantDoesNotSatisfyAnythingElse() {
        assertFalse(Permission.Calendar.isGrantedBy(listOf(true, false)))
        assertTrue(Permission.Calendar.isGrantedBy(listOf(true, true)))
    }

    /** Several permissions map to nothing on some API levels; that is satisfied, not missing. */
    @Test
    fun nothingToGrantIsSatisfied() {
        Permission.entries.forEach { assertTrue(it.isGrantedBy(emptyList()), "$it") }
    }
}
