package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TmdbCountryProvidersDto {

    @SerializedName("link")
    private String link;

    @SerializedName("flatrate")
    private List<TmdbProviderItemDto> flatrate = new ArrayList<>();

    @SerializedName("rent")
    private List<TmdbProviderItemDto> rent = new ArrayList<>();

    @SerializedName("buy")
    private List<TmdbProviderItemDto> buy = new ArrayList<>();

    public TmdbCountryProvidersDto() {
    }

    public String getLink() {
        return link;
    }

    public List<TmdbProviderItemDto> getFlatrate() {
        return (flatrate != null) ? flatrate : new ArrayList<>();
    }

    public List<TmdbProviderItemDto> getRent() {
        return (rent != null) ? rent : new ArrayList<>();
    }

    public List<TmdbProviderItemDto> getBuy() {
        return (buy != null) ? buy : new ArrayList<>();
    }
}
