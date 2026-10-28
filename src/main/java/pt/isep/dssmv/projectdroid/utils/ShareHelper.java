package pt.isep.dssmv.projectdroid.utils;

import android.content.Context;
import android.content.Intent;

import java.util.List;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.model.MovieShareFormatter;
import pt.isep.dssmv.projectdroid.model.SavedMovie;

/**
 * Utility helper for dispatching Android Implicit Intents (ACTION_SEND) with a system Chooser dialog.
 * Satisfies US11 and RF11:
 * - Creates an ACTION_SEND Intent with MIME type "text/plain".
 * - Attaches formatted movie details, rating, and synopsis.
 * - Wraps in Intent.createChooser() so user can select their preferred messaging or social app.
 */
public final class ShareHelper {

    private ShareHelper() {
    }

    /**
     * Shares a SavedMovie entity from the user's lists.
     */
    public static void shareSavedMovie(Context context, SavedMovie movie) {
        if (context == null || movie == null) {
            return;
        }
        String formattedText = MovieShareFormatter.format(movie);
        dispatchShareIntent(context, movie.getTitle(), formattedText, R.string.share_movie_chooser_title);
    }

    /**
     * Shares movie details from catalog exploration or detail view.
     */
    public static void shareMovie(Context context, String title, int releaseYear, String overview, int tmdbId) {
        if (context == null || title == null) {
            return;
        }
        String formattedText = MovieShareFormatter.format(title, releaseYear, overview, tmdbId);
        dispatchShareIntent(context, title, formattedText, R.string.share_movie_chooser_title);
    }

    /**
     * Shares an entire movie list with formatted summaries of each movie.
     */
    public static void shareMovieList(Context context, String listName, String listDescription, List<SavedMovie> movies) {
        if (context == null) {
            return;
        }
        String formattedText = MovieShareFormatter.formatList(listName, listDescription, movies);
        String subject = (listName != null && !listName.trim().isEmpty())
                ? listName.trim()
                : "CineTrack Movie List";
        dispatchShareIntent(context, subject, formattedText, R.string.share_list_chooser_title);
    }

    private static void dispatchShareIntent(Context context, String subject, String text, int chooserTitleResId) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);

        String chooserTitle = context.getString(chooserTitleResId);
        Intent chooserIntent = Intent.createChooser(sendIntent, chooserTitle);

        // Ensure flags if starting from non-Activity context
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(chooserIntent);
    }
}
