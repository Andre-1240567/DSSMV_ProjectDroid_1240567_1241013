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

    public TmdbVideoDto(String id, String key, String name, String site, String type, boolean official) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.site = site;
        this.type = type;
        this.official = official;
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
