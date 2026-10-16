package pt.isep.dssmv.projectdroid.network.dto;

import com.google.gson.annotations.SerializedName;

public class FsqLocationDto {

    @SerializedName("address")
    private String address;

    @SerializedName("formatted_address")
    private String formattedAddress;

    public FsqLocationDto() {
    }

    public String getAddress() {
        return address;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }
}
