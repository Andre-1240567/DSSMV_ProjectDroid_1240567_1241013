package pt.isep.dssmv.projectdroid.network;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.network.dto.TmdbMovieDto;

import static org.junit.jupiter.api.Assertions.*;

public class TmdbMovieDtoTest {

    @Test
    public void testReleaseYearExtraction() {
        TmdbMovieDto movie = new TmdbMovieDto(550, "Fight Club", "An insomniac office worker...", "/poster.jpg", "1999-10-15");
        assertEquals(1999, movie.getReleaseYear());
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", movie.getFullPosterUrl());
    }

    @Test
    public void testEmptyOrInvalidReleaseDate() {
        TmdbMovieDto movie1 = new TmdbMovieDto(1, "Upcoming Movie", "Overview", null, "");
        assertEquals(0, movie1.getReleaseYear());
        assertNull(movie1.getFullPosterUrl());

        TmdbMovieDto movie2 = new TmdbMovieDto(2, "Invalid Date", "Overview", "   ", "abc");
        assertEquals(0, movie2.getReleaseYear());
        assertNull(movie2.getFullPosterUrl());
    }
}
