package pt.isep.dssmv.projectdroid.controllers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.MovieSearchAdapter;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.dto.TmdbMovieDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbSearchResponse;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieSearchActivity extends AppCompatActivity {

    private Button buttonBack;
    private EditText editTextSearchQuery;
    private Button buttonPerformSearch;
    private ProgressBar progressBarSearch;
    private TextView textViewSearchStatus;
    private RecyclerView recyclerViewSearchResults;

    private MovieSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_search);

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        buttonBack = findViewById(R.id.buttonBack);
        editTextSearchQuery = findViewById(R.id.editTextSearchQuery);
        buttonPerformSearch = findViewById(R.id.buttonPerformSearch);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        textViewSearchStatus = findViewById(R.id.textViewSearchStatus);
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
    }

    private void setupRecyclerView() {
        adapter = new MovieSearchAdapter(this::onMovieSelected);
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSearchResults.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonPerformSearch.setOnClickListener(v -> performSearch());

        editTextSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = editTextSearchQuery.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_search, Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();

        progressBarSearch.setVisibility(View.VISIBLE);
        textViewSearchStatus.setVisibility(View.GONE);
        recyclerViewSearchResults.setVisibility(View.GONE);

        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String region = prefManager.getDefaultRegion();
        String language = "PT".equalsIgnoreCase(region) ? "pt-PT" : (region.toLowerCase() + "-" + region.toUpperCase());

        // AC02: Non-blocking asynchronous network call via Retrofit .enqueue()
        RetrofitClient.getInstance().getTmdbApiService()
                .searchMovies(query, language, 1)
                .enqueue(new Callback<TmdbSearchResponse>() {
                    @Override
                    public void onResponse(Call<TmdbSearchResponse> call, Response<TmdbSearchResponse> response) {
                        progressBarSearch.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            TmdbSearchResponse searchResponse = response.body();
                            if (searchResponse.getResults().isEmpty()) {
                                textViewSearchStatus.setText(getString(R.string.status_no_results, query));
                                textViewSearchStatus.setVisibility(View.VISIBLE);
                                recyclerViewSearchResults.setVisibility(View.GONE);
                            } else {
                                adapter.setMovies(searchResponse.getResults());
                                recyclerViewSearchResults.setVisibility(View.VISIBLE);
                                textViewSearchStatus.setVisibility(View.GONE);
                            }
                        } else {
                            String errorMsg = "Error: " + response.code();
                            textViewSearchStatus.setText(getString(R.string.error_search_failed, errorMsg));
                            textViewSearchStatus.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbSearchResponse> call, Throwable t) {
                        progressBarSearch.setVisibility(View.GONE);
                        String errorMsg = (t.getMessage() != null) ? t.getMessage() : "Network error";
                        textViewSearchStatus.setText(getString(R.string.error_search_failed, errorMsg));
                        textViewSearchStatus.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void onMovieSelected(TmdbMovieDto movie) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra(MovieDetailActivity.EXTRA_TMDB_ID, movie.getId());
        intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movie.getTitle());
        intent.putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.getPosterPath());
        intent.putExtra(MovieDetailActivity.EXTRA_OVERVIEW, movie.getOverview());
        intent.putExtra(MovieDetailActivity.EXTRA_RELEASE_YEAR, movie.getReleaseYear());
        startActivity(intent);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
