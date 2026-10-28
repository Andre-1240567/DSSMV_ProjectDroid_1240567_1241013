package pt.isep.dssmv.projectdroid.controllers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.firebase.FirebaseAuthManager;
import pt.isep.dssmv.projectdroid.firebase.FirestoreManager;
import pt.isep.dssmv.projectdroid.model.MovieList;
import pt.isep.dssmv.projectdroid.model.UserPreferences;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton buttonBack;
    private TextView textViewProfileName;
    private TextView textViewProfileEmail;
    private TextView textViewProfileUid;
    private ImageButton buttonCopyUid;
    private TextView textViewProfileMemberSince;
    private TextView textViewProfileTotalLists;
    private ProgressBar progressBarProfileStats;
    private View cardProfilePreferences;
    private Button buttonProfileLogout;

    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

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
        textViewProfileName = findViewById(R.id.textViewProfileName);
        textViewProfileEmail = findViewById(R.id.textViewProfileEmail);
        textViewProfileUid = findViewById(R.id.textViewProfileUid);
        buttonCopyUid = findViewById(R.id.buttonCopyUid);
        textViewProfileMemberSince = findViewById(R.id.textViewProfileMemberSince);
        textViewProfileTotalLists = findViewById(R.id.textViewProfileTotalLists);
        progressBarProfileStats = findViewById(R.id.progressBarProfileStats);
        cardProfilePreferences = findViewById(R.id.cardProfilePreferences);
        buttonProfileLogout = findViewById(R.id.buttonProfileLogout);
    }

    private void displayUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        // User display name or parsed name from email
        String name = currentUser.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            String email = currentUser.getEmail();
            if (email != null && email.contains("@")) {
                String rawName = email.substring(0, email.indexOf('@')).replace('.', ' ').replace('_', ' ');
                StringBuilder sb = new StringBuilder();
                for (String word : rawName.split(" ")) {
                    if (!word.isEmpty()) {
                        sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                    }
                }
                name = sb.toString().trim();
            } else {
                name = "CineTrack User";
            }
        }
        if (textViewProfileName != null) {
            textViewProfileName.setText(name);
        }

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

        if (buttonCopyUid != null) {
            buttonCopyUid.setOnClickListener(v -> {
                FirebaseUser user = authManager.getCurrentUser();
                if (user != null) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Firebase UID", user.getUid());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(ProfileActivity.this, "UID copiado para a área de transferência", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (cardProfilePreferences != null) {
            cardProfilePreferences.setOnClickListener(v -> showPreferencesDialog());
        }

        buttonProfileLogout.setOnClickListener(v -> confirmLogout());
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
            spinnerSensitivity.setSelection(1);
        } else if (Math.abs(currentThreshold - 3.8f) < 0.1f) {
            spinnerSensitivity.setSelection(2);
        } else {
            spinnerSensitivity.setSelection(0);
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    try {
                        int radius = Integer.parseInt(editTextRadius.getText().toString().trim());
                        String region = editTextRegion.getText().toString().trim();
                        float minRating = Float.parseFloat(editTextMinRating.getText().toString().trim());
                        String yearStr = editTextMinYear.getText().toString().trim();
                        int minYear = yearStr.isEmpty() ? 0 : Integer.parseInt(yearStr);

                        Set<String> selectedProviders = new HashSet<>();
                        if (checkboxNetflix.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_NETFLIX);
                        if (checkboxHboMax.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_HBO_MAX);
                        if (checkboxDisneyPlus.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_DISNEY_PLUS);
                        if (checkboxPrimeVideo.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_PRIME_VIDEO);
                        if (checkboxAppleTv.isChecked()) selectedProviders.add(UserPreferences.PROVIDER_APPLE_TV);

                        float selectedThreshold;
                        int selectedPos = spinnerSensitivity.getSelectedItemPosition();
                        if (selectedPos == 1) {
                            selectedThreshold = 2.7f;
                        } else if (selectedPos == 2) {
                            selectedThreshold = 3.8f;
                        } else {
                            selectedThreshold = 3.2f;
                        }

                        UserPreferences newPrefs = new UserPreferences(radius, region, minRating, minYear, selectedProviders, selectedThreshold);
                        prefManager.saveUserPreferences(newPrefs);
                        Toast.makeText(ProfileActivity.this, R.string.preferences_saved_success, Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(ProfileActivity.this, "Por favor verifique os valores numéricos", Toast.LENGTH_SHORT).show();
                    } catch (InvalidDataException e) {
                        Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
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
