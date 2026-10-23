package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbRatingResponse {

    @SerializedName("status_code")
    private int statusCode;

    @SerializedName("status_message")
    private String statusMessage;

    @SerializedName("success")
    private boolean success;

    public TmdbRatingResponse() {
    }

    public TmdbRatingResponse(int statusCode, String statusMessage, boolean success) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isSuccess() {
        return success;
    }
}
