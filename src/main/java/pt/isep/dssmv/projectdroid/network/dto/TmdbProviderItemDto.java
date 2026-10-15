package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbProviderItemDto {

    @SerializedName("provider_id")
    private int providerId;

    @SerializedName("provider_name")
    private String providerName;

    @SerializedName("logo_path")
    private String logoPath;

    @SerializedName("display_priority")
    private int displayPriority;

    public TmdbProviderItemDto() {
    }

    public int getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public int getDisplayPriority() {
        return displayPriority;
    }

    public String getFullLogoUrl() {
        if (logoPath == null || logoPath.trim().isEmpty()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/w154" + logoPath;
    }
}
