package pt.isep.dssmv.projectdroid.network;

import pt.isep.dssmv.projectdroid.network.dto.TmdbGuestSessionResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingRequest;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbSearchResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbVideosResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbWatchProvidersResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {

    @GET("movie/popular")
    Call<TmdbPagedResponse> getPopularMovies(
            @Query("page") int page,
            @Query("language") String language
    );

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

    @GET("discover/movie")
    Call<TmdbSearchResponse> discoverMovies(
            @Query("language") String language,
            @Query("sort_by") String sortBy,
            @Query("vote_count.gte") Integer minVoteCount,
            @Query("vote_average.gte") Float minVoteAverage,
            @Query("primary_release_date.gte") String minReleaseDate,
            @Query("watch_region") String watchRegion,
            @Query("with_watch_providers") String withWatchProviders,
            @Query("page") int page
    );

    @GET("authentication/guest_session/new")
    Call<TmdbGuestSessionResponse> createGuestSession();

    @POST("movie/{movie_id}/rating")
    Call<TmdbRatingResponse> postMovieRating(
            @Path("movie_id") int movieId,
            @Query("guest_session_id") String guestSessionId,
            @Body TmdbRatingRequest body
    );

    @DELETE("movie/{movie_id}/rating")
    Call<TmdbRatingResponse> deleteMovieRating(
            @Path("movie_id") int movieId,
            @Query("guest_session_id") String guestSessionId
    );
}
