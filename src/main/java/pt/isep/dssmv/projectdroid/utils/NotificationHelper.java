package pt.isep.dssmv.projectdroid.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.controllers.MovieDetailActivity;

/**
 * Utility helper for managing Android Notification Channels and dispatching
 * movie recommendation & reminder notifications.
 * Satisfies Android 8.0+ (API 26+) NotificationChannel requirement and Android 13+ (API 33+) POST_NOTIFICATIONS runtime permission.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "cinetrack_recommendations";
    public static final int RECOMMENDATION_NOTIFICATION_ID = 1001;
    public static final int WATCHLIST_NOTIFICATION_ID = 1002;

    private NotificationHelper() {
    }

    /**
     * Creates the notification channel on Android 8.0+ (API 26+).
     * Safe to call multiple times (idempotent).
     */
    public static void createNotificationChannel(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        CharSequence name = context.getString(R.string.notification_channel_name);
        String description = context.getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Checks if the app has permission to post notifications (mandatory on Android 13+).
     */
    public static boolean hasNotificationPermission(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Dispatches a movie recommendation notification with a PendingIntent that opens the movie details.
     *
     * @return true if notification was posted, false if permission was missing.
     */
    public static boolean showMovieRecommendationNotification(Context context, String movieTitle, String overview, int tmdbId) {
        if (context == null || !hasNotificationPermission(context)) {
            return false;
        }

        createNotificationChannel(context);

        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, tmdbId);
        intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movieTitle);
        intent.putExtra(MovieDetailActivity.EXTRA_OVERVIEW, overview);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                tmdbId > 0 ? tmdbId : RECOMMENDATION_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titleText = context.getString(R.string.notification_recommendation_title);
        String bodyText = context.getString(R.string.notification_recommendation_body, movieTitle != null ? movieTitle : "Movie");
        String bigText = (overview != null && !overview.trim().isEmpty())
                ? (bodyText + "\n\n" + overview.trim())
                : bodyText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_movie_placeholder)
                .setContentTitle(titleText)
                .setContentText(bodyText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(RECOMMENDATION_NOTIFICATION_ID, builder.build());
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
     * Dispatches a notification when a movie is added to a watchlist.
     */
    public static boolean showWatchlistAddedNotification(Context context, String listName, String movieTitle, int tmdbId) {
        if (context == null || !hasNotificationPermission(context)) {
            return false;
        }

        createNotificationChannel(context);

        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, tmdbId);
        intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movieTitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                WATCHLIST_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titleText = context.getString(R.string.notification_watchlist_title);
        String bodyText = context.getString(R.string.notification_watchlist_body, movieTitle != null ? movieTitle : "Movie", listName != null ? listName : "List");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_movie_placeholder)
                .setContentTitle(titleText)
                .setContentText(bodyText)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(WATCHLIST_NOTIFICATION_ID, builder.build());
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }
}
