package pt.isep.dssmv.projectdroid.network;

import pt.isep.dssmv.projectdroid.network.dto.FsqPlacesResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface FoursquareApiService {

    @GET("places/search")
    Call<FsqPlacesResponse> searchNearbyCinemas(
            @Header("Authorization") String authorization,
            @Query("ll") String latLng,
            @Query("categories") String categories,
            @Query("radius") int radiusMeters,
            @Query("sort") String sort,
            @Query("limit") int limit
    );
}
