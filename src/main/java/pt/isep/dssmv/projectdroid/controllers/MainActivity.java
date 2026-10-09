package pt.isep.dssmv.projectdroid.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.MovieListAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.MovieList;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView textViewWelcomeUser;
    private Button buttonLogout;
    private RecyclerView recyclerViewMovieLists;
    private ProgressBar progressBarLists;
    private TextView textViewEmptyLists;
    private FloatingActionButton fabAddList;

    private MovieListAdapter adapter;
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
        setupRecyclerView();
        displayUserData();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager.isUserLoggedIn()) {
            loadMovieLists();
        }
    }

    private void initViews() {
        textViewWelcomeUser = findViewById(R.id.textViewWelcomeUser);
        buttonLogout = findViewById(R.id.buttonLogout);
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
        buttonLogout.setOnClickListener(v -> {
            authManager.logout();
            redirectToLogin();
        });

        fabAddList.setOnClickListener(v -> showCreateListDialog());
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
