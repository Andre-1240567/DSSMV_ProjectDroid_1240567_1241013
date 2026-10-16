package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class FsqPlacesResponse {

    @SerializedName("results")
    private List<FsqPlaceDto> results = new ArrayList<>();

    public FsqPlacesResponse() {
    }

    public List<FsqPlaceDto> getResults() {
        return (results != null) ? results : new ArrayList<>();
    }
}
