package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class FsqPlaceDto {

    @SerializedName(value = "fsq_place_id", alternate = {"fsq_id"})
    private String fsqId;

    @SerializedName("name")
    private String name;

    @SerializedName("distance")
    private int distance;

    @SerializedName("latitude")
    private Double directLatitude;

    @SerializedName("longitude")
    private Double directLongitude;

    @SerializedName("geocodes")
    private FsqGeocodesDto geocodes;

    @SerializedName("location")
    private FsqLocationDto location;

    public FsqPlaceDto() {
    }

    public String getFsqId() {
        return fsqId;
    }

    public String getName() {
        return name;
    }

    public int getDistance() {
        return distance;
    }

    public double getLatitude() {
        if (directLatitude != null) {
            return directLatitude;
        }
        return (geocodes != null && geocodes.getMain() != null) ? geocodes.getMain().getLatitude() : 0.0;
    }

    public double getLongitude() {
        if (directLongitude != null) {
            return directLongitude;
        }
        return (geocodes != null && geocodes.getMain() != null) ? geocodes.getMain().getLongitude() : 0.0;
    }

    public String getDisplayAddress() {
        if (location == null) return "";
        if (location.getFormattedAddress() != null && !location.getFormattedAddress().trim().isEmpty()) {
            return location.getFormattedAddress().trim();
        }
        return (location.getAddress() != null) ? location.getAddress().trim() : "";
    }
}
