package io.rebble.libpebblecommon.util

import kotlin.test.Test
import kotlin.test.assertEquals

class GeolocationErrorTest {

    @Test
    fun codesMatchTheW3cGeolocationPositionError() {
        assertEquals(1, GeolocationError.PermissionDenied.code)
        assertEquals(2, GeolocationError.PositionUnavailable.code)
        assertEquals(3, GeolocationError.Timeout.code)
    }
}
