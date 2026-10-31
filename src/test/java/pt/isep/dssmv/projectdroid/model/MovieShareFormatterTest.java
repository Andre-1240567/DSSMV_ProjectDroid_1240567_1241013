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

    @Test
    public void testFormatListWithMultipleMovies() throws InvalidDataException, InvalidRatingException {
        java.util.List<SavedMovie> movies = new java.util.ArrayList<>();

        SavedMovie m1 = new SavedMovie("m1", 27205, "Inception");
        m1.setReleaseYear(2010);
        m1.setStatus(WatchStatus.FAVORITE);
        m1.setRating(new Rating("r1", 4.8f, "Mind-bending masterpiece"));
        movies.add(m1);

        SavedMovie m2 = new SavedMovie("m2", 157336, "Interstellar");
        m2.setReleaseYear(2014);
        m2.setStatus(WatchStatus.WATCHED);
        movies.add(m2);

        SavedMovie m3 = new SavedMovie("m3", 550, "Fight Club");
        m3.setStatus(WatchStatus.PLAN_TO_WATCH);
        movies.add(m3);

        String text = MovieShareFormatter.formatList("Nolan & Classics", "My all-time favorite cinema picks", movies);

        assertTrue(text.contains("🎬 Nolan & Classics"));
        assertTrue(text.contains("📝 My all-time favorite cinema picks"));
        assertTrue(text.contains("🍿 Total: 3 movies"));
        assertTrue(text.contains("1. Inception (2010) - ⭐ 4.8/5.0 [Favorite]"));
        assertTrue(text.contains("2. Interstellar (2014) [Watched]"));
        assertTrue(text.contains("3. Fight Club [Plan to Watch]"));
        assertTrue(text.contains("Shared via CineTrack 🍿"));
    }

    @Test
    public void testFormatListEmptyAndNull() {
        String emptyText = MovieShareFormatter.formatList("Empty List", null, new java.util.ArrayList<>());
        assertTrue(emptyText.contains("🎬 Empty List"));
        assertTrue(emptyText.contains("🍿 Total: 0 movies"));
        assertTrue(emptyText.contains("(No movies in this list yet)"));
        assertTrue(emptyText.contains("Shared via CineTrack 🍿"));

        String nullText = MovieShareFormatter.formatList(null, null, null);
        assertTrue(nullText.contains("🎬 CineTrack List"));
        assertTrue(nullText.contains("🍿 Total: 0 movies"));
    }

    @Test
    public void testFormatStatusLabel() {
        assertEquals("Watching", MovieShareFormatter.formatStatusLabel(WatchStatus.WATCHING));
        assertEquals("Watched", MovieShareFormatter.formatStatusLabel(WatchStatus.WATCHED));
        assertEquals("Favorite", MovieShareFormatter.formatStatusLabel(WatchStatus.FAVORITE));
        assertEquals("Plan to Watch", MovieShareFormatter.formatStatusLabel(WatchStatus.PLAN_TO_WATCH));
        assertEquals("Plan to Watch", MovieShareFormatter.formatStatusLabel(null));
    }
}
