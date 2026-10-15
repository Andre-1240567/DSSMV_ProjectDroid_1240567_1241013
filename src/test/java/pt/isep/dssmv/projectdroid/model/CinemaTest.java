package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import static org.junit.jupiter.api.Assertions.*;

public class CinemaTest {

    @Test
    public void testValidCinemaCreation() throws InvalidDataException {
        Cinema cinema = new Cinema("fsq_123", "Cinema Trindade", 41.1504, -8.6095);
        cinema.setAddress("Rua do Almada 412");
        cinema.setDistanceMeters(850);

        assertEquals("fsq_123", cinema.getFsqId());
        assertEquals("Cinema Trindade", cinema.getName());
        assertEquals(41.1504, cinema.getLatitude(), 0.0001);
        assertEquals(-8.6095, cinema.getLongitude(), 0.0001);
        assertEquals("Rua do Almada 412", cinema.getAddress());
        assertEquals(850, cinema.getDistanceMeters());
        assertEquals("Cinema Trindade (850m)", cinema.toString());
    }

    @Test
    public void testEmptyFsqIdThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new Cinema("", "Cinema Trindade", 41.15, -8.60);
        });

        assertThrows(InvalidDataException.class, () -> {
            new Cinema(null, "Cinema Trindade", 41.15, -8.60);
        });
    }

    @Test
    public void testEmptyNameThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new Cinema("fsq_123", "   ", 41.15, -8.60);
        });

        assertThrows(InvalidDataException.class, () -> {
            new Cinema("fsq_123", null, 41.15, -8.60);
        });
    }

    @Test
    public void testEqualsAndHashCode() throws InvalidDataException {
        Cinema cinema1 = new Cinema("fsq_123", "Cinema 1", 41.1, -8.6);
        Cinema cinema2 = new Cinema("fsq_123", "Cinema 1 Renamed", 42.0, -9.0);
        Cinema cinema3 = new Cinema("fsq_456", "Cinema 2", 41.1, -8.6);

        assertEquals(cinema1, cinema2);
        assertNotEquals(cinema1, cinema3);
        assertEquals(cinema1.hashCode(), cinema2.hashCode());
    }
}
