package pt.isep.dssmv.projectdroid.controllers;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.WatchProviderAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.model.WatchProvider;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.dto.TmdbCountryProvidersDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbProviderItemDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbVideosResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbWatchProvidersResponse;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TMDB_ID = "extra_tmdb_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_POSTER_PATH = "extra_poster_path";
    public static final String EXTRA_OVERVIEW = "extra_overview";
    public static final String EXTRA_RELEASE_YEAR = "extra_release_year";

    private Button buttonBack;
    private TextView textViewHeaderTitle;
    private ImageView imageViewPoster;
    private TextView textViewTitle;
    private TextView textViewYear;
    private TextView textViewOverview;
    private Button buttonWatchTrailer;
    private TextView textViewProvidersHeader;
    private ProgressBar progressBarProviders;
    private TextView textViewNoProviders;
    private RecyclerView recyclerViewProviders;

    private WatchProviderAdapter providerAdapter;
    private SharedPrefManager prefManager;

    private int tmdbId;
    private String movieTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        prefManager = SharedPrefManager.getInstance(this);

        initViews();
        extractIntentData();
        setupRecyclerView();
        setupListeners();
        loadWatchProviders();
    }

    private void initViews() {
        buttonBack = findViewById(R.id.buttonDetailBack);
        textViewHeaderTitle = findViewById(R.id.textViewDetailHeaderTitle);
        imageViewPoster = findViewById(R.id.imageViewDetailPoster);
        textViewTitle = findViewById(R.id.textViewDetailTitle);
        textViewYear = findViewById(R.id.textViewDetailYear);
        textViewOverview = findViewById(R.id.textViewDetailOverview);
        buttonWatchTrailer = findViewById(R.id.buttonWatchTrailer);
        textViewProvidersHeader = findViewById(R.id.textViewProvidersSectionHeader);
        progressBarProviders = findViewById(R.id.progressBarProviders);
        textViewNoProviders = findViewById(R.id.textViewNoProviders);
        recyclerViewProviders = findViewById(R.id.recyclerViewWatchProviders);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        tmdbId = intent.getIntExtra(EXTRA_TMDB_ID, 0);
        movieTitle = intent.getStringExtra(EXTRA_TITLE);
        String posterPath = intent.getStringExtra(EXTRA_POSTER_PATH);
        String overview = intent.getStringExtra(EXTRA_OVERVIEW);
        int releaseYear = intent.getIntExtra(EXTRA_RELEASE_YEAR, 0);

        textViewHeaderTitle.setText(movieTitle != null ? movieTitle : getString(R.string.title_movie_detail));
        textViewTitle.setText(movieTitle != null ? movieTitle : "");

        if (releaseYear > 0) {
            textViewYear.setText(String.valueOf(releaseYear));
            textViewYear.setVisibility(View.VISIBLE);
        } else {
            textViewYear.setVisibility(View.GONE);
        }

        if (overview != null && !overview.trim().isEmpty()) {
            textViewOverview.setText(overview);
        } else {
            textViewOverview.setText("No synopsis available.");
        }

        String posterUrl = (posterPath != null && !posterPath.trim().isEmpty())
                ? "https://image.tmdb.org/t/p/w500" + posterPath
                : null;

        Glide.with(this)
                .load(posterUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_error)
                .into(imageViewPoster);
    }

    private void setupRecyclerView() {
        providerAdapter = new WatchProviderAdapter();
        recyclerViewProviders.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewProviders.setAdapter(providerAdapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonWatchTrailer.setOnClickListener(v -> loadAndPlayOfficialTrailer());
    }

    /**
     * AC05: Fetches watch providers from TMDB API and maps them to pure WatchProvider domain entities
     * for the region configured in user preferences (default "PT").
     */
    private void loadWatchProviders() {
        if (tmdbId <= 0) return;

        String region = prefManager.getDefaultRegion();
        textViewProvidersHeader.setText(getString(R.string.section_streaming_providers, region));

        progressBarProviders.setVisibility(View.VISIBLE);
        textViewNoProviders.setVisibility(View.GONE);
        recyclerViewProviders.setVisibility(View.GONE);

        RetrofitClient.getInstance().getTmdbApiService()
                .getWatchProviders(tmdbId)
                .enqueue(new Callback<TmdbWatchProvidersResponse>() {
                    @Override
                    public void onResponse(Call<TmdbWatchProvidersResponse> call, Response<TmdbWatchProvidersResponse> response) {
                        progressBarProviders.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            TmdbWatchProvidersResponse body = response.body();
                            TmdbCountryProvidersDto countryProviders = body.getProvidersForCountry(region);

                            List<WatchProvider> domainProviders = mapToDomainProviders(countryProviders);

                            if (domainProviders.isEmpty()) {
                                textViewNoProviders.setText(getString(R.string.no_streaming_providers, region));
                                textViewNoProviders.setVisibility(View.VISIBLE);
                                recyclerViewProviders.setVisibility(View.GONE);
                            } else {
                                providerAdapter.setProviders(domainProviders);
                                recyclerViewProviders.setVisibility(View.VISIBLE);
                                textViewNoProviders.setVisibility(View.GONE);
                            }
                        } else {
                            textViewNoProviders.setText(getString(R.string.no_streaming_providers, region));
                            textViewNoProviders.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbWatchProvidersResponse> call, Throwable t) {
                        progressBarProviders.setVisibility(View.GONE);
                        textViewNoProviders.setText(getString(R.string.error_loading_providers, t.getMessage()));
                        textViewNoProviders.setVisibility(View.VISIBLE);
                    }
                });
    }

    private List<WatchProvider> mapToDomainProviders(TmdbCountryProvidersDto dto) {
        List<WatchProvider> list = new ArrayList<>();
        if (dto == null) return list;

        // Map flatrate (subscription streaming)
        for (TmdbProviderItemDto item : dto.getFlatrate()) {
            try {
                list.add(new WatchProvider(item.getProviderId(), item.getProviderName(), item.getLogoPath(), "flatrate"));
            } catch (InvalidDataException ignored) {
            }
        }

        // Map rent
        for (TmdbProviderItemDto item : dto.getRent()) {
            try {
                list.add(new WatchProvider(item.getProviderId(), item.getProviderName(), item.getLogoPath(), "rent"));
            } catch (InvalidDataException ignored) {
            }
        }

        // Map buy
        for (TmdbProviderItemDto item : dto.getBuy()) {
            try {
                list.add(new WatchProvider(item.getProviderId(), item.getProviderName(), item.getLogoPath(), "buy"));
            } catch (InvalidDataException ignored) {
            }
        }

        return list;
    }

    /**
     * US10: Queries official trailer from TMDB and launches implicit Intent with native YouTube or web fallback.
     */
    private void loadAndPlayOfficialTrailer() {
        if (tmdbId <= 0) return;

        buttonWatchTrailer.setEnabled(false);

        RetrofitClient.getInstance().getTmdbApiService()
                .getMovieVideos(tmdbId)
                .enqueue(new Callback<TmdbVideosResponse>() {
                    @Override
                    public void onResponse(Call<TmdbVideosResponse> call, Response<TmdbVideosResponse> response) {
                        buttonWatchTrailer.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            String videoKey = response.body().findOfficialYouTubeTrailerKey();
                            if (videoKey != null) {
                                openYouTubeVideo(videoKey);
                            } else {
                                Toast.makeText(MovieDetailActivity.this, R.string.error_no_trailer, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MovieDetailActivity.this, R.string.error_no_trailer, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbVideosResponse> call, Throwable t) {
                        buttonWatchTrailer.setEnabled(true);
                        Toast.makeText(MovieDetailActivity.this, getString(R.string.error_loading_trailer, t.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openYouTubeVideo(String videoKey) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoKey));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoKey));

        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            // Robust fallback to web browser
            startActivity(webIntent);
        }
    }
}
