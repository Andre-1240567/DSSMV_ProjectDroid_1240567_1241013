package pt.isep.dssmv.projectdroid.network;

import pt.isep.dssmv.projectdroid.network.dto.TmdbSearchResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbVideosResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbWatchProvidersResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {

    @GET("movie/popular")
    Call<TmdbPagedResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("page") int page,
            @Query("language") String language
    );

    @GET("search/movie")
    Call<TmdbSearchResponse> searchMovies(
            @Query("query") String query,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("movie/{movie_id}/watch/providers")
    Call<TmdbWatchProvidersResponse> getWatchProviders(
            @Path("movie_id") int movieId
    );

    @GET("movie/{movie_id}/videos")
    Call<TmdbVideosResponse> getMovieVideos(
            @Path("movie_id") int movieId
    );
}
