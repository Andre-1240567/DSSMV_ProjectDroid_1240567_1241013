package pt.isep.dssmv.projectdroid.model;

import java.util.Locale;

/**
 * Pure domain service for formatting movie metadata into social sharing messages.
 * Satisfies US11 and RF11:
 * - Pure Java 21 with zero Android SDK dependencies.
 * - Formats title, year, synopsis, rating, review comments, and TMDB catalog link.
 */
public final class MovieShareFormatter {

    private static final String TMDB_MOVIE_BASE_URL = "https://www.themoviedb.org/movie/";
    private static final int MAX_OVERVIEW_LENGTH = 300;

    private MovieShareFormatter() {
    }

    public static String format(SavedMovie movie) {
        if (movie == null) {
            return "";
        }

        Float score = (movie.getRating() != null && movie.getRating().getScore() > 0)
                ? movie.getRating().getScore()
                : null;
        String comment = (movie.getRating() != null) ? movie.getRating().getComment() : null;

        return formatDetails(
                movie.getTitle(),
                movie.getReleaseYear(),
                movie.getOverview(),
                movie.getTmdbId(),
                score,
                comment,
                movie.getStatus()
        );
    }

    public static String format(String title, int releaseYear, String overview, int tmdbId) {
        return formatDetails(title, releaseYear, overview, tmdbId, null, null, null);
    }

    public static String formatDetails(
            String title,
            int releaseYear,
            String overview,
            int tmdbId,
            Float score,
            String comment,
            WatchStatus status
    ) {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 🎬 Title (Year)
        sb.append("🎬 ").append(title.trim());
        if (releaseYear > 0) {
            sb.append(" (").append(releaseYear).append(")");
        }
        sb.append("\n\n");

        // ⭐ Rating
        if (score != null && score > 0) {
            sb.append(String.format(Locale.US, "⭐ Rating: %.1f / 5.0 ★\n", score));
        }

        // 📝 Review Comment
        if (comment != null && !comment.trim().isEmpty()) {
            sb.append("📝 Review: \"").append(comment.trim()).append("\"\n");
        }

        // 📌 Watch Status
        if (status != null) {
            String statusText;
            switch (status) {
                case WATCHING:
                    statusText = "Watching";
                    break;
                case WATCHED:
                    statusText = "Watched";
                    break;
                case FAVORITE:
                    statusText = "Favorite";
                    break;
                case PLAN_TO_WATCH:
                default:
                    statusText = "Plan to Watch";
                    break;
            }
            sb.append("📌 Status: ").append(statusText).append("\n");
        }

        // 📖 Synopsis
        if (overview != null && !overview.trim().isEmpty()) {
            sb.append("\n📖 Synopsis:\n");
            String trimmedOverview = overview.trim();
            if (trimmedOverview.length() > MAX_OVERVIEW_LENGTH) {
                sb.append(trimmedOverview, 0, MAX_OVERVIEW_LENGTH).append("…\n");
            } else {
                sb.append(trimmedOverview).append("\n");
            }
        }

        // 🔗 TMDB Link
        if (tmdbId > 0) {
            sb.append("\n🔗 TMDB: ").append(TMDB_MOVIE_BASE_URL).append(tmdbId).append("\n");
        }

        sb.append("\nShared via CineTrack 🍿");

        return sb.toString();
    }
}
