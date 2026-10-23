package pt.isep.dssmv.projectdroid.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import pt.isep.dssmv.projectdroid.network.dto.TmdbGuestSessionResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingRequest;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingResponse;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manager handling TMDB rating mutations via HTTP POST and DELETE.
 * Satisfies the DSSMV project requirement for REST mutations (POST/DELETE) with JSON @Body serialization.
 * Manages TMDB guest sessions automatically without requiring user login credentials.
 */
public class TmdbRatingManager {

    private static final String TAG = "TmdbRatingManager";
    private static TmdbRatingManager instance;

    private final TmdbApiService apiService;
    private final SharedPrefManager prefManager;

    public interface RatingCallback {
        void onSuccess(TmdbRatingResponse response);
        void onFailure(String errorMessage);
    }

    private TmdbRatingManager(Context context) {
        this.apiService = RetrofitClient.getInstance().getTmdbApiService();
        this.prefManager = SharedPrefManager.getInstance(context);
    }

    public static synchronized TmdbRatingManager getInstance(Context context) {
        if (instance == null) {
            instance = new TmdbRatingManager(context);
        }
        return instance;
    }

    /**
     * Submits a rating to TMDB via HTTP POST /3/movie/{movie_id}/rating.
     * Maps the local 1.0 - 5.0 star scale to TMDB's 0.5 - 10.0 scale (value = stars * 2.0).
     *
     * @param tmdbId Movie ID on TMDB.
     * @param stars Rating score in stars (1.0 to 5.0).
     * @param callback Optional async callback.
     */
    public void submitRating(int tmdbId, float stars, RatingCallback callback) {
        if (tmdbId <= 0) {
            if (callback != null) {
                callback.onFailure("Invalid TMDB movie ID: " + tmdbId);
            }
            return;
        }

        // Scale conversion: CineTrack [1.0, 5.0] -> TMDB [0.5, 10.0]
        double tmdbScore = Math.min(10.0, Math.max(0.5, stars * 2.0));

        String guestSessionId = prefManager.getTmdbGuestSessionId();
        if (guestSessionId == null || guestSessionId.trim().isEmpty()) {
            fetchNewSessionAndPostRating(tmdbId, tmdbScore, callback);
        } else {
            postRatingWithSession(tmdbId, guestSessionId, tmdbScore, callback, true);
        }
    }

    private void postRatingWithSession(int tmdbId, String sessionId, double tmdbScore, RatingCallback callback, boolean canRetry) {
        TmdbRatingRequest requestBody = new TmdbRatingRequest(tmdbScore);
        apiService.postMovieRating(tmdbId, sessionId, requestBody).enqueue(new Callback<TmdbRatingResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbRatingResponse> call, @NonNull Response<TmdbRatingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "Rating posted successfully to TMDB for movie " + tmdbId + ": " + response.body().getStatusMessage());
                    if (callback != null) {
                        callback.onSuccess(response.body());
                    }
                } else if ((response.code() == 401 || (response.body() != null && response.body().getStatusCode() == 3)) && canRetry) {
                    // Guest session expired, request a fresh session and retry once
                    Log.w(TAG, "Guest session expired or unauthorized. Refreshing guest session...");
                    fetchNewSessionAndPostRating(tmdbId, tmdbScore, callback);
                } else {
                    String msg = (response.body() != null && response.body().getStatusMessage() != null)
                            ? response.body().getStatusMessage()
                            : "HTTP " + response.code();
                    Log.e(TAG, "TMDB rating POST failed: " + msg);
                    if (callback != null) {
                        callback.onFailure(msg);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbRatingResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error posting TMDB rating", t);
                if (callback != null) {
                    callback.onFailure(t.getMessage());
                }
            }
        });
    }

    private void fetchNewSessionAndPostRating(int tmdbId, double tmdbScore, RatingCallback callback) {
        apiService.createGuestSession().enqueue(new Callback<TmdbGuestSessionResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbGuestSessionResponse> call, @NonNull Response<TmdbGuestSessionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String newSessionId = response.body().getGuestSessionId();
                    prefManager.saveTmdbGuestSessionId(newSessionId);
                    Log.d(TAG, "Acquired new TMDB guest session: " + newSessionId);
                    postRatingWithSession(tmdbId, newSessionId, tmdbScore, callback, false);
                } else {
                    String msg = "Failed to create TMDB guest session: HTTP " + response.code();
                    Log.e(TAG, msg);
                    if (callback != null) {
                        callback.onFailure(msg);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbGuestSessionResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error obtaining TMDB guest session", t);
                if (callback != null) {
                    callback.onFailure(t.getMessage());
                }
            }
        });
    }

    /**
     * Deletes a movie rating on TMDB via HTTP DELETE /3/movie/{movie_id}/rating.
     *
     * @param tmdbId Movie ID on TMDB.
     * @param callback Optional async callback.
     */
    public void deleteRating(int tmdbId, RatingCallback callback) {
        if (tmdbId <= 0) {
            if (callback != null) {
                callback.onFailure("Invalid TMDB movie ID: " + tmdbId);
            }
            return;
        }

        String guestSessionId = prefManager.getTmdbGuestSessionId();
        if (guestSessionId == null || guestSessionId.trim().isEmpty()) {
            if (callback != null) {
                callback.onFailure("No active TMDB session to delete rating");
            }
            return;
        }

        apiService.deleteMovieRating(tmdbId, guestSessionId).enqueue(new Callback<TmdbRatingResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbRatingResponse> call, @NonNull Response<TmdbRatingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "Rating deleted on TMDB for movie " + tmdbId);
                    if (callback != null) {
                        callback.onSuccess(response.body());
                    }
                } else {
                    String msg = (response.body() != null && response.body().getStatusMessage() != null)
                            ? response.body().getStatusMessage()
                            : "HTTP " + response.code();
                    if (callback != null) {
                        callback.onFailure(msg);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbRatingResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error deleting TMDB rating", t);
                if (callback != null) {
                    callback.onFailure(t.getMessage());
                }
            }
        });
    }
}
