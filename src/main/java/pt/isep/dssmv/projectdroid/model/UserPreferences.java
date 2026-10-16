package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import java.util.HashSet;
import java.util.Set;

public class UserPreferences {

    public static final int DEFAULT_RADIUS_KM = 10;
    public static final String DEFAULT_REGION = "PT";
    public static final float DEFAULT_MIN_RATING = 6.5f;
    public static final int DEFAULT_MIN_YEAR = 0;
    public static final float DEFAULT_SHAKE_THRESHOLD = 3.2f;

    // Major provider IDs
    public static final String PROVIDER_NETFLIX = "8";
    public static final String PROVIDER_HBO_MAX = "1899";
    public static final String PROVIDER_DISNEY_PLUS = "337";
    public static final String PROVIDER_PRIME_VIDEO = "119";
    public static final String PROVIDER_APPLE_TV = "350";

    private int cinemaRadiusKm;
    private String defaultRegion;
    private float minRating;
    private int minReleaseYear;
    private Set<String> streamingProviders;
    private float shakeThreshold;

    public UserPreferences() {
        this.cinemaRadiusKm = DEFAULT_RADIUS_KM;
        this.defaultRegion = DEFAULT_REGION;
        this.minRating = DEFAULT_MIN_RATING;
        this.minReleaseYear = DEFAULT_MIN_YEAR;
        this.streamingProviders = new HashSet<>();
        this.shakeThreshold = DEFAULT_SHAKE_THRESHOLD;
    }

    public UserPreferences(int cinemaRadiusKm, String defaultRegion) throws InvalidDataException {
        this();
        setCinemaRadiusKm(cinemaRadiusKm);
        setDefaultRegion(defaultRegion);
    }

    public UserPreferences(int cinemaRadiusKm, String defaultRegion, float minRating, int minReleaseYear, Set<String> streamingProviders, float shakeThreshold) throws InvalidDataException {
        setCinemaRadiusKm(cinemaRadiusKm);
        setDefaultRegion(defaultRegion);
        setMinRating(minRating);
        setMinReleaseYear(minReleaseYear);
        setStreamingProviders(streamingProviders);
        setShakeThreshold(shakeThreshold);
    }

    public int getCinemaRadiusKm() {
        return cinemaRadiusKm;
    }

    public void setCinemaRadiusKm(int cinemaRadiusKm) throws InvalidDataException {
        if (cinemaRadiusKm < 1 || cinemaRadiusKm > 100) {
            throw new InvalidDataException("Search radius must be between 1 and 100 km");
        }
        this.cinemaRadiusKm = cinemaRadiusKm;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) throws InvalidDataException {
        if (defaultRegion == null || defaultRegion.trim().isEmpty()) {
            throw new InvalidDataException("Default region cannot be empty");
        }
        this.defaultRegion = defaultRegion.trim().toUpperCase();
    }

    public float getMinRating() {
        return minRating;
    }

    public void setMinRating(float minRating) throws InvalidDataException {
        if (minRating < 0.0f || minRating > 10.0f) {
            throw new InvalidDataException("Minimum rating must be between 0.0 and 10.0");
        }
        this.minRating = minRating;
    }

    public int getMinReleaseYear() {
        return minReleaseYear;
    }

    public void setMinReleaseYear(int minReleaseYear) throws InvalidDataException {
        if (minReleaseYear != 0 && (minReleaseYear < 1900 || minReleaseYear > 2030)) {
            throw new InvalidDataException("Release year must be 0 (any) or between 1900 and 2030");
        }
        this.minReleaseYear = minReleaseYear;
    }

    public Set<String> getStreamingProviders() {
        return streamingProviders != null ? new HashSet<>(streamingProviders) : new HashSet<>();
    }

    public void setStreamingProviders(Set<String> streamingProviders) {
        this.streamingProviders = streamingProviders != null ? new HashSet<>(streamingProviders) : new HashSet<>();
    }

    public float getShakeThreshold() {
        return shakeThreshold;
    }

    public void setShakeThreshold(float shakeThreshold) throws InvalidDataException {
        if (shakeThreshold < 1.5f || shakeThreshold > 6.0f) {
            throw new InvalidDataException("Shake threshold must be between 1.5g and 6.0g");
        }
        this.shakeThreshold = shakeThreshold;
    }

    /**
     * Helper to get pipe-delimited providers string for TMDB discover endpoint.
     * E.g. "8|1899|337"
     * Returns null if no providers configured.
     */
    public String getFormattedWatchProviders() {
        if (streamingProviders == null || streamingProviders.isEmpty()) {
            return null;
        }
        return String.join("|", streamingProviders);
    }

    @Override
    public String toString() {
        return "Preferences [Radius: " + cinemaRadiusKm + "km, Region: " + defaultRegion + "]";
    }
}
