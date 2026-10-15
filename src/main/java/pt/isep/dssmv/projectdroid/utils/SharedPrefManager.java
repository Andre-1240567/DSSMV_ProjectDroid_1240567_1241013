package pt.isep.dssmv.projectdroid.utils;

import android.content.Context;
import android.content.SharedPreferences;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.model.UserPreferences;

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

    private static final int DEFAULT_RADIUS_KM = 10;
    private static final String DEFAULT_REGION = "PT";

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
        int radius = sharedPreferences.getInt(KEY_CINEMA_RADIUS_KM, DEFAULT_RADIUS_KM);
        String region = sharedPreferences.getString(KEY_DEFAULT_REGION, DEFAULT_REGION);
        try {
            return new UserPreferences(radius, region);
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
                .apply();
    }

    public int getCinemaRadiusKm() {
        return sharedPreferences.getInt(KEY_CINEMA_RADIUS_KM, DEFAULT_RADIUS_KM);
    }

    public void saveCinemaRadiusKm(int radiusKm) throws InvalidDataException {
        if (radiusKm < 1 || radiusKm > 100) {
            throw new InvalidDataException("Search radius must be between 1 and 100 km");
        }
        sharedPreferences.edit()
                .putInt(KEY_CINEMA_RADIUS_KM, radiusKm)
                .apply();
    }

    public String getDefaultRegion() {
        return sharedPreferences.getString(KEY_DEFAULT_REGION, DEFAULT_REGION);
    }

    public void saveDefaultRegion(String region) throws InvalidDataException {
        if (region == null || region.trim().isEmpty()) {
            throw new InvalidDataException("Default region cannot be empty");
        }
        sharedPreferences.edit()
                .putString(KEY_DEFAULT_REGION, region.trim().toUpperCase())
                .apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
