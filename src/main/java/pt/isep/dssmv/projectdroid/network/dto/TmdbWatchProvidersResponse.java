package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class TmdbWatchProvidersResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("results")
    private Map<String, TmdbCountryProvidersDto> results = new HashMap<>();

    public TmdbWatchProvidersResponse() {
    }

    public int getId() {
        return id;
    }

    public Map<String, TmdbCountryProvidersDto> getResults() {
        return (results != null) ? results : new HashMap<>();
    }

    public TmdbCountryProvidersDto getProvidersForCountry(String countryCode) {
        if (results == null || countryCode == null) {
            return null;
        }
        return results.get(countryCode.trim().toUpperCase());
    }
}
