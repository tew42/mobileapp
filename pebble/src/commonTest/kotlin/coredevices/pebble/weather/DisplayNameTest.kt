package coredevices.pebble.weather

import coredevices.pebble.ui.displayName
import dev.jordond.compass.Coordinates
import dev.jordond.compass.Place
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayNameTest {

    private fun place(
        locality: String? = null,
        administrativeArea: String? = null,
        country: String? = null,
    ) = Place(
        coordinates = Coordinates(52.2689, 10.5268),
        name = null,
        street = null,
        isoCountryCode = null,
        country = country,
        postalCode = null,
        administrativeArea = administrativeArea,
        subAdministrativeArea = null,
        locality = locality,
        subLocality = null,
        thoroughfare = null,
        subThoroughfare = null,
    )

    @Test
    fun namesTheRegionAlongsideThePlace() {
        assertEquals("Braunschweig, Lower Saxony", place("Braunschweig", "Lower Saxony").displayName())
    }

    /** displayName appends the administrative area, which usefulName can itself return. */
    @Test
    fun doesNotRepeatTheRegion() {
        assertEquals("Lower Saxony", place(null, "Lower Saxony").displayName())
    }

    /** An empty name reaches the watch as a record it rejects outright. */
    @Test
    fun neverReturnsBlank() {
        assertEquals("Unknown location", place().displayName())
        assertEquals("Unknown location", place("", "", "").displayName())
    }
}
