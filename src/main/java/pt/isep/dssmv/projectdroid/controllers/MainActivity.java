package pt.isep.dssmv.projectdroid.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;

public class MainActivity extends AppCompatActivity {

    private TextView textViewWelcomeUser;
    private Button buttonLogout;

    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = FirebaseAuthManager.getInstance();

        if (!authManager.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        initViews();
        displayUserData();
        setupListeners();
    }

    private void initViews() {
        textViewWelcomeUser = findViewById(R.id.textViewWelcomeUser);
        buttonLogout = findViewById(R.id.buttonLogout);
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
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
