package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import static org.junit.jupiter.api.Assertions.*;

public class SavedMovieTest {

    @Test
    public void testValidSavedMovieCreation() throws InvalidDataException {
        SavedMovie movie = new SavedMovie("m1", 550, "Fight Club");
        movie.setReleaseYear(1999);

        assertEquals("m1", movie.getInternalId());
        assertEquals(550, movie.getTmdbId());
        assertEquals("Fight Club", movie.getTitle());
        assertEquals(WatchStatus.PLAN_TO_WATCH, movie.getStatus());
        assertEquals("Fight Club (1999)", movie.toString());
    }

    @Test
    public void testNegativeOrZeroTmdbIdThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new SavedMovie("m1", 0, "Invalid Movie");
        });
        assertThrows(InvalidDataException.class, () -> {
            new SavedMovie("m1", -10, "Invalid Movie");
        });
    }

    @Test
    public void testEmptyTitleThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new SavedMovie("m1", 550, "");
        });
    }

    @Test
    public void testChangeWatchStatus() throws InvalidDataException {
        SavedMovie movie = new SavedMovie("m1", 550, "Fight Club");
        movie.setStatus(WatchStatus.WATCHED);
        assertEquals(WatchStatus.WATCHED, movie.getStatus());
    }
}
