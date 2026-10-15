package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TmdbVideosResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("results")
    private List<TmdbVideoDto> results = new ArrayList<>();

    public TmdbVideosResponse() {
    }

    public int getId() {
        return id;
    }

    public List<TmdbVideoDto> getResults() {
        return (results != null) ? results : new ArrayList<>();
    }

    public String findOfficialYouTubeTrailerKey() {
        if (results == null) return null;

        // First pass: official YouTube trailer
        for (TmdbVideoDto video : results) {
            if (video.isYouTubeTrailer() && video.isOfficial()) {
                return video.getKey();
            }
        }
        // Second pass: any YouTube trailer
        for (TmdbVideoDto video : results) {
            if (video.isYouTubeTrailer()) {
                return video.getKey();
            }
        }
        // Third pass: any YouTube video (teaser/clip)
        for (TmdbVideoDto video : results) {
            if ("YouTube".equalsIgnoreCase(video.getSite()) && video.getKey() != null) {
                return video.getKey();
            }
        }
        return null;
    }
}
