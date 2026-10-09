package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import static org.junit.jupiter.api.Assertions.*;

public class MovieListTest {

    @Test
    public void testValidMovieListCreation() throws InvalidDataException {
        MovieList list = new MovieList("l1", "Sci-Fi Favorites");

        assertEquals("l1", list.getListId());
        assertEquals("Sci-Fi Favorites", list.getName());
        assertNotNull(list.getCreatedAt());
        assertEquals("Sci-Fi Favorites", list.toString());
    }

    @Test
    public void testEmptyListIdThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new MovieList("", "Action");
        });
    }

    @Test
    public void testEmptyListNameThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new MovieList("l1", "   ");
        });
    }
}
