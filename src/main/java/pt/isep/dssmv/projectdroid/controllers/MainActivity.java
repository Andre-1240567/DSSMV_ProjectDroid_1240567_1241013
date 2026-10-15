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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.MovieListAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.MovieList;
import pt.isep.dssmv.projectdroid.model.UserPreferences;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.dto.TmdbMovieDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbSearchResponse;
import pt.isep.dssmv.projectdroid.utils.ShakeDetector;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView textViewWelcomeUser;
    private Button buttonSearchMovies;
    private Button buttonPreferences;
    private Button buttonLogout;
    private Button buttonNearbyCinemas;
    private Button buttonShakeSuggest;
    private RecyclerView recyclerViewMovieLists;
    private ProgressBar progressBarLists;
    private TextView textViewEmptyLists;
    private FloatingActionButton fabAddList;

    private MovieListAdapter adapter;
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private SharedPrefManager prefManager;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private Vibrator vibrator;

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
        setupRecyclerView();
        displayUserData();
        setupListeners();
        setupSensors();
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        shakeDetector = new ShakeDetector(this::handleDeviceShake);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager.isUserLoggedIn()) {
            loadMovieLists();
        }
        // AC04: Strictly register accelerometer in onResume()
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // AC04: Strictly unregister accelerometer in onPause() to preserve device battery
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    private void initViews() {
        textViewWelcomeUser = findViewById(R.id.textViewWelcomeUser);
        buttonSearchMovies = findViewById(R.id.buttonSearchMovies);
        buttonPreferences = findViewById(R.id.buttonPreferences);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonNearbyCinemas = findViewById(R.id.buttonNearbyCinemas);
        buttonShakeSuggest = findViewById(R.id.buttonShakeSuggest);
        recyclerViewMovieLists = findViewById(R.id.recyclerViewMovieLists);
        progressBarLists = findViewById(R.id.progressBarLists);
        textViewEmptyLists = findViewById(R.id.textViewEmptyLists);
        fabAddList = findViewById(R.id.fabAddList);
    }

    private void setupRecyclerView() {
        adapter = new MovieListAdapter(this::onMovieListClicked, this::confirmDeleteList);
        recyclerViewMovieLists.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovieLists.setAdapter(adapter);
    }

    private void displayUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            String welcomeText = getString(R.string.welcome_user, currentUser.getEmail());
            textViewWelcomeUser.setText(welcomeText);
        }
    }

    private void setupListeners() {
        buttonSearchMovies.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MovieSearchActivity.class);
            startActivity(intent);
        });

        buttonNearbyCinemas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CinemasActivity.class);
            startActivity(intent);
        });

        buttonShakeSuggest.setOnClickListener(v -> triggerShakeSuggestion());

        buttonPreferences.setOnClickListener(v -> showPreferencesDialog());

        buttonLogout.setOnClickListener(v -> {
            authManager.logout();
            redirectToLogin();
        });

        fabAddList.setOnClickListener(v -> showCreateListDialog());
    }

    private void handleDeviceShake() {
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
     * AC06 & AC07: Shake to suggest randomly picks a movie and presents it in a modal dialog.
     */
    private void triggerShakeSuggestion() {
        RetrofitClient.getInstance().getTmdbApiService()
                .searchMovies("cinema", "en-US", 1)
                .enqueue(new Callback<TmdbSearchResponse>() {
                    @Override
                    public void onResponse(Call<TmdbSearchResponse> call, Response<TmdbSearchResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().getResults().isEmpty()) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            int randomIndex = new Random().nextInt(results.size());
                            TmdbMovieDto picked = results.get(randomIndex);
                            showShakeSuggestionDialog(picked);
                        } else {
                            showEmptyShakeDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbSearchResponse> call, Throwable t) {
                        showEmptyShakeDialog();
                    }
                });
    }

    private void showEmptyShakeDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_shake_suggest)
                .setMessage(R.string.empty_shake_movies)
                .setPositiveButton(R.string.btn_search_movies, (dialog, which) -> {
                    startActivity(new Intent(MainActivity.this, MovieSearchActivity.class));
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showShakeSuggestionDialog(TmdbMovieDto movie) {
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

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_shake_details, (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
                    intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, movie.getId());
                    intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movie.getTitle());
                    intent.putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.getPosterPath());
                    intent.putExtra(MovieDetailActivity.EXTRA_OVERVIEW, movie.getOverview());
                    intent.putExtra(MovieDetailActivity.EXTRA_RELEASE_YEAR, movie.getReleaseYear());
                    startActivity(intent);
                })
                .setNeutralButton(R.string.btn_shake_again, (dialog, which) -> triggerShakeSuggestion())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showPreferencesDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_preferences, null);
        EditText editTextRadius = dialogView.findViewById(R.id.editTextCinemaRadius);
        EditText editTextRegion = dialogView.findViewById(R.id.editTextDefaultRegion);

        UserPreferences currentPrefs = prefManager.getUserPreferences();
        editTextRadius.setText(String.valueOf(currentPrefs.getCinemaRadiusKm()));
        editTextRegion.setText(currentPrefs.getDefaultRegion());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String radiusStr = editTextRadius.getText().toString().trim();
                    String regionStr = editTextRegion.getText().toString().trim();

                    try {
                        if (radiusStr.isEmpty()) {
                            throw new InvalidDataException(getString(R.string.helper_cinema_radius));
                        }
                        int radius = Integer.parseInt(radiusStr);
                        UserPreferences updatedPrefs = new UserPreferences(radius, regionStr);
                        prefManager.saveUserPreferences(updatedPrefs);
                        Toast.makeText(MainActivity.this, R.string.preferences_saved_success, Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, R.string.helper_cinema_radius, Toast.LENGTH_SHORT).show();
                    } catch (InvalidDataException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void loadMovieLists() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return;
        }

        progressBarLists.setVisibility(View.VISIBLE);
        firestoreManager.getUserMovieLists(user.getUid(), new FirestoreManager.FirestoreCallback<List<MovieList>>() {
            @Override
            public void onSuccess(List<MovieList> result) {
                progressBarLists.setVisibility(View.GONE);
                adapter.setLists(result);
                updateEmptyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBarLists.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, getString(R.string.error_loading_lists, errorMessage), Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
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
        if (user == null) {
            return;
        }

        try {
            MovieList newList = new MovieList(UUID.randomUUID().toString(), name);
            progressBarLists.setVisibility(View.VISIBLE);

            firestoreManager.createMovieList(user.getUid(), newList, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    progressBarLists.setVisibility(View.GONE);
                    adapter.addList(newList);
                    updateEmptyState();
                    Toast.makeText(MainActivity.this, R.string.list_created_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressBarLists.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidDataException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteList(MovieList movieList) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_delete_list)
                .setMessage(getString(R.string.dialog_message_delete_list, movieList.getName()))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    progressBarLists.setVisibility(View.VISIBLE);
                    firestoreManager.deleteMovieList(user.getUid(), movieList.getListId(), new FirestoreManager.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            progressBarLists.setVisibility(View.GONE);
                            adapter.removeList(movieList.getListId());
                            updateEmptyState();
                            Toast.makeText(MainActivity.this, R.string.list_deleted_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            progressBarLists.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void onMovieListClicked(MovieList movieList) {
        Toast.makeText(this, movieList.getName(), Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            textViewEmptyLists.setVisibility(View.VISIBLE);
        } else {
            textViewEmptyLists.setVisibility(View.GONE);
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
