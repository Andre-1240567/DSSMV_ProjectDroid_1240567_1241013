package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbRatingRequest {

    @SerializedName("value")
    private double value;

    public TmdbRatingRequest() {
    }

    public TmdbRatingRequest(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
