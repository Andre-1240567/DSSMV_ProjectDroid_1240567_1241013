package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import static org.junit.jupiter.api.Assertions.*;

public class UserPreferencesTest {

    @Test
    public void testDefaultConstructorValues() {
        UserPreferences prefs = new UserPreferences();
        assertEquals(10, prefs.getCinemaRadiusKm());
        assertEquals("PT", prefs.getDefaultRegion());
    }

    @Test
    public void testValidParameterizedConstructor() throws InvalidDataException {
        UserPreferences prefs = new UserPreferences(25, "es");
        assertEquals(25, prefs.getCinemaRadiusKm());
        assertEquals("ES", prefs.getDefaultRegion());
        assertEquals("Preferences [Radius: 25km, Region: ES]", prefs.toString());
    }

    @Test
    public void testBoundaryRadiusValues() throws InvalidDataException {
        UserPreferences minRadius = new UserPreferences(1, "PT");
        assertEquals(1, minRadius.getCinemaRadiusKm());

        UserPreferences maxRadius = new UserPreferences(100, "PT");
        assertEquals(100, maxRadius.getCinemaRadiusKm());
    }

    @Test
    public void testRadiusBelowMinimumThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new UserPreferences(0, "PT");
        });
    }

    @Test
    public void testRadiusAboveMaximumThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new UserPreferences(101, "PT");
        });
    }

    @Test
    public void testNullRegionThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new UserPreferences(15, null);
        });
    }

    @Test
    public void testEmptyRegionThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new UserPreferences(15, "   ");
        });
    }
}
