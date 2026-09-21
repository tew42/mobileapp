package coredevices.pebble.weather

import coredevices.pebble.ui.displayName
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
        country: String? = "Germany",
    ) = Place(
        coordinates = Coordinates(52.2689, 10.5268),
        name = null,
        street = street,
        isoCountryCode = "DE",
        country = country,
        postalCode = null,
        administrativeArea = administrativeArea,
        subAdministrativeArea = subAdministrativeArea,
        locality = locality,
        subLocality = null,
        thoroughfare = null,
        subThoroughfare = null,
    )

    @Test
    fun walksOutwardFromLocalityWithStreetLast() {
        assertEquals("Braunschweig", place("Braunschweig", "Wolfenbüttel", "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Wolfenbüttel", place(null, "Wolfenbüttel", "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Lower Saxony", place(null, null, "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Bohlweg", place(null, null, null, "Bohlweg").usefulName())
        assertNull(place().usefulName())
    }

    @Test
    fun skipsBlanksAndTrims() {
        assertEquals("Lower Saxony", place("", "  ", "Lower Saxony", "Bohlweg").usefulName())
        assertEquals("Braunschweig", place("  Braunschweig  ").usefulName())
        assertNull(place("", "", "", "").usefulName())
    }

    @Test
    fun displayNameAppendsRegionOnce() {
        assertEquals("Braunschweig, Lower Saxony", place("Braunschweig", administrativeArea = "Lower Saxony").displayName())
        assertEquals("Lower Saxony", place(administrativeArea = "Lower Saxony").displayName())
        assertEquals("Germany", place().displayName())
        assertEquals("Germany", place(administrativeArea = "").displayName())
    }

    @Test
    fun displayNameIsNeverBlank() {
        assertEquals("Unknown location", place(country = null).displayName())
        assertEquals("Unknown location", place("", "", "", "", country = "").displayName())
    }
}
