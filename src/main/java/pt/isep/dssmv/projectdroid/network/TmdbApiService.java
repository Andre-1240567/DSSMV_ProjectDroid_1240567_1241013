package pt.isep.dssmv.projectdroid.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TmdbApiService {

    @GET("movie/popular")
    Call<TmdbPagedResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("page") int page,
            @Query("language") String language
    );
}
