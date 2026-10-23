package pt.isep.dssmv.projectdroid.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure domain service for filtering and sorting collections of SavedMovie entities.
 * Satisfies US9 and RF09:
 * - Uses pure Java 21 Comparator functional interfaces.
 * - Zero Android SDK dependencies for fast, reliable unit testing.
 * - Supports filtering by WatchStatus and search query text.
 * - Supports sorting by Title, Rating, and Release Year (ascending and descending).
 */
public final class SavedMovieFilterSorter {

    private SavedMovieFilterSorter() {
        // Utility class
    }

    public static final Comparator<SavedMovie> COMPARATOR_TITLE_ASC =
            Comparator.comparing((SavedMovie m) -> m.getTitle() == null ? "" : m.getTitle(), String.CASE_INSENSITIVE_ORDER);

    public static final Comparator<SavedMovie> COMPARATOR_TITLE_DESC =
            COMPARATOR_TITLE_ASC.reversed();

    public static final Comparator<SavedMovie> COMPARATOR_RATING_DESC =
            Comparator.comparingDouble((SavedMovie m) -> (m.getRating() != null) ? m.getRating().getScore() : -1.0)
                    .reversed()
                    .thenComparing(COMPARATOR_TITLE_ASC);

    public static final Comparator<SavedMovie> COMPARATOR_RATING_ASC =
            Comparator.comparingDouble((SavedMovie m) -> (m.getRating() != null) ? m.getRating().getScore() : 999.0)
                    .thenComparing(COMPARATOR_TITLE_ASC);

    public static final Comparator<SavedMovie> COMPARATOR_YEAR_DESC =
            Comparator.comparingInt(SavedMovie::getReleaseYear)
                    .reversed()
                    .thenComparing(COMPARATOR_TITLE_ASC);

    public static final Comparator<SavedMovie> COMPARATOR_YEAR_ASC =
            Comparator.comparingInt((SavedMovie m) -> m.getReleaseYear() > 0 ? m.getReleaseYear() : 9999)
                    .thenComparing(COMPARATOR_TITLE_ASC);

    /**
     * Obtains the Comparator for the specified sort option.
     */
    public static Comparator<SavedMovie> getComparator(SavedMovieSortOption option) {
        if (option == null) {
            return COMPARATOR_TITLE_ASC;
        }
        switch (option) {
            case TITLE_DESC:
                return COMPARATOR_TITLE_DESC;
            case RATING_DESC:
                return COMPARATOR_RATING_DESC;
            case RATING_ASC:
                return COMPARATOR_RATING_ASC;
            case YEAR_DESC:
                return COMPARATOR_YEAR_DESC;
            case YEAR_ASC:
                return COMPARATOR_YEAR_ASC;
            case TITLE_ASC:
            default:
                return COMPARATOR_TITLE_ASC;
        }
    }

    /**
     * Filters a list of SavedMovie entities by WatchStatus and/or search query.
     * Returns a new List containing only matching items.
     *
     * @param movies The source list (unmodified).
     * @param statusFilter The status to filter by, or null for ALL.
     * @param query Search query text to match title, or null/empty for no query filter.
     * @return Filtered list of movies.
     */
    public static List<SavedMovie> filter(List<SavedMovie> movies, WatchStatus statusFilter, String query) {
        if (movies == null || movies.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = (query != null && !query.trim().isEmpty())
                ? query.trim().toLowerCase()
                : null;

        List<SavedMovie> filtered = new ArrayList<>();
        for (SavedMovie movie : movies) {
            if (movie == null) continue;

            // Check status filter
            if (statusFilter != null && movie.getStatus() != statusFilter) {
                continue;
            }

            // Check query filter
            if (normalizedQuery != null) {
                String title = movie.getTitle();
                if (title == null || !title.toLowerCase().contains(normalizedQuery)) {
                    continue;
                }
            }

            filtered.add(movie);
        }

        return filtered;
    }

    /**
     * Sorts a list of SavedMovie entities by the given sort option.
     * Returns a new sorted list (does not mutate original list).
     */
    public static List<SavedMovie> sort(List<SavedMovie> movies, SavedMovieSortOption option) {
        if (movies == null || movies.isEmpty()) {
            return new ArrayList<>();
        }

        List<SavedMovie> sorted = new ArrayList<>(movies);
        sorted.sort(getComparator(option));
        return sorted;
    }

    /**
     * Combines filtering and sorting in one operation.
     */
    public static List<SavedMovie> filterAndSort(List<SavedMovie> movies, WatchStatus statusFilter, String query, SavedMovieSortOption sortOption) {
        List<SavedMovie> filtered = filter(movies, statusFilter, query);
        return sort(filtered, sortOption);
    }
}
