package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import java.util.Objects;

public class WatchProvider {

    private int providerId;
    private String name;
    private String logoPath;
    private String accessType;

    public WatchProvider() {
    }

    public WatchProvider(int providerId, String name, String logoPath, String accessType) throws InvalidDataException {
        if (providerId <= 0) {
            throw new InvalidDataException("Provider ID must be positive");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("Provider name cannot be empty");
        }
        this.providerId = providerId;
        this.name = name.trim();
        this.logoPath = logoPath;
        this.accessType = accessType;
    }

    public int getProviderId() {
        return providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WatchProvider that)) return false;
        return providerId == that.providerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId);
    }

    @Override
    public String toString() {
        return name + " (" + accessType + ")";
    }
}
