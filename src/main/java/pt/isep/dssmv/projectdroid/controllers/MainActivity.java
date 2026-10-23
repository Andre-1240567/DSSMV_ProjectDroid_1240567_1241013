package pt.isep.dssmv.projectdroid.controllers;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import pt.isep.dssmv.projectdroid.BuildConfig;
import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.MovieListAdapter;
import pt.isep.dssmv.projectdroid.adapters.TmdbMovieAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.MovieList;
import pt.isep.dssmv.projectdroid.model.Rating;
import pt.isep.dssmv.projectdroid.model.SavedMovie;
import pt.isep.dssmv.projectdroid.model.UserPreferences;
import pt.isep.dssmv.projectdroid.model.WatchStatus;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.TmdbMovieDto;
import pt.isep.dssmv.projectdroid.network.TmdbPagedResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbSearchResponse;
import pt.isep.dssmv.projectdroid.utils.ShakeDetector;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Common views
    private ImageButton buttonSearchMovies;
    private ImageButton buttonPreferences;
    private BottomNavigationView bottomNavigationView;

    // Tab 1: Explore / Popular Movies views
    private LinearLayout layoutExploreFeed;
    private RecyclerView recyclerViewExploreMovies;
    private ProgressBar progressBarExplore;
    private TextView textViewEmptyExplore;
    private TmdbMovieAdapter tmdbMovieAdapter;
    private int currentFeedPage = 1;
    private int totalFeedPages = 1;
    private boolean isLoadingFeed = false;

    // Tab 2: My Lists views
    private LinearLayout layoutMyLists;
    private TextView textViewWelcomeUser;
    private TextView textViewListsCount;
    private ImageButton buttonNearbyCinemas;
    private RecyclerView recyclerViewMovieLists;
    private ProgressBar progressBarLists;
    private LinearLayout layoutEmptyLists;
    private Button buttonCreateFirstList;
    private FloatingActionButton fabAddList;
    private MovieListAdapter movieListAdapter;
    private List<MovieList> userLists = new ArrayList<>();
    private ListenerRegistration listsRegistration;

    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private SharedPrefManager prefManager;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private Vibrator vibrator;

    private boolean isShakeDialogOpen = false;
    private boolean isShakeLoading = false;
    private AlertDialog currentShakeDialog = null;

    // Recommendation Cache & No-repeat history
    private final List<TmdbMovieDto> cachedRecommendations = new ArrayList<>();
    private final Set<Integer> shownRecommendationMovieIds = new HashSet<>();
    private int recommendationCurrentPage = 1;
    private int recommendationTotalPages = 1;
    private boolean isPrefetchingRecommendations = false;
    private int recommendationConfigVersion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        if (!authManager.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        initViews();
        setupNavigation();
        setupExploreFeed();
        setupMyLists();
        displayUserData();
        setupListeners();
        setupSensors();

        // Load initial feed of popular movies
        loadPopularMovies(1);

        // Preload pool of smart recommendations in cache on boot (no repeats, instant response)
        prefetchRecommendations();
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        float savedThreshold = prefManager.getShakeThreshold();
        shakeDetector = new ShakeDetector(this::handleDeviceShake, savedThreshold);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // AC04: Strictly register accelerometer in onResume()
        if (sensorManager != null && accelerometer != null && shakeDetector != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authManager.isUserLoggedIn()) {
            attachListsListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // AC04: Strictly unregister accelerometer in onPause() to preserve device battery
        if (sensorManager != null && shakeDetector != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
        if (currentShakeDialog != null && currentShakeDialog.isShowing()) {
            currentShakeDialog.dismiss();
            currentShakeDialog = null;
        }
        isShakeDialogOpen = false;
        isShakeLoading = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListsListener();
    }

    private void initViews() {
        buttonSearchMovies = findViewById(R.id.buttonSearchMovies);
        buttonPreferences = findViewById(R.id.buttonPreferences);
        buttonNearbyCinemas = findViewById(R.id.buttonNearbyCinemas);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Tab 1
        layoutExploreFeed = findViewById(R.id.layoutExploreFeed);
        recyclerViewExploreMovies = findViewById(R.id.recyclerViewExploreMovies);
        progressBarExplore = findViewById(R.id.progressBarExplore);
        textViewEmptyExplore = findViewById(R.id.textViewEmptyExplore);

        // Tab 2
        layoutMyLists = findViewById(R.id.layoutMyLists);
        textViewWelcomeUser = findViewById(R.id.textViewWelcomeUser);
        textViewListsCount = findViewById(R.id.textViewListsCount);
        recyclerViewMovieLists = findViewById(R.id.recyclerViewMovieLists);
        progressBarLists = findViewById(R.id.progressBarLists);
        layoutEmptyLists = findViewById(R.id.layoutEmptyLists);
        buttonCreateFirstList = findViewById(R.id.buttonCreateFirstList);
        fabAddList = findViewById(R.id.fabAddList);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_explore) {
                layoutExploreFeed.setVisibility(View.VISIBLE);
                layoutMyLists.setVisibility(View.GONE);
                fabAddList.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.navigation_my_lists) {
                layoutExploreFeed.setVisibility(View.GONE);
                layoutMyLists.setVisibility(View.VISIBLE);
                fabAddList.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return false;
            }
            return false;
        });
    }

    private void setupExploreFeed() {
        tmdbMovieAdapter = new TmdbMovieAdapter(this::openMovieDetail, this::showAddMovieToListDialog);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewExploreMovies.setLayoutManager(layoutManager);
        recyclerViewExploreMovies.setAdapter(tmdbMovieAdapter);

        // Infinite Scroll Listener
        recyclerViewExploreMovies.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                    if (!isLoadingFeed && lastVisibleItem >= totalItemCount - 4 && currentFeedPage <= totalFeedPages) {
                        loadPopularMovies(currentFeedPage);
                    }
                }
            }
        });
    }

    private void setupMyLists() {
        movieListAdapter = new MovieListAdapter(this::onMovieListClicked, this::confirmDeleteList);
        recyclerViewMovieLists.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovieLists.setAdapter(movieListAdapter);
    }

    private void displayUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                name = currentUser.getEmail();
            }
            if (name != null) {
                int atIndex = name.indexOf('@');
                if (atIndex > 0) {
                    name = name.substring(0, atIndex);
                }
                textViewWelcomeUser.setText("Olá, " + name + "!");
            }
        }
    }

    private void setupListeners() {
        buttonSearchMovies.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MovieSearchActivity.class);
            startActivity(intent);
        });

        buttonPreferences.setOnClickListener(v -> showPreferencesDialog());

        if (buttonNearbyCinemas != null) {
            buttonNearbyCinemas.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CinemasActivity.class);
                startActivity(intent);
            });
        }

        fabAddList.setOnClickListener(v -> showCreateListDialog());

        if (buttonCreateFirstList != null) {
            buttonCreateFirstList.setOnClickListener(v -> showCreateListDialog());
        }
    }

    private void openMovieDetail(TmdbMovieDto movie) {
        if (movie == null) return;
        Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
        intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, movie.getId());
        intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movie.getTitle());
        intent.putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.getPosterPath());
        intent.putExtra(MovieDetailActivity.EXTRA_OVERVIEW, movie.getOverview());
        intent.putExtra(MovieDetailActivity.EXTRA_RELEASE_YEAR, movie.getReleaseYear());
        startActivity(intent);
    }

    private void handleDeviceShake() {
        // Prevent stacking dialogs while one is already shown or while fetching
        if (isShakeDialogOpen || isShakeLoading) {
            return;
        }

        // AC05: Gentle haptic feedback
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
        triggerShakeSuggestion();
    }

    /**
     * Preloads a pool of acclaimed movies into cachedRecommendations in the background.
     * Uses vote_average.desc with vote_count.gte=300 to focus on great movies across all years.
     */
    private void prefetchRecommendations() {
        if (isPrefetchingRecommendations) return;
        if (recommendationCurrentPage > recommendationTotalPages && recommendationTotalPages > 1) {
            return;
        }

        isPrefetchingRecommendations = true;
        final int currentVersion = recommendationConfigVersion;

        UserPreferences prefs = prefManager.getUserPreferences();
        String region = prefs.getDefaultRegion();
        Float minRating = prefs.getMinRating() > 0 ? prefs.getMinRating() : 7.0f;
        String minDate = prefs.getMinReleaseYear() > 0 ? (prefs.getMinReleaseYear() + "-01-01") : null;
        String providers = prefs.getFormattedWatchProviders();
        int pageToFetch = recommendationCurrentPage;

        RetrofitClient.getInstance().getTmdbApiService()
                .discoverMovies("pt-PT", "vote_average.desc", 300, minRating, minDate, region, providers, pageToFetch)
                .enqueue(new Callback<TmdbSearchResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbSearchResponse> call, @NonNull Response<TmdbSearchResponse> response) {
                        isPrefetchingRecommendations = false;
                        if (currentVersion != recommendationConfigVersion) return;
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            recommendationTotalPages = Math.min(response.body().getTotalPages(), 20);
                            recommendationCurrentPage++;

                            for (TmdbMovieDto movie : response.body().getResults()) {
                                if (!shownRecommendationMovieIds.contains(movie.getId())) {
                                    cachedRecommendations.add(movie);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbSearchResponse> call, @NonNull Throwable t) {
                        if (currentVersion == recommendationConfigVersion) {
                            isPrefetchingRecommendations = false;
                        }
                    }
                });
    }

    /**
     * Returns an unconsumed movie from the cache without repeats, and removes it from the cache.
     */
    private TmdbMovieDto getNextUnconsumedRecommendation() {
        while (!cachedRecommendations.isEmpty()) {
            int randomIndex = new Random().nextInt(cachedRecommendations.size());
            TmdbMovieDto candidate = cachedRecommendations.remove(randomIndex);
            if (!shownRecommendationMovieIds.contains(candidate.getId())) {
                shownRecommendationMovieIds.add(candidate.getId());
                return candidate;
            }
        }
        return null;
    }

    /**
     * AC06 & AC07: Shake to suggest delivers a movie from the preloaded cache with ZERO repeats,
     * refilling the cache automatically in the background.
     */
    private void triggerShakeSuggestion() {
        if (isShakeDialogOpen || isShakeLoading) {
            return;
        }

        // Fast path: pull from preloaded cache
        TmdbMovieDto cachedMovie = getNextUnconsumedRecommendation();
        if (cachedMovie != null) {
            showShakeSuggestionDialog(cachedMovie);

            // Refill cache in background if running low (< 5 items)
            if (cachedRecommendations.size() < 5 && recommendationCurrentPage <= recommendationTotalPages) {
                prefetchRecommendations();
            }
            return;
        }

        // Fallback: If cache is empty, fetch immediately
        isShakeLoading = true;
        final int currentVersion = recommendationConfigVersion;
        UserPreferences prefs = prefManager.getUserPreferences();
        String region = prefs.getDefaultRegion();
        Float minRating = prefs.getMinRating() > 0 ? prefs.getMinRating() : 7.0f;
        String minDate = prefs.getMinReleaseYear() > 0 ? (prefs.getMinReleaseYear() + "-01-01") : null;
        String providers = prefs.getFormattedWatchProviders();
        int pageToFetch = recommendationCurrentPage <= recommendationTotalPages ? recommendationCurrentPage : 1;

        RetrofitClient.getInstance().getTmdbApiService()
                .discoverMovies("pt-PT", "vote_average.desc", 300, minRating, minDate, region, providers, pageToFetch)
                .enqueue(new Callback<TmdbSearchResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbSearchResponse> call, @NonNull Response<TmdbSearchResponse> response) {
                        isShakeLoading = false;
                        if (currentVersion != recommendationConfigVersion) return;
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null && !response.body().getResults().isEmpty()) {
                            recommendationTotalPages = Math.min(response.body().getTotalPages(), 20);
                            recommendationCurrentPage++;

                            for (TmdbMovieDto movie : response.body().getResults()) {
                                if (!shownRecommendationMovieIds.contains(movie.getId())) {
                                    cachedRecommendations.add(movie);
                                }
                            }

                            TmdbMovieDto picked = getNextUnconsumedRecommendation();
                            if (picked != null) {
                                showShakeSuggestionDialog(picked);
                            } else {
                                showEmptyShakeDialog();
                            }
                        } else {
                            showEmptyShakeDialog();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbSearchResponse> call, @NonNull Throwable t) {
                        isShakeLoading = false;
                        if (currentVersion == recommendationConfigVersion) {
                            showEmptyShakeDialog();
                        }
                    }
                });
    }

    private void showEmptyShakeDialog() {
        if (isFinishing() || isDestroyed()) return;

        if (currentShakeDialog != null && currentShakeDialog.isShowing()) {
            currentShakeDialog.dismiss();
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_shake_suggest)
                .setMessage(R.string.empty_shake_filtered_movies)
                .setPositiveButton(R.string.btn_preferences, (d, which) -> showPreferencesDialog())
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> isShakeDialogOpen = true);
        dialog.setOnDismissListener(d -> {
            isShakeDialogOpen = false;
            if (currentShakeDialog == dialog) {
                currentShakeDialog = null;
            }
        });

        currentShakeDialog = dialog;
        dialog.show();
    }

    private void showShakeSuggestionDialog(TmdbMovieDto movie) {
        if (isFinishing() || isDestroyed()) return;

        if (currentShakeDialog != null && currentShakeDialog.isShowing()) {
            currentShakeDialog.dismiss();
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_shake_suggestion, null);
        ImageView imageViewPoster = dialogView.findViewById(R.id.imageViewShakePoster);
        TextView textViewTitle = dialogView.findViewById(R.id.textViewShakeTitle);
        TextView textViewYear = dialogView.findViewById(R.id.textViewShakeYear);
        TextView textViewOverview = dialogView.findViewById(R.id.textViewShakeOverview);

        textViewTitle.setText(movie.getTitle());
        if (movie.getReleaseYear() > 0) {
            textViewYear.setText(String.valueOf(movie.getReleaseYear()));
            textViewYear.setVisibility(View.VISIBLE);
        } else {
            textViewYear.setVisibility(View.GONE);
        }

        if (movie.getOverview() != null && !movie.getOverview().isEmpty()) {
            textViewOverview.setText(movie.getOverview());
        }

        Glide.with(this)
                .load(movie.getFullPosterUrl())
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_error)
                .into(imageViewPoster);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_shake_details, (d, which) -> {
                    Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
                    intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, movie.getId());
                    intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movie.getTitle());
                    intent.putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.getPosterPath());
                    intent.putExtra(MovieDetailActivity.EXTRA_OVERVIEW, movie.getOverview());
                    intent.putExtra(MovieDetailActivity.EXTRA_RELEASE_YEAR, movie.getReleaseYear());
                    startActivity(intent);
                })
                .setNeutralButton(R.string.btn_shake_again, (d, which) -> {
                    d.dismiss();
                    triggerShakeSuggestion();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> isShakeDialogOpen = true);
        dialog.setOnDismissListener(d -> {
            isShakeDialogOpen = false;
            if (currentShakeDialog == dialog) {
                currentShakeDialog = null;
            }
        });

        currentShakeDialog = dialog;
        dialog.show();
    }

    private void showPreferencesDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_preferences, null);
        EditText editTextRadius = dialogView.findViewById(R.id.editTextCinemaRadius);
        EditText editTextRegion = dialogView.findViewById(R.id.editTextDefaultRegion);
        EditText editTextMinRating = dialogView.findViewById(R.id.editTextMinRating);
        EditText editTextMinYear = dialogView.findViewById(R.id.editTextMinYear);

        CheckBox checkboxNetflix = dialogView.findViewById(R.id.checkboxNetflix);
        CheckBox checkboxHboMax = dialogView.findViewById(R.id.checkboxHboMax);
        CheckBox checkboxDisneyPlus = dialogView.findViewById(R.id.checkboxDisneyPlus);
        CheckBox checkboxPrimeVideo = dialogView.findViewById(R.id.checkboxPrimeVideo);
        CheckBox checkboxAppleTv = dialogView.findViewById(R.id.checkboxAppleTv);

        Spinner spinnerSensitivity = dialogView.findViewById(R.id.spinnerShakeSensitivity);

        String[] sensitivities = new String[]{
                getString(R.string.sensitivity_normal),
                getString(R.string.sensitivity_high),
                getString(R.string.sensitivity_low)
        };
        ArrayAdapter<String> sensitivityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sensitivities);
        spinnerSensitivity.setAdapter(sensitivityAdapter);

        UserPreferences currentPrefs = prefManager.getUserPreferences();
        editTextRadius.setText(String.valueOf(currentPrefs.getCinemaRadiusKm()));
        editTextRegion.setText(currentPrefs.getDefaultRegion());
        editTextMinRating.setText(String.format(Locale.US, "%.1f", currentPrefs.getMinRating()));
        editTextMinYear.setText(currentPrefs.getMinReleaseYear() > 0 ? String.valueOf(currentPrefs.getMinReleaseYear()) : "");

        Set<String> activeProviders = currentPrefs.getStreamingProviders();
        checkboxNetflix.setChecked(activeProviders.contains(UserPreferences.PROVIDER_NETFLIX));
        checkboxHboMax.setChecked(activeProviders.contains(UserPreferences.PROVIDER_HBO_MAX));
        checkboxDisneyPlus.setChecked(activeProviders.contains(UserPreferences.PROVIDER_DISNEY_PLUS));
        checkboxPrimeVideo.setChecked(activeProviders.contains(UserPreferences.PROVIDER_PRIME_VIDEO));
        checkboxAppleTv.setChecked(activeProviders.contains(UserPreferences.PROVIDER_APPLE_TV));

        float currentThreshold = currentPrefs.getShakeThreshold();
        if (Math.abs(currentThreshold - 2.7f) < 0.1f) {
            spinnerSensitivity.setSelection(1); // High
        } else if (Math.abs(currentThreshold - 3.8f) < 0.1f) {
            spinnerSensitivity.setSelection(2); // Low
        } else {
            spinnerSensitivity.setSelection(0); // Normal
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String radiusStr = editTextRadius.getText().toString().trim();
                    String regionStr = editTextRegion.getText().toString().trim();
                    String ratingStr = editTextMinRating.getText().toString().trim();
                    String yearStr = editTextMinYear.getText().toString().trim();

                    try {
                        if (radiusStr.isEmpty()) {
                            throw new InvalidDataException(getString(R.string.helper_cinema_radius));
                        }
                        int radius = Integer.parseInt(radiusStr);
                        float minRating = ratingStr.isEmpty() ? 0.0f : Float.parseFloat(ratingStr);
                        int minYear = yearStr.isEmpty() ? 0 : Integer.parseInt(yearStr);

                        Set<String> selectedProviders = new HashSet<>();
                        if (checkboxNetflix.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_NETFLIX);
                        if (checkboxHboMax.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_HBO_MAX);
                        if (checkboxDisneyPlus.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_DISNEY_PLUS);
                        if (checkboxPrimeVideo.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_PRIME_VIDEO);
                        if (checkboxAppleTv.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_APPLE_TV);

                        float threshold = switch (spinnerSensitivity.getSelectedItemPosition()) {
                            case 1 -> 2.7f;
                            case 2 -> 3.8f;
                            default -> 3.2f;
                        };

                        UserPreferences updatedPrefs = new UserPreferences(radius, regionStr, minRating, minYear, selectedProviders, threshold);
                        prefManager.saveUserPreferences(updatedPrefs);
                        if (shakeDetector != null) {
                            shakeDetector.setThreshold(threshold);
                        }

                        // Reset recommendation cache so new filters take effect immediately
                        recommendationConfigVersion++;
                        isPrefetchingRecommendations = false;
                        cachedRecommendations.clear();
                        shownRecommendationMovieIds.clear();
                        recommendationCurrentPage = 1;
                        recommendationTotalPages = 1;
                        prefetchRecommendations();

                        Toast.makeText(MainActivity.this, R.string.preferences_saved_success, Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Por favor verifique os valores numéricos", Toast.LENGTH_SHORT).show();
                    } catch (InvalidDataException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void loadPopularMovies(int page) {
        if (isLoadingFeed) return;

        String apiKey = BuildConfig.TMDB_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            textViewEmptyExplore.setText("TMDB API Key is missing. Please configure TMDB_API_KEY in your .env file.");
            textViewEmptyExplore.setVisibility(View.VISIBLE);
            return;
        }

        isLoadingFeed = true;
        if (page == 1) {
            progressBarExplore.setVisibility(View.VISIBLE);
            textViewEmptyExplore.setVisibility(View.GONE);
        }

        RetrofitClient.getInstance()
                .getTmdbApiService()
                .getPopularMovies(page, "pt-PT")
                .enqueue(new Callback<TmdbPagedResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbPagedResponse> call, @NonNull Response<TmdbPagedResponse> response) {
                        isLoadingFeed = false;
                        progressBarExplore.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            tmdbMovieAdapter.addMovies(results);
                            totalFeedPages = response.body().getTotalPages();
                            currentFeedPage = page + 1;

                            if (tmdbMovieAdapter.getItemCount() == 0) {
                                textViewEmptyExplore.setText("No movies found.");
                                textViewEmptyExplore.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (tmdbMovieAdapter.getItemCount() == 0) {
                                textViewEmptyExplore.setText("Failed to load movies. (HTTP " + response.code() + ")");
                                textViewEmptyExplore.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbPagedResponse> call, @NonNull Throwable t) {
                        isLoadingFeed = false;
                        progressBarExplore.setVisibility(View.GONE);
                        if (tmdbMovieAdapter.getItemCount() == 0) {
                            textViewEmptyExplore.setText(getString(R.string.error_loading_feed, t.getMessage()));
                            textViewEmptyExplore.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.error_loading_feed, t.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void attachListsListener() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) return;

        progressBarLists.setVisibility(View.VISIBLE);
        detachListsListener();

        listsRegistration = firestoreManager.attachMovieListsListener(user.getUid(), new FirestoreManager.FirestoreCallback<List<MovieList>>() {
            @Override
            public void onSuccess(List<MovieList> result) {
                userLists = result;
                progressBarLists.setVisibility(View.GONE);
                movieListAdapter.setLists(result);
                updateListsEmptyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBarLists.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                updateListsEmptyState();
            }
        });
    }

    private void detachListsListener() {
        if (listsRegistration != null) {
            listsRegistration.remove();
            listsRegistration = null;
        }
    }

    private void showCreateListDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_list, null);
        EditText editTextName = dialogView.findViewById(R.id.editTextNewListName);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_create, (dialog, which) -> {
                    String name = editTextName.getText().toString().trim();
                    createMovieList(name);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void createMovieList(String name) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) return;

        try {
            MovieList newList = new MovieList(UUID.randomUUID().toString(), name);
            firestoreManager.createMovieList(user.getUid(), newList, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(MainActivity.this, R.string.list_created_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidDataException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteList(MovieList movieList) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_delete_list)
                .setMessage(getString(R.string.dialog_message_delete_list, movieList.getName()))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    firestoreManager.deleteMovieList(user.getUid(), movieList.getListId(), new FirestoreManager.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(MainActivity.this, R.string.list_deleted_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void onMovieListClicked(MovieList movieList) {
        if (movieList != null) {
            Intent intent = new Intent(MainActivity.this, ListDetailsActivity.class);
            intent.putExtra(ListDetailsActivity.EXTRA_LIST_ID, movieList.getListId());
            intent.putExtra(ListDetailsActivity.EXTRA_LIST_NAME, movieList.getName());
            startActivity(intent);
        }
    }

    private void showAddMovieToListDialog(TmdbMovieDto movie) {
        if (userLists == null || userLists.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_add_to_list)
                    .setMessage(R.string.prompt_no_lists_create_first)
                    .setPositiveButton("Create List", (dialog, which) -> {
                        bottomNavigationView.setSelectedItemId(R.id.navigation_my_lists);
                        showCreateListDialog();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_movie_to_list, null);
        TextView textViewTitle = dialogView.findViewById(R.id.textViewDialogMovieTitle);
        Spinner spinnerTargetList = dialogView.findViewById(R.id.spinnerTargetList);
        Spinner spinnerMovieStatus = dialogView.findViewById(R.id.spinnerMovieStatus);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarAddToList);
        TextView textViewRatingLabel = dialogView.findViewById(R.id.textViewRatingScoreLabel);
        EditText editTextComment = dialogView.findViewById(R.id.editTextDialogComment);

        String movieTitleText = movie.getTitle() + (movie.getReleaseYear() > 0 ? " (" + movie.getReleaseYear() + ")" : "");
        textViewTitle.setText(movieTitleText);

        // Populate lists spinner
        List<String> listNames = new ArrayList<>();
        for (MovieList ml : userLists) {
            listNames.add(ml.getName());
        }
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listNames);
        spinnerTargetList.setAdapter(listAdapter);

        // Populate status spinner
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
                    int selectedListIndex = spinnerTargetList.getSelectedItemPosition();
                    if (selectedListIndex >= 0 && selectedListIndex < userLists.size()) {
                        MovieList targetList = userLists.get(selectedListIndex);
                        WatchStatus status = parseSelectedStatus(spinnerMovieStatus.getSelectedItemPosition());
                        float score = ratingBar.getRating();
                        String comment = editTextComment.getText().toString().trim();

                        saveMovieToTargetList(movie, targetList, status, score, comment);
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

    private void saveMovieToTargetList(TmdbMovieDto movie, MovieList targetList, WatchStatus status, float score, String comment) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) return;

        try {
            String internalId = UUID.randomUUID().toString();
            int tmdbId = movie.getId() > 0 ? movie.getId() : 1;
            SavedMovie savedMovie = new SavedMovie(internalId, tmdbId, movie.getTitle());
            savedMovie.setReleaseYear(movie.getReleaseYear());
            savedMovie.setOverview(movie.getOverview());
            savedMovie.setPosterPath(movie.getPosterPath());
            savedMovie.setStatus(status);

            if (score >= 1.0f) {
                Rating rating = new Rating(UUID.randomUUID().toString(), score, comment);
                savedMovie.setRating(rating);
            }

            firestoreManager.addMovieToList(user.getUid(), targetList.getListId(), savedMovie, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(MainActivity.this, getString(R.string.movie_added_to_list_success, targetList.getName()), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidDataException | InvalidRatingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateListsEmptyState() {
        int count = movieListAdapter.getItemCount();
        if (count == 0) {
            if (layoutEmptyLists != null) layoutEmptyLists.setVisibility(View.VISIBLE);
            if (textViewListsCount != null) textViewListsCount.setText("0 listas");
        } else {
            if (layoutEmptyLists != null) layoutEmptyLists.setVisibility(View.GONE);
            if (textViewListsCount != null) {
                textViewListsCount.setText(count == 1 ? "1 lista" : count + " listas");
            }
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
