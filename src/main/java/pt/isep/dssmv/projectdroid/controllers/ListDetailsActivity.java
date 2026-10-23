package pt.isep.dssmv.projectdroid.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.SavedMovieAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.Rating;
import pt.isep.dssmv.projectdroid.model.SavedMovie;
import pt.isep.dssmv.projectdroid.model.WatchStatus;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ListDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_LIST_ID = "extra_list_id";
    public static final String EXTRA_LIST_NAME = "extra_list_name";

    private String listId;
    private String listName;

    private TextView textViewListTitle;
    private TextView textViewMovieCount;
    private ImageButton buttonBack;
    private ProgressBar progressBarMovies;
    private TextView textViewEmptyMovies;
    private RecyclerView recyclerViewMovies;
    private FloatingActionButton fabAddMovie;

    private SavedMovieAdapter adapter;
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_details);

        listId = getIntent().getStringExtra(EXTRA_LIST_ID);
        listName = getIntent().getStringExtra(EXTRA_LIST_NAME);

        if (listId == null || listId.trim().isEmpty()) {
            Toast.makeText(this, "List ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMovies();
    }

    private void initViews() {
        textViewListTitle = findViewById(R.id.textViewListTitle);
        textViewMovieCount = findViewById(R.id.textViewMovieCount);
        buttonBack = findViewById(R.id.buttonBack);
        progressBarMovies = findViewById(R.id.progressBarMovies);
        textViewEmptyMovies = findViewById(R.id.textViewEmptyMovies);
        recyclerViewMovies = findViewById(R.id.recyclerViewMovies);
        fabAddMovie = findViewById(R.id.fabAddMovie);

        textViewListTitle.setText(listName != null ? listName : getString(R.string.title_list_details));
    }

    private void setupRecyclerView() {
        adapter = new SavedMovieAdapter(
                this::onMovieClicked,
                this::confirmDeleteMovie,
                this::showRateMovieDialog
        );
        recyclerViewMovies.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovies.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        fabAddMovie.setOnClickListener(v -> showAddMovieMethodChooser());
    }

    private void showAddMovieMethodChooser() {
        String[] options = new String[]{
                getString(R.string.option_search_catalog),
                getString(R.string.option_manual_entry)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_choose_add_method)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(ListDetailsActivity.this, MovieSearchActivity.class);
                        startActivity(intent);
                    } else {
                        showManualAddMovieDialog();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void loadMovies() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return;
        }

        progressBarMovies.setVisibility(View.VISIBLE);
        firestoreManager.getMoviesInList(user.getUid(), listId, new FirestoreManager.FirestoreCallback<List<SavedMovie>>() {
            @Override
            public void onSuccess(List<SavedMovie> result) {
                progressBarMovies.setVisibility(View.GONE);
                adapter.setMovies(result);
                updateMovieCount();
                updateEmptyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBarMovies.setVisibility(View.GONE);
                Toast.makeText(ListDetailsActivity.this, getString(R.string.error_loading_movies, errorMessage), Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void showManualAddMovieDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_movie, null);
        EditText editTextTitle = dialogView.findViewById(R.id.editTextAddMovieTitle);
        EditText editTextYear = dialogView.findViewById(R.id.editTextAddMovieYear);
        EditText editTextOverview = dialogView.findViewById(R.id.editTextAddMovieOverview);
        Spinner spinnerStatus = dialogView.findViewById(R.id.spinnerAddMovieStatus);

        String[] statuses = new String[]{
                getString(R.string.status_plan_to_watch),
                getString(R.string.status_watching),
                getString(R.string.status_watched),
                getString(R.string.status_favorite)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerStatus.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_add, (dialog, which) -> {
                    String title = editTextTitle.getText().toString().trim();
                    String yearStr = editTextYear.getText().toString().trim();
                    String overview = editTextOverview.getText().toString().trim();
                    WatchStatus status = parseSelectedStatus(spinnerStatus.getSelectedItemPosition());

                    int year = 0;
                    if (!yearStr.isEmpty()) {
                        try {
                            year = Integer.parseInt(yearStr);
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    saveMovie(title, year, overview, status);
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

    private void saveMovie(String title, int year, String overview, WatchStatus status) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            String internalId = UUID.randomUUID().toString();
            int tmdbId = Math.abs(internalId.hashCode());
            if (tmdbId <= 0) tmdbId = 1;

            SavedMovie movie = new SavedMovie(internalId, tmdbId, title);
            movie.setReleaseYear(year);
            movie.setOverview(overview);
            movie.setStatus(status);

            progressBarMovies.setVisibility(View.VISIBLE);
            firestoreManager.addMovieToList(user.getUid(), listId, movie, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    progressBarMovies.setVisibility(View.GONE);
                    adapter.addMovie(movie);
                    updateMovieCount();
                    updateEmptyState();
                    Toast.makeText(ListDetailsActivity.this, R.string.movie_added_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressBarMovies.setVisibility(View.GONE);
                    Toast.makeText(ListDetailsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidDataException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showRateMovieDialog(SavedMovie movie) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rate_movie, null);
        TextView textViewTitle = dialogView.findViewById(R.id.textViewRateMovieTitle);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarMovie);
        TextView textViewScoreDisplay = dialogView.findViewById(R.id.textViewRatingScoreDisplay);
        EditText editTextComment = dialogView.findViewById(R.id.editTextRatingComment);

        textViewTitle.setText(movie.getTitle());

        float initialScore = 3.0f;
        if (movie.getRating() != null && movie.getRating().getScore() > 0) {
            initialScore = movie.getRating().getScore();
            if (movie.getRating().getComment() != null) {
                editTextComment.setText(movie.getRating().getComment());
            }
        }

        ratingBar.setRating(initialScore);
        textViewScoreDisplay.setText(String.format(Locale.getDefault(), "%.1f / 5.0", initialScore));

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating < 1.0f) {
                bar.setRating(1.0f);
                rating = 1.0f;
            }
            textViewScoreDisplay.setText(String.format(Locale.getDefault(), "%.1f / 5.0", rating));
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_submit_rating, (dialog, which) -> {
                    float score = ratingBar.getRating();
                    String comment = editTextComment.getText().toString().trim();
                    saveRating(movie, score, comment);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void saveRating(SavedMovie movie, float score, String comment) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            String ratingId = (movie.getRating() != null) ? movie.getRating().getRatingId() : UUID.randomUUID().toString();
            Rating rating = new Rating(ratingId, score, comment);

            progressBarMovies.setVisibility(View.VISIBLE);
            firestoreManager.rateMovieInList(user.getUid(), listId, movie.getInternalId(), rating, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    progressBarMovies.setVisibility(View.GONE);
                    movie.setRating(rating);
                    adapter.updateMovie(movie);
                    Toast.makeText(ListDetailsActivity.this, R.string.rating_submitted_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressBarMovies.setVisibility(View.GONE);
                    Toast.makeText(ListDetailsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidRatingException | InvalidDataException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteMovie(SavedMovie movie) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_delete_movie)
                .setMessage(getString(R.string.dialog_message_delete_movie, movie.getTitle()))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    progressBarMovies.setVisibility(View.VISIBLE);
                    firestoreManager.deleteMovieFromList(user.getUid(), listId, movie.getInternalId(), new FirestoreManager.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            progressBarMovies.setVisibility(View.GONE);
                            adapter.removeMovie(movie.getInternalId());
                            updateMovieCount();
                            updateEmptyState();
                            Toast.makeText(ListDetailsActivity.this, R.string.movie_deleted_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            progressBarMovies.setVisibility(View.GONE);
                            Toast.makeText(ListDetailsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void onMovieClicked(SavedMovie movie) {
        if (movie != null && movie.getTmdbId() > 0) {
            Intent intent = new Intent(ListDetailsActivity.this, MovieDetailActivity.class);
            intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, movie.getTmdbId());
            intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movie.getTitle());
            intent.putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.getPosterPath());
            intent.putExtra(MovieDetailActivity.EXTRA_OVERVIEW, movie.getOverview());
            intent.putExtra(MovieDetailActivity.EXTRA_RELEASE_YEAR, movie.getReleaseYear());
            startActivity(intent);
        } else if (movie != null) {
            Toast.makeText(this, movie.getTitle() + " (" + movie.getStatus() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMovieCount() {
        int count = adapter.getItemCount();
        textViewMovieCount.setText(count == 1 ? "1 movie" : count + " movies");
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            textViewEmptyMovies.setVisibility(View.VISIBLE);
        } else {
            textViewEmptyMovies.setVisibility(View.GONE);
        }
    }
}
