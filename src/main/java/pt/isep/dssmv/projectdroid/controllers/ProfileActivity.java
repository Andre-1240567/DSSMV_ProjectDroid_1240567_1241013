package pt.isep.dssmv.projectdroid.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.MovieList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton buttonBack;
    private TextView textViewProfileEmail;
    private TextView textViewProfileUid;
    private TextView textViewProfileMemberSince;
    private TextView textViewProfileTotalLists;
    private ProgressBar progressBarProfileStats;
    private Button buttonProfileLogout;

    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();

        if (!authManager.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        initViews();
        displayUserData();
        loadUserStatistics();
        setupListeners();
    }

    private void initViews() {
        buttonBack = findViewById(R.id.buttonBack);
        textViewProfileEmail = findViewById(R.id.textViewProfileEmail);
        textViewProfileUid = findViewById(R.id.textViewProfileUid);
        textViewProfileMemberSince = findViewById(R.id.textViewProfileMemberSince);
        textViewProfileTotalLists = findViewById(R.id.textViewProfileTotalLists);
        progressBarProfileStats = findViewById(R.id.progressBarProfileStats);
        buttonProfileLogout = findViewById(R.id.buttonProfileLogout);
    }

    private void displayUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        textViewProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");
        textViewProfileUid.setText(currentUser.getUid());

        FirebaseUserMetadata metadata = currentUser.getMetadata();
        if (metadata != null && metadata.getCreationTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textViewProfileMemberSince.setText(sdf.format(new Date(metadata.getCreationTimestamp())));
        } else {
            textViewProfileMemberSince.setText("N/A");
        }
    }

    private void loadUserStatistics() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        progressBarProfileStats.setVisibility(View.VISIBLE);
        firestoreManager.getUserMovieLists(currentUser.getUid(), new FirestoreManager.FirestoreCallback<List<MovieList>>() {
            @Override
            public void onSuccess(List<MovieList> result) {
                progressBarProfileStats.setVisibility(View.GONE);
                int count = (result != null) ? result.size() : 0;
                textViewProfileTotalLists.setText(count == 1 ? "1 list" : count + " lists");
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBarProfileStats.setVisibility(View.GONE);
                textViewProfileTotalLists.setText("0 lists");
            }
        });
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonProfileLogout.setOnClickListener(v -> confirmLogout());
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_logout)
                .setMessage(R.string.dialog_message_logout)
                .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                    authManager.logout();
                    redirectToLogin();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
