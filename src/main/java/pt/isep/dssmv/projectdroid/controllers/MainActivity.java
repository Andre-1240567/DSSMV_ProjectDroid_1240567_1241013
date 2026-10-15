package pt.isep.dssmv.projectdroid.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import pt.isep.dssmv.projectdroid.model.WatchStatus;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.TmdbMovieDto;
import pt.isep.dssmv.projectdroid.network.TmdbPagedResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Common views
    private Button buttonLogout;
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
    private RecyclerView recyclerViewMovieLists;
    private ProgressBar progressBarLists;
    private TextView textViewEmptyLists;
    private FloatingActionButton fabAddList;
    private MovieListAdapter movieListAdapter;
    private List<MovieList> userLists = new ArrayList<>();
    private ListenerRegistration listsRegistration;

    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();

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

        // Load initial feed of popular movies
        loadPopularMovies(1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authManager.isUserLoggedIn()) {
            attachListsListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListsListener();
    }

    private void initViews() {
        buttonLogout = findViewById(R.id.buttonLogout);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Tab 1
        layoutExploreFeed = findViewById(R.id.layoutExploreFeed);
        recyclerViewExploreMovies = findViewById(R.id.recyclerViewExploreMovies);
        progressBarExplore = findViewById(R.id.progressBarExplore);
        textViewEmptyExplore = findViewById(R.id.textViewEmptyExplore);

        // Tab 2
        layoutMyLists = findViewById(R.id.layoutMyLists);
        textViewWelcomeUser = findViewById(R.id.textViewWelcomeUser);
        recyclerViewMovieLists = findViewById(R.id.recyclerViewMovieLists);
        progressBarLists = findViewById(R.id.progressBarLists);
        textViewEmptyLists = findViewById(R.id.textViewEmptyLists);
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
            }
            return false;
        });
    }

    private void setupExploreFeed() {
        tmdbMovieAdapter = new TmdbMovieAdapter(this::showAddMovieToListDialog);
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
        if (currentUser != null && currentUser.getEmail() != null) {
            String welcomeText = getString(R.string.welcome_user, currentUser.getEmail());
            textViewWelcomeUser.setText(welcomeText);
        }
    }

    private void setupListeners() {
        buttonLogout.setOnClickListener(v -> {
            authManager.logout();
            redirectToLogin();
        });

        fabAddList.setOnClickListener(v -> showCreateListDialog());
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
                .getPopularMovies(apiKey, page, "pt-PT")
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
        if (movieListAdapter.getItemCount() == 0) {
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
