package pt.isep.dssmv.projectdroid.model;

import java.util.List;
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
            sb.append("📌 Status: ").append(formatStatusLabel(status)).append("\n");
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

    /**
     * Formats an entire movie list into a formatted social message.
     */
    public static String formatList(String listName, String listDescription, List<SavedMovie> movies) {
        String effectiveListName = (listName != null && !listName.trim().isEmpty())
                ? listName.trim()
                : "CineTrack List";

        StringBuilder sb = new StringBuilder();
        sb.append("🎬 ").append(effectiveListName).append("\n");

        if (listDescription != null && !listDescription.trim().isEmpty()) {
            sb.append("📝 ").append(listDescription.trim()).append("\n");
        }

        int count = (movies != null) ? movies.size() : 0;
        sb.append("🍿 Total: ").append(count).append(count == 1 ? " movie" : " movies").append("\n\n");

        if (movies == null || movies.isEmpty()) {
            sb.append("(No movies in this list yet)\n");
        } else {
            for (int i = 0; i < movies.size(); i++) {
                SavedMovie movie = movies.get(i);
                if (movie == null) continue;

                sb.append(i + 1).append(". ");
                String title = (movie.getTitle() != null && !movie.getTitle().trim().isEmpty())
                        ? movie.getTitle().trim()
                        : "Untitled";
                sb.append(title);

                if (movie.getReleaseYear() > 0) {
                    sb.append(" (").append(movie.getReleaseYear()).append(")");
                }

                if (movie.getRating() != null && movie.getRating().getScore() > 0) {
                    sb.append(String.format(Locale.US, " - ⭐ %.1f/5.0", movie.getRating().getScore()));
                }

                if (movie.getStatus() != null) {
                    sb.append(" [").append(formatStatusLabel(movie.getStatus())).append("]");
                }

                sb.append("\n");
            }
        }

        sb.append("\nShared via CineTrack 🍿");
        return sb.toString();
    }

    public static String formatStatusLabel(WatchStatus status) {
        if (status == null) {
            return "Plan to Watch";
        }
        switch (status) {
            case WATCHING:
                return "Watching";
            case WATCHED:
                return "Watched";
            case FAVORITE:
                return "Favorite";
            case PLAN_TO_WATCH:
            default:
                return "Plan to Watch";
        }
    }
}
