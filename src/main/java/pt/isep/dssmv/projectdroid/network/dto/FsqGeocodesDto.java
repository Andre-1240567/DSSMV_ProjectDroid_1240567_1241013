package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class FsqGeocodesDto {

    @SerializedName("main")
    private FsqGeoPointDto main;

    public FsqGeocodesDto() {
    }

    public FsqGeoPointDto getMain() {
        return main;
    }
}
