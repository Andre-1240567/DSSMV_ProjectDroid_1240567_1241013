package pt.isep.dssmv.projectdroid.utils;

import android.content.Context;
import android.content.SharedPreferences;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.model.UserPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralized manager for persistent lightweight key-value storage using SharedPreferences.
 * Satisfies RNF05 and US8 Acceptance Criteria:
 * - Encapsulates configuration keys and default fallbacks.
 * - Enforces asynchronous persistence strictly using SharedPreferences.Editor.apply().
 * - Commit() is strictly forbidden to prevent blocking the UI thread.
 */
public class SharedPrefManager {

    private static final String PREF_NAME = "cinetrack_user_preferences";
    public static final String KEY_CINEMA_RADIUS_KM = "pref_cinema_radius_km";
    public static final String KEY_DEFAULT_REGION = "pref_default_region";
    public static final String KEY_MIN_RATING = "pref_min_rating";
    public static final String KEY_MIN_RELEASE_YEAR = "pref_min_release_year";
    public static final String KEY_STREAMING_PROVIDERS = "pref_streaming_providers";
    public static final String KEY_SHAKE_THRESHOLD = "pref_shake_threshold";
    public static final String KEY_THEME_MODE = "pref_theme_mode";
    public static final String KEY_TMDB_GUEST_SESSION_ID = "pref_tmdb_guest_session_id";

    private static SharedPrefManager instance;
    private final SharedPreferences sharedPreferences;

    private SharedPrefManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    /**
     * Reads and validates the user preferences from SharedPreferences.
     * Falls back to default values if not set or invalid.
     */
    public UserPreferences getUserPreferences() {
        int radius = sharedPreferences.getInt(KEY_CINEMA_RADIUS_KM, UserPreferences.DEFAULT_RADIUS_KM);
        String region = sharedPreferences.getString(KEY_DEFAULT_REGION, UserPreferences.DEFAULT_REGION);
        float minRating = sharedPreferences.getFloat(KEY_MIN_RATING, UserPreferences.DEFAULT_MIN_RATING);
        int minYear = sharedPreferences.getInt(KEY_MIN_RELEASE_YEAR, UserPreferences.DEFAULT_MIN_YEAR);
        Set<String> providers = sharedPreferences.getStringSet(KEY_STREAMING_PROVIDERS, new HashSet<>());
        float threshold = sharedPreferences.getFloat(KEY_SHAKE_THRESHOLD, UserPreferences.DEFAULT_SHAKE_THRESHOLD);
        String themeMode = sharedPreferences.getString(KEY_THEME_MODE, UserPreferences.DEFAULT_THEME_MODE);

        try {
            return new UserPreferences(radius, region, minRating, minYear, providers, threshold, themeMode);
        } catch (InvalidDataException e) {
            return new UserPreferences();
        }
    }

    /**
     * Saves user preferences to SharedPreferences asynchronously using apply().
     *
     * @param preferences The validated UserPreferences domain object.
     */
    public void saveUserPreferences(UserPreferences preferences) {
        if (preferences == null) {
            return;
        }
        sharedPreferences.edit()
                .putInt(KEY_CINEMA_RADIUS_KM, preferences.getCinemaRadiusKm())
                .putString(KEY_DEFAULT_REGION, preferences.getDefaultRegion())
                .putFloat(KEY_MIN_RATING, preferences.getMinRating())
                .putInt(KEY_MIN_RELEASE_YEAR, preferences.getMinReleaseYear())
                .putStringSet(KEY_STREAMING_PROVIDERS, preferences.getStreamingProviders())
                .putFloat(KEY_SHAKE_THRESHOLD, preferences.getShakeThreshold())
                .putString(KEY_THEME_MODE, preferences.getThemeMode())
                .apply();

        applyThemeMode(preferences.getThemeMode());
    }

    public int getCinemaRadiusKm() {
        return sharedPreferences.getInt(KEY_CINEMA_RADIUS_KM, UserPreferences.DEFAULT_RADIUS_KM);
    }

    public String getDefaultRegion() {
        return sharedPreferences.getString(KEY_DEFAULT_REGION, UserPreferences.DEFAULT_REGION);
    }

    public float getShakeThreshold() {
        return sharedPreferences.getFloat(KEY_SHAKE_THRESHOLD, UserPreferences.DEFAULT_SHAKE_THRESHOLD);
    }

    public String getTmdbGuestSessionId() {
        return sharedPreferences.getString(KEY_TMDB_GUEST_SESSION_ID, null);
    }

    public void saveTmdbGuestSessionId(String sessionId) {
        sharedPreferences.edit()
                .putString(KEY_TMDB_GUEST_SESSION_ID, sessionId)
                .apply();
    }

    public void applyThemeMode(String themeMode) {
        if (UserPreferences.THEME_LIGHT.equalsIgnoreCase(themeMode)) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        } else if (UserPreferences.THEME_DARK.equalsIgnoreCase(themeMode)) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public void setThemeMode(String themeMode) {
        sharedPreferences.edit()
                .putString(KEY_THEME_MODE, themeMode)
                .apply();
        applyThemeMode(themeMode);
    }

    public String getThemeMode() {
        return sharedPreferences.getString(KEY_THEME_MODE, UserPreferences.DEFAULT_THEME_MODE);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
