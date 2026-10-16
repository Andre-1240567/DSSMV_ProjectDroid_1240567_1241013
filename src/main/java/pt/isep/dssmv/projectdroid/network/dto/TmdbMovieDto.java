package pt.isep.dssmv.projectdroid.network.dto;

public class TmdbMovieDto extends pt.isep.dssmv.projectdroid.network.TmdbMovieDto {

    public TmdbMovieDto() {
        super();
    }

    public TmdbMovieDto(int id, String title, String overview, String posterPath, String releaseDate) {
        super(id, title, overview, posterPath, releaseDate);
    }
}
