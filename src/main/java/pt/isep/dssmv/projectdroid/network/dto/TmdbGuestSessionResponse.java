package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbGuestSessionResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("guest_session_id")
    private String guestSessionId;

    @SerializedName("expires_at")
    private String expiresAt;

    public TmdbGuestSessionResponse() {
    }

    public TmdbGuestSessionResponse(boolean success, String guestSessionId, String expiresAt) {
        this.success = success;
        this.guestSessionId = guestSessionId;
        this.expiresAt = expiresAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGuestSessionId() {
        return guestSessionId;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
