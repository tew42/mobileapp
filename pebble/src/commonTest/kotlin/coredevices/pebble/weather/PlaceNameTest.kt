package coredevices.pebble.weather

import dev.jordond.compass.Coordinates
import dev.jordond.compass.Place
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaceNameTest {

    private fun place(
        locality: String? = null,
        subAdministrativeArea: String? = null,
        administrativeArea: String? = null,
        street: String? = null,
    ) = Place(
        coordinates = Coordinates(52.2689, 10.5268),
        name = null,
        street = street,
        isoCountryCode = "DE",
        country = "Germany",
        postalCode = null,
        administrativeArea = administrativeArea,
        subAdministrativeArea = subAdministrativeArea,
        locality = locality,
        subLocality = null,
        thoroughfare = null,
        subThoroughfare = null,
    )

    @Test
    fun deviceFixPrefersAdministrativeAreasOverStreet() {
        assertEquals("Braunschweig", place("Braunschweig", "Wolfenbüttel", "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Wolfenbüttel", place(null, "Wolfenbüttel", "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Lower Saxony", place(null, null, "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Bohlweg", place(null, null, null, "Bohlweg").usefulName())
        assertNull(place().usefulName())
    }

    @Test
    fun searchResultKeepsStreetAheadOfAdministrativeAreas() {
        assertEquals("Bohlweg", place(null, "Wolfenbüttel", "Lower Saxony", "Bohlweg").searchResultName())
        assertEquals("Braunschweig", place("Braunschweig", "Wolfenbüttel", "Lower Saxony", "Bohlweg").searchResultName())
        assertEquals("Lower Saxony", place(null, null, "Lower Saxony", null).searchResultName())
    }

    @Test
    fun paddingIsTrimmedOff() {
        assertEquals("Braunschweig", place("  Braunschweig  ").usefulName())
        assertEquals("Bohlweg", place(null, null, null, " Bohlweg ").searchResultName())
    }

    /** A blank name makes the watch reject the whole weather record, so blanks must not win. */
    @Test
    fun blanksAreSkippedNotReturned() {
        assertEquals("Lower Saxony", place("", "  ", "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Bohlweg", place("", "", "", "Bohlweg").searchResultName())
        assertNull(place("", "", "", "").usefulName())
    }
}
