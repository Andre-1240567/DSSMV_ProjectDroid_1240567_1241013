package pt.isep.dssmv.projectdroid.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.network.dto.TmdbGuestSessionResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingRequest;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingResponse;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TmdbRatingIntegrationTest {

    private static final String TMDB_API_KEY = "156bc911e505361a49c26966626bc5f0";

    @Test
    public void testLiveTmdbGuestSessionAndRatingPostFlow() throws IOException {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> System.out.println("[TMDB-HTTP-LOG] " + message));
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request reqWithKey = original.newBuilder()
                            .url(original.url().newBuilder().addQueryParameter("api_key", TMDB_API_KEY).build())
                            .build();
                    return chain.proceed(reqWithKey);
                })
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TmdbApiService service = retrofit.create(TmdbApiService.class);

        // 1. GET Guest Session
        Response<TmdbGuestSessionResponse> sessionRes = service.createGuestSession().execute();
        assertTrue(sessionRes.isSuccessful(), "Guest session creation failed: " + sessionRes.code());
        assertNotNull(sessionRes.body());
        assertTrue(sessionRes.body().isSuccess());
        String guestSessionId = sessionRes.body().getGuestSessionId();
        assertNotNull(guestSessionId);
        assertFalse(guestSessionId.trim().isEmpty());

        // 2. POST Rating (Fight Club - Movie ID 550, Score 8.5)
        TmdbRatingRequest ratingRequest = new TmdbRatingRequest(8.5);
        Response<TmdbRatingResponse> postRes = service.postMovieRating(550, guestSessionId, ratingRequest).execute();
        assertTrue(postRes.isSuccessful(), "Rating POST failed: " + postRes.code());
        assertNotNull(postRes.body());
        assertTrue(postRes.body().isSuccess());
        assertEquals(1, postRes.body().getStatusCode()); // 1 = "Success."

        // 3. DELETE Rating
        Response<TmdbRatingResponse> delRes = service.deleteMovieRating(550, guestSessionId).execute();
        assertTrue(delRes.isSuccessful(), "Rating DELETE failed: " + delRes.code());
        assertNotNull(delRes.body());
        assertTrue(delRes.body().isSuccess());
    }
}
