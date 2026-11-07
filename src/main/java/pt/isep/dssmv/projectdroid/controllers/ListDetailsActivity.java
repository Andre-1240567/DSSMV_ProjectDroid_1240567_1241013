package pt.isep.dssmv.projectdroid.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.SavedMovieAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.Rating;
import pt.isep.dssmv.projectdroid.model.SavedMovie;
import pt.isep.dssmv.projectdroid.model.SavedMovieFilterSorter;
import pt.isep.dssmv.projectdroid.model.SavedMovieSortOption;
import pt.isep.dssmv.projectdroid.model.WatchStatus;
import pt.isep.dssmv.projectdroid.network.TmdbRatingManager;
import pt.isep.dssmv.projectdroid.utils.ShareHelper;

import java.util.ArrayList;
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
    private ImageButton buttonShareList;
    private ProgressBar progressBarMovies;
    private TextView textViewEmptyMovies;
    private RecyclerView recyclerViewMovies;
    private FloatingActionButton fabAddMovie;

    // US9: Local Filtering and Sorting state
    private final List<SavedMovie> allMovies = new ArrayList<>();
    private WatchStatus currentStatusFilter = null;
    private String currentSearchQuery = "";
    private SavedMovieSortOption currentSortOption = SavedMovieSortOption.TITLE_ASC;

    private EditText editTextSearchList;
    private MaterialButton buttonSortMovies;
    private ChipGroup chipGroupStatus;
    private Chip chipFilterAll;
    private Chip chipFilterPlanToWatch;
    private Chip chipFilterWatching;
    private Chip chipFilterWatched;
    private Chip chipFilterFavorite;

    private SavedMovieAdapter adapter;
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private ListenerRegistration moviesListenerRegistration;

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
    protected void onStart() {
        super.onStart();
        attachMoviesListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachMoviesListener();
    }

    private void initViews() {
        textViewListTitle = findViewById(R.id.textViewListTitle);
        textViewMovieCount = findViewById(R.id.textViewMovieCount);
        buttonBack = findViewById(R.id.buttonBack);
        buttonShareList = findViewById(R.id.buttonShareList);
        progressBarMovies = findViewById(R.id.progressBarMovies);
        textViewEmptyMovies = findViewById(R.id.textViewEmptyMovies);
        recyclerViewMovies = findViewById(R.id.recyclerViewMovies);
        fabAddMovie = findViewById(R.id.fabAddMovie);

        editTextSearchList = findViewById(R.id.editTextSearchList);
        buttonSortMovies = findViewById(R.id.buttonSortMovies);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterPlanToWatch = findViewById(R.id.chipFilterPlanToWatch);
        chipFilterWatching = findViewById(R.id.chipFilterWatching);
        chipFilterWatched = findViewById(R.id.chipFilterWatched);
        chipFilterFavorite = findViewById(R.id.chipFilterFavorite);

        textViewListTitle.setText(listName != null ? listName : getString(R.string.title_list_details));
    }

    private void setupRecyclerView() {
        adapter = new SavedMovieAdapter(
                this::onMovieClicked,
                this::confirmDeleteMovie,
                this::showRateMovieDialog,
                this::onMovieShare
        );
        recyclerViewMovies.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovies.setAdapter(adapter);
    }

    private void onMovieShare(SavedMovie movie) {
        ShareHelper.shareSavedMovie(this, movie);
    }

    private void handleShareList() {
        if (allMovies.isEmpty()) {
            Toast.makeText(this, R.string.warning_share_list_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ShareHelper.shareMovieList(this, listName, null, allMovies);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonShareList.setOnClickListener(v -> handleShareList());
        fabAddMovie.setOnClickListener(v -> showAddMovieMethodChooser());
        buttonSortMovies.setOnClickListener(v -> showSortDialog());

        editTextSearchList.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = (s != null) ? s.toString().trim() : "";
                applyFilterAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.contains(R.id.chipFilterAll)) {
                currentStatusFilter = null;
            } else if (checkedIds.contains(R.id.chipFilterPlanToWatch)) {
                currentStatusFilter = WatchStatus.PLAN_TO_WATCH;
            } else if (checkedIds.contains(R.id.chipFilterWatching)) {
                currentStatusFilter = WatchStatus.WATCHING;
            } else if (checkedIds.contains(R.id.chipFilterWatched)) {
                currentStatusFilter = WatchStatus.WATCHED;
            } else if (checkedIds.contains(R.id.chipFilterFavorite)) {
                currentStatusFilter = WatchStatus.FAVORITE;
            } else {
                currentStatusFilter = null;
            }
            applyFilterAndSort();
        });
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

    private void attachMoviesListener() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || listId == null) {
            return;
        }

        progressBarMovies.setVisibility(View.VISIBLE);
        detachMoviesListener();

        moviesListenerRegistration = firestoreManager.attachMoviesInListListener(user.getUid(), listId, new FirestoreManager.FirestoreCallback<List<SavedMovie>>() {
            @Override
            public void onSuccess(List<SavedMovie> result) {
                progressBarMovies.setVisibility(View.GONE);
                allMovies.clear();
                if (result != null) {
                    allMovies.addAll(result);
                }
                applyFilterAndSort();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBarMovies.setVisibility(View.GONE);
                Toast.makeText(ListDetailsActivity.this, getString(R.string.error_loading_movies, errorMessage), Toast.LENGTH_SHORT).show();
                updateEmptyState(0, allMovies.size());
            }
        });
    }

    private void detachMoviesListener() {
        if (moviesListenerRegistration != null) {
            moviesListenerRegistration.remove();
            moviesListenerRegistration = null;
        }
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

            firestoreManager.addMovieToList(user.getUid(), listId, movie, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(ListDetailsActivity.this, R.string.movie_added_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
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
        TextView textViewYear = dialogView.findViewById(R.id.textViewRateMovieYear);
        ImageView imageViewPoster = dialogView.findViewById(R.id.imageViewRateMoviePoster);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarMovie);
        TextView textViewScoreDisplay = dialogView.findViewById(R.id.textViewRatingScoreDisplay);
        EditText editTextComment = dialogView.findViewById(R.id.editTextRatingComment);

        textViewTitle.setText(movie.getTitle());

        if (textViewYear != null && movie.getReleaseYear() > 0) {
            textViewYear.setText(String.valueOf(movie.getReleaseYear()));
            textViewYear.setVisibility(View.VISIBLE);
        } else if (textViewYear != null) {
            textViewYear.setVisibility(View.GONE);
        }

        if (imageViewPoster != null) {
            String posterPath = movie.getPosterPath();
            if (posterPath != null && !posterPath.trim().isEmpty()) {
                String fullUrl = posterPath.startsWith("http")
                        ? posterPath
                        : "https://image.tmdb.org/t/p/w500" + posterPath;

                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .error(R.drawable.ic_movie_error)
                        .into(imageViewPoster);
            } else {
                imageViewPoster.setImageResource(R.drawable.ic_movie_placeholder);
            }
        }

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

            firestoreManager.rateMovieInList(user.getUid(), listId, movie.getInternalId(), rating, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(ListDetailsActivity.this, R.string.rating_submitted_success, Toast.LENGTH_SHORT).show();

                    // HTTP POST rating to TMDB REST API if movie is linked to TMDB catalog
                    if (movie.getTmdbId() > 0) {
                        TmdbRatingManager.getInstance(ListDetailsActivity.this)
                                .submitRating(movie.getTmdbId(), score, null);
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
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
                    firestoreManager.deleteMovieFromList(user.getUid(), listId, movie.getInternalId(), new FirestoreManager.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(ListDetailsActivity.this, R.string.movie_deleted_success, Toast.LENGTH_SHORT).show();

                            // HTTP DELETE rating from TMDB REST API if movie was rated
                            if (movie.getTmdbId() > 0 && movie.getRating() != null) {
                                TmdbRatingManager.getInstance(ListDetailsActivity.this)
                                        .deleteRating(movie.getTmdbId(), null);
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
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

    private void showSortDialog() {
        String[] options = new String[]{
                getString(R.string.sort_title_asc),
                getString(R.string.sort_title_desc),
                getString(R.string.sort_rating_desc),
                getString(R.string.sort_rating_asc),
                getString(R.string.sort_year_desc),
                getString(R.string.sort_year_asc)
        };

        int checkedIndex;
        switch (currentSortOption) {
            case TITLE_DESC:
                checkedIndex = 1;
                break;
            case RATING_DESC:
                checkedIndex = 2;
                break;
            case RATING_ASC:
                checkedIndex = 3;
                break;
            case YEAR_DESC:
                checkedIndex = 4;
                break;
            case YEAR_ASC:
                checkedIndex = 5;
                break;
            case TITLE_ASC:
            default:
                checkedIndex = 0;
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.sort_dialog_title)
                .setSingleChoiceItems(options, checkedIndex, (dialog, which) -> {
                    switch (which) {
                        case 1:
                            currentSortOption = SavedMovieSortOption.TITLE_DESC;
                            break;
                        case 2:
                            currentSortOption = SavedMovieSortOption.RATING_DESC;
                            break;
                        case 3:
                            currentSortOption = SavedMovieSortOption.RATING_ASC;
                            break;
                        case 4:
                            currentSortOption = SavedMovieSortOption.YEAR_DESC;
                            break;
                        case 5:
                            currentSortOption = SavedMovieSortOption.YEAR_ASC;
                            break;
                        case 0:
                        default:
                            currentSortOption = SavedMovieSortOption.TITLE_ASC;
                            break;
                    }
                    applyFilterAndSort();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void applyFilterAndSort() {
        List<SavedMovie> result = SavedMovieFilterSorter.filterAndSort(
                allMovies,
                currentStatusFilter,
                currentSearchQuery,
                currentSortOption
        );

        adapter.setMovies(result);
        updateMovieCount(result.size(), allMovies.size());
        updateEmptyState(result.size(), allMovies.size());
    }

    private void updateMovieCount(int filteredCount, int totalCount) {
        if (filteredCount == totalCount) {
            textViewMovieCount.setText(totalCount == 1 ? "1 movie" : totalCount + " movies");
        } else {
            textViewMovieCount.setText(filteredCount + " of " + totalCount + " movies");
        }
    }

    private void updateEmptyState(int filteredCount, int totalCount) {
        if (totalCount == 0) {
            textViewEmptyMovies.setText(R.string.empty_movies_list);
            textViewEmptyMovies.setVisibility(View.VISIBLE);
        } else if (filteredCount == 0) {
            textViewEmptyMovies.setText(R.string.empty_filtered_movies);
            textViewEmptyMovies.setVisibility(View.VISIBLE);
        } else {
            textViewEmptyMovies.setVisibility(View.GONE);
        }
    }
}
