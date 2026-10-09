package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import java.util.Objects;

public class Cinema {

    private String fsqId;
    private String name;
    private double latitude;
    private double longitude;
    private String address;
    private int distanceMeters;

    public Cinema() {
    }

    public Cinema(String fsqId, String name, double latitude, double longitude) throws InvalidDataException {
        setFsqId(fsqId);
        setName(name);
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = "";
        this.distanceMeters = 0;
    }

    public String getFsqId() {
        return fsqId;
    }

    public void setFsqId(String fsqId) throws InvalidDataException {
        if (fsqId == null || fsqId.trim().isEmpty()) {
            throw new InvalidDataException("Foursquare ID cannot be empty");
        }
        this.fsqId = fsqId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidDataException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("Cinema name cannot be empty");
        }
        this.name = name.trim();
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = (address != null) ? address.trim() : "";
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(int distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cinema cinema)) return false;
        return Objects.equals(fsqId, cinema.fsqId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fsqId);
    }

    @Override
    public String toString() {
        if (distanceMeters > 0) {
            return name + " (" + distanceMeters + "m)";
        }
        return name;
    }
}
