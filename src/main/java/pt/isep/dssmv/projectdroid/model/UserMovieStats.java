package pt.isep.dssmv.projectdroid.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure domain model for computing user movie library statistics.
 * Zero Android SDK dependencies, pure Java 21.
 */
public class UserMovieStats {

    private final int totalMovies;
    private final int uniqueMoviesCount;
    private final int favoritesCount;
    private final int watchedCount;
    private final int planToWatchCount;
    private final int watchingCount;
    private final int totalRatedCount;
    private final float averageRating;

    public UserMovieStats(Collection<SavedMovie> movies) {
        if (movies == null || movies.isEmpty()) {
            this.totalMovies = 0;
            this.uniqueMoviesCount = 0;
            this.favoritesCount = 0;
            this.watchedCount = 0;
            this.planToWatchCount = 0;
            this.watchingCount = 0;
            this.totalRatedCount = 0;
            this.averageRating = 0.0f;
            return;
        }

        int total = 0;
        int favs = 0;
        int watched = 0;
        int plan = 0;
        int watching = 0;
        int rated = 0;
        float ratingSum = 0.0f;
        Set<Integer> uniqueIds = new HashSet<>();

        for (SavedMovie movie : movies) {
            if (movie == null) continue;
            total++;

            if (movie.getTmdbId() > 0) {
                uniqueIds.add(movie.getTmdbId());
            }

            if (movie.getStatus() != null) {
                switch (movie.getStatus()) {
                    case FAVORITE:
                        favs++;
                        break;
                    case WATCHED:
                        watched++;
                        break;
                    case WATCHING:
                        watching++;
                        break;
                    case PLAN_TO_WATCH:
                        plan++;
                        break;
                }
            }

            if (movie.getRating() != null && movie.getRating().getScore() > 0) {
                rated++;
                ratingSum += movie.getRating().getScore();
            }
        }

        this.totalMovies = total;
        this.uniqueMoviesCount = uniqueIds.isEmpty() ? total : uniqueIds.size();
        this.favoritesCount = favs;
        this.watchedCount = watched;
        this.planToWatchCount = plan;
        this.watchingCount = watching;
        this.totalRatedCount = rated;
        this.averageRating = (rated > 0) ? (ratingSum / rated) : 0.0f;
    }

    public int getTotalMovies() {
        return totalMovies;
    }

    public int getUniqueMoviesCount() {
        return uniqueMoviesCount;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public int getWatchedCount() {
        return watchedCount;
    }

    public int getPlanToWatchCount() {
        return planToWatchCount;
    }

    public int getWatchingCount() {
        return watchingCount;
    }

    public int getTotalRatedCount() {
        return totalRatedCount;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public String getFormattedAverageRating() {
        if (totalRatedCount == 0 || averageRating <= 0.0f) {
            return "N/A";
        }
        return String.format(Locale.US, "%.1f ★", averageRating);
    }
}
