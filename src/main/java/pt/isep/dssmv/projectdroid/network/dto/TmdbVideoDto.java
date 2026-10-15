package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbVideoDto {

    @SerializedName("id")
    private String id;

    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("site")
    private String site;

    @SerializedName("type")
    private String type;

    @SerializedName("official")
    private boolean official;

    public TmdbVideoDto() {
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getSite() {
        return site;
    }

    public String getType() {
        return type;
    }

    public boolean isOfficial() {
        return official;
    }

    public boolean isYouTubeTrailer() {
        return "YouTube".equalsIgnoreCase(site) && "Trailer".equalsIgnoreCase(type);
    }
}
