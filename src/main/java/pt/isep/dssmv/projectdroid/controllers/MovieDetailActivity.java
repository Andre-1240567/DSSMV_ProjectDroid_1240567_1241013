package pt.isep.dssmv.projectdroid.controllers;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.WatchProviderAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.MovieList;
import pt.isep.dssmv.projectdroid.model.Rating;
import pt.isep.dssmv.projectdroid.model.SavedMovie;
import pt.isep.dssmv.projectdroid.model.WatchProvider;
import pt.isep.dssmv.projectdroid.model.WatchStatus;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.TmdbRatingManager;
import pt.isep.dssmv.projectdroid.network.dto.TmdbCountryProvidersDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbProviderItemDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbVideosResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbWatchProvidersResponse;
import pt.isep.dssmv.projectdroid.utils.ShareHelper;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MovieDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TMDB_ID = "extra_tmdb_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_POSTER_PATH = "extra_poster_path";
    public static final String EXTRA_OVERVIEW = "extra_overview";
    public static final String EXTRA_RELEASE_YEAR = "extra_release_year";

    private Button buttonBack;
    private ImageButton buttonShareMovie;
    private TextView textViewHeaderTitle;
    private ImageView imageViewPoster;
    private TextView textViewTitle;
    private TextView textViewYear;
    private TextView textViewOverview;
    private Button buttonWatchTrailer;
    private Button buttonDetailAddToList;
    private TextView textViewProvidersHeader;
    private ProgressBar progressBarProviders;
    private TextView textViewNoProviders;
    private RecyclerView recyclerViewProviders;

    private WatchProviderAdapter providerAdapter;
    private SharedPrefManager prefManager;

    private int tmdbId;
    private String movieTitle;
    private String moviePosterPath;
    private String movieOverview;
    private int movieReleaseYear;

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
        buttonShareMovie = findViewById(R.id.buttonDetailShare);
        textViewHeaderTitle = findViewById(R.id.textViewDetailHeaderTitle);
        imageViewPoster = findViewById(R.id.imageViewDetailPoster);
        textViewTitle = findViewById(R.id.textViewDetailTitle);
        textViewYear = findViewById(R.id.textViewDetailYear);
        textViewOverview = findViewById(R.id.textViewDetailOverview);
        buttonWatchTrailer = findViewById(R.id.buttonWatchTrailer);
        buttonDetailAddToList = findViewById(R.id.buttonDetailAddToList);
        textViewProvidersHeader = findViewById(R.id.textViewProvidersSectionHeader);
        progressBarProviders = findViewById(R.id.progressBarProviders);
        textViewNoProviders = findViewById(R.id.textViewNoProviders);
        recyclerViewProviders = findViewById(R.id.recyclerViewWatchProviders);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        tmdbId = intent.getIntExtra(EXTRA_TMDB_ID, 0);
        movieTitle = intent.getStringExtra(EXTRA_TITLE);
        moviePosterPath = intent.getStringExtra(EXTRA_POSTER_PATH);
        movieOverview = intent.getStringExtra(EXTRA_OVERVIEW);
        movieReleaseYear = intent.getIntExtra(EXTRA_RELEASE_YEAR, 0);

        textViewHeaderTitle.setText(movieTitle != null ? movieTitle : getString(R.string.title_movie_detail));
        textViewTitle.setText(movieTitle != null ? movieTitle : "");

        if (movieReleaseYear > 0) {
            textViewYear.setText(String.valueOf(movieReleaseYear));
            textViewYear.setVisibility(View.VISIBLE);
        } else {
            textViewYear.setVisibility(View.GONE);
        }

        if (movieOverview != null && !movieOverview.trim().isEmpty()) {
            textViewOverview.setText(movieOverview);
        } else {
            textViewOverview.setText("No synopsis available.");
        }

        String posterUrl = (moviePosterPath != null && !moviePosterPath.trim().isEmpty())
                ? "https://image.tmdb.org/t/p/w500" + moviePosterPath
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
        buttonShareMovie.setOnClickListener(v -> ShareHelper.shareMovie(this, movieTitle, movieReleaseYear, movieOverview, tmdbId));
        buttonWatchTrailer.setOnClickListener(v -> loadAndPlayOfficialTrailer());
        buttonDetailAddToList.setOnClickListener(v -> handleAddMovieToList());
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

    private void handleAddMovieToList() {
        FirebaseUser user = FirebaseAuthManager.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to add movies to your lists", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonDetailAddToList.setEnabled(false);
        FirestoreManager.getInstance().getUserMovieLists(user.getUid(), new FirestoreManager.FirestoreCallback<List<MovieList>>() {
            @Override
            public void onSuccess(List<MovieList> userLists) {
                buttonDetailAddToList.setEnabled(true);
                if (isFinishing() || isDestroyed()) return;

                if (userLists == null || userLists.isEmpty()) {
                    new AlertDialog.Builder(MovieDetailActivity.this)
                            .setTitle(R.string.dialog_title_add_to_list)
                            .setMessage(R.string.prompt_no_lists_create_first)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    showAddMovieToListDialog(userLists);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                buttonDetailAddToList.setEnabled(true);
                Toast.makeText(MovieDetailActivity.this, "Failed to load lists: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddMovieToListDialog(List<MovieList> userLists) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_movie_to_list, null);
        TextView textViewTitle = dialogView.findViewById(R.id.textViewDialogMovieTitle);
        Spinner spinnerTargetList = dialogView.findViewById(R.id.spinnerTargetList);
        Spinner spinnerMovieStatus = dialogView.findViewById(R.id.spinnerMovieStatus);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarAddToList);
        TextView textViewRatingLabel = dialogView.findViewById(R.id.textViewRatingScoreLabel);
        EditText editTextComment = dialogView.findViewById(R.id.editTextDialogComment);

        String titleWithYear = (movieTitle != null ? movieTitle : "") + (movieReleaseYear > 0 ? " (" + movieReleaseYear + ")" : "");
        textViewTitle.setText(titleWithYear);

        List<String> listNames = new ArrayList<>();
        for (MovieList ml : userLists) {
            listNames.add(ml.getName());
        }
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listNames);
        spinnerTargetList.setAdapter(listAdapter);

        String[] statuses = new String[]{
                getString(R.string.status_plan_to_watch),
                getString(R.string.status_watching),
                getString(R.string.status_watched),
                getString(R.string.status_favorite)
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerMovieStatus.setAdapter(statusAdapter);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating > 0) {
                textViewRatingLabel.setText(String.format(Locale.getDefault(), "★ %.1f / 5.0", rating));
            } else {
                textViewRatingLabel.setText(R.string.no_rating_yet);
            }
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_add, (dialog, which) -> {
                    int selectedIndex = spinnerTargetList.getSelectedItemPosition();
                    if (selectedIndex >= 0 && selectedIndex < userLists.size()) {
                        MovieList targetList = userLists.get(selectedIndex);
                        WatchStatus status = parseSelectedStatus(spinnerMovieStatus.getSelectedItemPosition());
                        float score = ratingBar.getRating();
                        String comment = (editTextComment != null && editTextComment.getText() != null)
                                ? editTextComment.getText().toString().trim() : "";

                        saveMovieToTargetList(targetList, status, score, comment);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private WatchStatus parseSelectedStatus(int position) {
        return switch (position) {
            case 1 -> WatchStatus.WATCHING;
            case 2 -> WatchStatus.WATCHED;
            case 3 -> WatchStatus.FAVORITE;
            default -> WatchStatus.PLAN_TO_WATCH;
        };
    }

    private void saveMovieToTargetList(MovieList targetList, WatchStatus status, float score, String comment) {
        FirebaseUser user = FirebaseAuthManager.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            String internalId = UUID.randomUUID().toString();
            int validTmdbId = tmdbId > 0 ? tmdbId : 1;
            String safeTitle = movieTitle != null ? movieTitle : "Untitled";

            SavedMovie savedMovie = new SavedMovie(internalId, validTmdbId, safeTitle);
            savedMovie.setReleaseYear(movieReleaseYear);
            savedMovie.setOverview(movieOverview != null ? movieOverview : "");
            savedMovie.setPosterPath(moviePosterPath != null ? moviePosterPath : "");
            savedMovie.setStatus(status);

            if (score >= 1.0f) {
                Rating rating = new Rating(UUID.randomUUID().toString(), score, comment);
                savedMovie.setRating(rating);
            }

            FirestoreManager.getInstance().addMovieToList(user.getUid(), targetList.getListId(), savedMovie, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(MovieDetailActivity.this, getString(R.string.movie_added_to_list_success, targetList.getName()), Toast.LENGTH_SHORT).show();

                    // HTTP POST rating to TMDB REST API if rated
                    if (score >= 1.0f && validTmdbId > 0) {
                        TmdbRatingManager.getInstance(MovieDetailActivity.this)
                                .submitRating(validTmdbId, score, null);
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MovieDetailActivity.this, "Failed to add movie: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidDataException | InvalidRatingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
