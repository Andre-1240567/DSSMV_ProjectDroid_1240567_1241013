package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import java.util.Objects;

public class SavedMovie {

    private String internalId;
    private int tmdbId;
    private String title;
    private String posterPath;
    private int releaseYear;
    private String overview;
    private WatchStatus status;

    public SavedMovie() {
        this.status = WatchStatus.PLAN_TO_WATCH;
    }

    public SavedMovie(String internalId, int tmdbId, String title) throws InvalidDataException {
        setInternalId(internalId);
        setTmdbId(tmdbId);
        setTitle(title);
        this.status = WatchStatus.PLAN_TO_WATCH;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) throws InvalidDataException {
        if (internalId == null || internalId.trim().isEmpty()) {
            throw new InvalidDataException("Internal ID cannot be empty");
        }
        this.internalId = internalId.trim();
    }

    public int getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(int tmdbId) throws InvalidDataException {
        if (tmdbId <= 0) {
            throw new InvalidDataException("TMDB ID must be a positive integer");
        }
        this.tmdbId = tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) throws InvalidDataException {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidDataException("Movie title cannot be empty");
        }
        this.title = title.trim();
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public WatchStatus getStatus() {
        return status;
    }

    public void setStatus(WatchStatus status) {
        this.status = (status != null) ? status : WatchStatus.PLAN_TO_WATCH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedMovie that)) return false;
        return tmdbId == that.tmdbId && Objects.equals(internalId, that.internalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalId, tmdbId);
    }

    @Override
    public String toString() {
        if (releaseYear > 0) {
            return title + " (" + releaseYear + ")";
        }
        return title;
    }
}
