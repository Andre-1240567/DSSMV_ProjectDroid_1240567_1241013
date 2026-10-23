package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;

import static org.junit.jupiter.api.Assertions.*;

public class MovieShareFormatterTest {

    @Test
    public void testFormatCompleteSavedMovie() throws InvalidDataException, InvalidRatingException {
        SavedMovie movie = new SavedMovie("id-123", 550, "Fight Club");
        movie.setReleaseYear(1999);
        movie.setStatus(WatchStatus.FAVORITE);
        movie.setOverview("An insomniac office worker and a devil-may-care soap maker form an underground fight club.");
        movie.setRating(new Rating("r-1", 4.5f, "Exceptional narrative twist!"));

        String text = MovieShareFormatter.format(movie);

        assertTrue(text.contains("🎬 Fight Club (1999)"));
        assertTrue(text.contains("⭐ Rating: 4.5 / 5.0 ★"));
        assertTrue(text.contains("📝 Review: \"Exceptional narrative twist!\""));
        assertTrue(text.contains("📌 Status: Favorite"));
        assertTrue(text.contains("📖 Synopsis:"));
        assertTrue(text.contains("https://www.themoviedb.org/movie/550"));
        assertTrue(text.contains("Shared via CineTrack 🍿"));
    }

    @Test
    public void testFormatMinimalMovieWithoutRating() throws InvalidDataException {
        SavedMovie movie = new SavedMovie("id-999", 27205, "Inception");
        movie.setReleaseYear(2010);
        movie.setStatus(WatchStatus.PLAN_TO_WATCH);

        String text = MovieShareFormatter.format(movie);

        assertTrue(text.contains("🎬 Inception (2010)"));
        assertFalse(text.contains("⭐ Rating:"));
        assertFalse(text.contains("📝 Review:"));
        assertTrue(text.contains("📌 Status: Plan to Watch"));
        assertTrue(text.contains("https://www.themoviedb.org/movie/27205"));
    }

    @Test
    public void testFormatRawParameters() {
        String text = MovieShareFormatter.format("Interstellar", 2014, "Mankind was born on Earth. It was never meant to die here.", 157336);

        assertTrue(text.contains("🎬 Interstellar (2014)"));
        assertTrue(text.contains("Mankind was born on Earth"));
        assertTrue(text.contains("https://www.themoviedb.org/movie/157336"));
    }

    @Test
    public void testFormatTruncatesExcessivelyLongSynopsis() {
        String longOverview = "A".repeat(400);
        String text = MovieShareFormatter.format("Long Movie", 2024, longOverview, 100);

        assertTrue(text.contains("…"));
        assertFalse(text.contains("A".repeat(350)));
    }

    @Test
    public void testFormatNullAndEmptyInputs() {
        assertEquals("", MovieShareFormatter.format(null));
        assertEquals("", MovieShareFormatter.format(null, 2024, "Overview", 100));
        assertEquals("", MovieShareFormatter.format("   ", 0, null, 0));
    }
}
