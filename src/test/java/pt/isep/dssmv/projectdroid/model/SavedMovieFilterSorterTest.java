package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SavedMovieFilterSorterTest {

    private SavedMovie movieA; // Avatar, 2009, 3.5★, WATCHED
    private SavedMovie movieB; // The Dark Knight, 2008, 5.0★, FAVORITE
    private SavedMovie movieC; // Interstellar, 2014, 4.5★, WATCHING
    private SavedMovie movieD; // Dune: Part Two, 2024, unrated, PLAN_TO_WATCH
    private List<SavedMovie> movies;

    @BeforeEach
    public void setUp() throws InvalidDataException, InvalidRatingException {
        movieA = new SavedMovie("id-1", 19995, "Avatar");
        movieA.setReleaseYear(2009);
        movieA.setStatus(WatchStatus.WATCHED);
        movieA.setRating(new Rating("r1", 3.5f, "Great visuals"));

        movieB = new SavedMovie("id-2", 155, "The Dark Knight");
        movieB.setReleaseYear(2008);
        movieB.setStatus(WatchStatus.FAVORITE);
        movieB.setRating(new Rating("r2", 5.0f, "Masterpiece"));

        movieC = new SavedMovie("id-3", 157336, "Interstellar");
        movieC.setReleaseYear(2014);
        movieC.setStatus(WatchStatus.WATCHING);
        movieC.setRating(new Rating("r3", 4.5f, "Emotional and epic"));

        movieD = new SavedMovie("id-4", 693134, "Dune: Part Two");
        movieD.setReleaseYear(2024);
        movieD.setStatus(WatchStatus.PLAN_TO_WATCH);

        movies = List.of(movieA, movieB, movieC, movieD);
    }

    @Test
    public void testSortByTitleAscending() {
        List<SavedMovie> sorted = SavedMovieFilterSorter.sort(movies, SavedMovieSortOption.TITLE_ASC);

        assertEquals(4, sorted.size());
        assertEquals("Avatar", sorted.get(0).getTitle());
        assertEquals("Dune: Part Two", sorted.get(1).getTitle());
        assertEquals("Interstellar", sorted.get(2).getTitle());
        assertEquals("The Dark Knight", sorted.get(3).getTitle());
    }

    @Test
    public void testSortByTitleDescending() {
        List<SavedMovie> sorted = SavedMovieFilterSorter.sort(movies, SavedMovieSortOption.TITLE_DESC);

        assertEquals(4, sorted.size());
        assertEquals("The Dark Knight", sorted.get(0).getTitle());
        assertEquals("Interstellar", sorted.get(1).getTitle());
        assertEquals("Dune: Part Two", sorted.get(2).getTitle());
        assertEquals("Avatar", sorted.get(3).getTitle());
    }

    @Test
    public void testSortByRatingDescending() {
        List<SavedMovie> sorted = SavedMovieFilterSorter.sort(movies, SavedMovieSortOption.RATING_DESC);

        assertEquals(4, sorted.size());
        assertEquals("The Dark Knight", sorted.get(0).getTitle()); // 5.0
        assertEquals("Interstellar", sorted.get(1).getTitle());    // 4.5
        assertEquals("Avatar", sorted.get(2).getTitle());          // 3.5
        assertEquals("Dune: Part Two", sorted.get(3).getTitle());  // Unrated (at the bottom)
    }

    @Test
    public void testSortByRatingAscending() {
        List<SavedMovie> sorted = SavedMovieFilterSorter.sort(movies, SavedMovieSortOption.RATING_ASC);

        assertEquals(4, sorted.size());
        assertEquals("Avatar", sorted.get(0).getTitle());          // 3.5
        assertEquals("Interstellar", sorted.get(1).getTitle());    // 4.5
        assertEquals("The Dark Knight", sorted.get(2).getTitle()); // 5.0
        assertEquals("Dune: Part Two", sorted.get(3).getTitle());  // Unrated
    }

    @Test
    public void testSortByYearDescending() {
        List<SavedMovie> sorted = SavedMovieFilterSorter.sort(movies, SavedMovieSortOption.YEAR_DESC);

        assertEquals(4, sorted.size());
        assertEquals("Dune: Part Two", sorted.get(0).getTitle());  // 2024
        assertEquals("Interstellar", sorted.get(1).getTitle());    // 2014
        assertEquals("Avatar", sorted.get(2).getTitle());          // 2009
        assertEquals("The Dark Knight", sorted.get(3).getTitle()); // 2008
    }

    @Test
    public void testSortByYearAscending() {
        List<SavedMovie> sorted = SavedMovieFilterSorter.sort(movies, SavedMovieSortOption.YEAR_ASC);

        assertEquals(4, sorted.size());
        assertEquals("The Dark Knight", sorted.get(0).getTitle()); // 2008
        assertEquals("Avatar", sorted.get(1).getTitle());          // 2009
        assertEquals("Interstellar", sorted.get(2).getTitle());    // 2014
        assertEquals("Dune: Part Two", sorted.get(3).getTitle());  // 2024
    }

    @Test
    public void testFilterByStatus() {
        List<SavedMovie> watched = SavedMovieFilterSorter.filter(movies, WatchStatus.WATCHED, null);
        assertEquals(1, watched.size());
        assertEquals("Avatar", watched.get(0).getTitle());

        List<SavedMovie> favorites = SavedMovieFilterSorter.filter(movies, WatchStatus.FAVORITE, null);
        assertEquals(1, favorites.size());
        assertEquals("The Dark Knight", favorites.get(0).getTitle());

        List<SavedMovie> all = SavedMovieFilterSorter.filter(movies, null, null);
        assertEquals(4, all.size());
    }

    @Test
    public void testFilterBySearchQuery() {
        List<SavedMovie> results = SavedMovieFilterSorter.filter(movies, null, "dark");
        assertEquals(1, results.size());
        assertEquals("The Dark Knight", results.get(0).getTitle());

        List<SavedMovie> caseInsensitive = SavedMovieFilterSorter.filter(movies, null, "INTER");
        assertEquals(1, caseInsensitive.size());
        assertEquals("Interstellar", caseInsensitive.get(0).getTitle());

        List<SavedMovie> none = SavedMovieFilterSorter.filter(movies, null, "NonExistingMovie");
        assertTrue(none.isEmpty());
    }

    @Test
    public void testCombinedFilterAndSort() {
        // All watched/favorite movies sorted by rating desc
        List<SavedMovie> combined = SavedMovieFilterSorter.filterAndSort(
                movies,
                WatchStatus.FAVORITE,
                null,
                SavedMovieSortOption.RATING_DESC
        );

        assertEquals(1, combined.size());
        assertEquals("The Dark Knight", combined.get(0).getTitle());
    }

    @Test
    public void testNullAndEmptyInputs() {
        assertTrue(SavedMovieFilterSorter.filter(null, null, null).isEmpty());
        assertTrue(SavedMovieFilterSorter.filter(new ArrayList<>(), WatchStatus.WATCHED, "test").isEmpty());
        assertTrue(SavedMovieFilterSorter.sort(null, SavedMovieSortOption.TITLE_ASC).isEmpty());
        assertTrue(SavedMovieFilterSorter.sort(new ArrayList<>(), SavedMovieSortOption.YEAR_DESC).isEmpty());
    }
}
