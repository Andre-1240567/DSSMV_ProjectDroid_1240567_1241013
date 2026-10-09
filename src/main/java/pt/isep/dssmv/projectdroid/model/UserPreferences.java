package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

public class UserPreferences {

    private static final int DEFAULT_RADIUS_KM = 10;
    private static final String DEFAULT_REGION = "PT";

    private int cinemaRadiusKm;
    private String defaultRegion;

    public UserPreferences() {
        this.cinemaRadiusKm = DEFAULT_RADIUS_KM;
        this.defaultRegion = DEFAULT_REGION;
    }

    public UserPreferences(int cinemaRadiusKm, String defaultRegion) throws InvalidDataException {
        setCinemaRadiusKm(cinemaRadiusKm);
        setDefaultRegion(defaultRegion);
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

    @Override
    public String toString() {
        return "Preferences [Radius: " + cinemaRadiusKm + "km, Region: " + defaultRegion + "]";
    }
}
