package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import static org.junit.jupiter.api.Assertions.*;

public class WatchProviderTest {

    @Test
    public void testValidWatchProviderCreation() throws InvalidDataException {
        WatchProvider provider = new WatchProvider(8, "Netflix", "/logo.jpg", "flatrate");

        assertEquals(8, provider.getProviderId());
        assertEquals("Netflix", provider.getName());
        assertEquals("/logo.jpg", provider.getLogoPath());
        assertEquals("flatrate", provider.getAccessType());
        assertEquals("Netflix (flatrate)", provider.toString());
    }

    @Test
    public void testInvalidProviderIdThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new WatchProvider(0, "Netflix", "/logo.jpg", "flatrate");
        });

        assertThrows(InvalidDataException.class, () -> {
            new WatchProvider(-1, "Netflix", "/logo.jpg", "flatrate");
        });
    }

    @Test
    public void testNullProviderNameThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new WatchProvider(8, null, "/logo.jpg", "flatrate");
        });
    }

    @Test
    public void testEmptyProviderNameThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new WatchProvider(8, "   ", "/logo.jpg", "flatrate");
        });
    }

    @Test
    public void testEqualsAndHashCode() throws InvalidDataException {
        WatchProvider provider1 = new WatchProvider(8, "Netflix", "/logo1.jpg", "flatrate");
        WatchProvider provider2 = new WatchProvider(8, "Netflix PT", "/logo2.jpg", "flatrate");
        WatchProvider provider3 = new WatchProvider(119, "Amazon Prime", "/logo3.jpg", "flatrate");

        assertEquals(provider1, provider2);
        assertNotEquals(provider1, provider3);
        assertEquals(provider1.hashCode(), provider2.hashCode());
    }
}
