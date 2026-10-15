package pt.isep.dssmv.projectdroid.network;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import pt.isep.dssmv.projectdroid.BuildConfig;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3/";

    private static RetrofitClient instance;
    private final TmdbApiService tmdbApiService;
    private final FoursquareApiService foursquareApiService;

    private RetrofitClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Security / Injection Interceptor (AC03): injects BuildConfig.TMDB_API_KEY from .env
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                String apiKey = BuildConfig.TMDB_API_KEY;

                Request.Builder requestBuilder = original.newBuilder();

                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    apiKey = apiKey.trim();
                    if (apiKey.startsWith("eyJ")) {
                        // v4 Bearer Token
                        requestBuilder.header("Authorization", "Bearer " + apiKey);
                    } else {
                        // v3 API Key query parameter
                        HttpUrl originalUrl = original.url();
                        HttpUrl urlWithKey = originalUrl.newBuilder()
                                .addQueryParameter("api_key", apiKey)
                                .build();
                        requestBuilder.url(urlWithKey);
                    }
                }

                return chain.proceed(requestBuilder.build());
            }
        };

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TMDB_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.tmdbApiService = retrofit.create(TmdbApiService.class);

        // Foursquare Places API client setup (US5 AC03 & AC04)
        Interceptor fsqAuthInterceptor = chain -> {
            Request original = chain.request();
            String fsqKey = BuildConfig.FOURSQUARE_API_KEY;
            Request.Builder b = original.newBuilder()
                    .header("Accept", "application/json");
            if (fsqKey != null && !fsqKey.trim().isEmpty()) {
                b.header("Authorization", fsqKey.trim());
            }
            return chain.proceed(b.build());
        };

        OkHttpClient fsqOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(fsqAuthInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit fsqRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.foursquare.com/v3/")
                .client(fsqOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.foursquareApiService = fsqRetrofit.create(FoursquareApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public TmdbApiService getTmdbApiService() {
        return tmdbApiService;
    }

    public FoursquareApiService getFoursquareApiService() {
        return foursquareApiService;
    }
}
