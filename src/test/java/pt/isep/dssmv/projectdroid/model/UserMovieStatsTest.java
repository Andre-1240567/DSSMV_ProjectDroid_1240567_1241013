package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserMovieStatsTest {

    @Test
    public void testEmptyAndNullStats() {
        UserMovieStats nullStats = new UserMovieStats(null);
        assertEquals(0, nullStats.getTotalMovies());
        assertEquals(0, nullStats.getUniqueMoviesCount());
        assertEquals(0, nullStats.getFavoritesCount());
        assertEquals(0, nullStats.getWatchedCount());
        assertEquals(0, nullStats.getPlanToWatchCount());
        assertEquals(0, nullStats.getWatchingCount());
        assertEquals(0, nullStats.getTotalRatedCount());
        assertEquals(0.0f, nullStats.getAverageRating(), 0.001f);
        assertEquals("N/A", nullStats.getFormattedAverageRating());

        UserMovieStats emptyStats = new UserMovieStats(Collections.emptyList());
        assertEquals(0, emptyStats.getTotalMovies());
        assertEquals("N/A", emptyStats.getFormattedAverageRating());
    }

    @Test
    public void testStatsWithMixedMovies() throws InvalidDataException, InvalidRatingException {
        List<SavedMovie> movies = new ArrayList<>();

        SavedMovie m1 = new SavedMovie("m1", 100, "Movie 1");
        m1.setStatus(WatchStatus.FAVORITE);
        m1.setRating(new Rating("r1", 4.0f, "Great"));
        movies.add(m1);

        SavedMovie m2 = new SavedMovie("m2", 101, "Movie 2");
        m2.setStatus(WatchStatus.WATCHED);
        m2.setRating(new Rating("r2", 5.0f, "Masterpiece"));
        movies.add(m2);

        SavedMovie m3 = new SavedMovie("m3", 102, "Movie 3");
        m3.setStatus(WatchStatus.WATCHING);
        // Unrated
        movies.add(m3);

        SavedMovie m4 = new SavedMovie("m4", 103, "Movie 4");
        m4.setStatus(WatchStatus.PLAN_TO_WATCH);
        // Unrated
        movies.add(m4);

        // Duplicate tmdbId in another list to test unique count
        SavedMovie m5 = new SavedMovie("m5", 100, "Movie 1 duplicate in another list");
        m5.setStatus(WatchStatus.FAVORITE);
        movies.add(m5);

        UserMovieStats stats = new UserMovieStats(movies);

        assertEquals(5, stats.getTotalMovies());
        assertEquals(4, stats.getUniqueMoviesCount());
        assertEquals(2, stats.getFavoritesCount());
        assertEquals(1, stats.getWatchedCount());
        assertEquals(1, stats.getWatchingCount());
        assertEquals(1, stats.getPlanToWatchCount());
        assertEquals(2, stats.getTotalRatedCount());
        assertEquals(4.5f, stats.getAverageRating(), 0.001f);
        assertEquals("4.5 ★", stats.getFormattedAverageRating());
    }

    @Test
    public void testStatsWithOnlyUnratedMovies() throws InvalidDataException {
        List<SavedMovie> movies = new ArrayList<>();
        SavedMovie m1 = new SavedMovie("m1", 200, "Unrated Movie");
        m1.setStatus(WatchStatus.WATCHED);
        movies.add(m1);

        UserMovieStats stats = new UserMovieStats(movies);
        assertEquals(1, stats.getTotalMovies());
        assertEquals(0, stats.getTotalRatedCount());
        assertEquals(0.0f, stats.getAverageRating(), 0.001f);
        assertEquals("N/A", stats.getFormattedAverageRating());
    }
}
