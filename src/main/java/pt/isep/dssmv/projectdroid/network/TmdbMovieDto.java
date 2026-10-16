package pt.isep.dssmv.projectdroid.network;

import com.google.gson.annotations.SerializedName;

public class TmdbMovieDto {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("overview")
    private String overview;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("vote_average")
    private double voteAverage;

    public TmdbMovieDto() {
    }

    public TmdbMovieDto(int id, String title, String overview, String posterPath, String releaseDate) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.posterPath = posterPath;
        this.releaseDate = releaseDate;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public String getOverview() {
        return overview != null ? overview : "";
    }

    public String getReleaseDate() {
        return releaseDate != null ? releaseDate : "";
    }

    public int getReleaseYear() {
        if (releaseDate != null && releaseDate.length() >= 4) {
            try {
                return Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public String getPosterPath() {
        return posterPath != null ? posterPath : "";
    }

    public String getFullPosterUrl() {
        if (posterPath != null && !posterPath.trim().isEmpty()) {
            return "https://image.tmdb.org/t/p/w500" + posterPath;
        }
        return null;
    }

    public double getVoteAverage() {
        return voteAverage;
    }
}
