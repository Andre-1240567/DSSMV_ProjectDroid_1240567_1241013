package pt.isep.dssmv.projectdroid.network;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TmdbPagedResponse {

    @SerializedName("page")
    private int page;

    @SerializedName("results")
    private List<TmdbMovieDto> results;

    @SerializedName("total_pages")
    private int totalPages;

    @SerializedName("total_results")
    private int totalResults;

    public int getPage() {
        return page;
    }

    public List<TmdbMovieDto> getResults() {
        return results != null ? results : new ArrayList<>();
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalResults() {
        return totalResults;
    }
}
