package pt.isep.dssmv.projectdroid.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {

    private static FirebaseAuthManager instance;
    private FirebaseAuth auth;

    public interface AuthCallback {
        void onSuccess(String userId, String email);
        void onFailure(String errorMessage);
    }

    private FirebaseAuthManager() {
        try {
            this.auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            this.auth = null;
        }
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        return auth != null && auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return (auth != null) ? auth.getCurrentUser() : null;
    }

    public void login(String email, String password, AuthCallback callback) {
        if (auth == null) {
            callback.onFailure("Firebase Authentication is not available. Please verify Firebase configuration.");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        FirebaseUser user = auth.getCurrentUser();
                        callback.onSuccess(user.getUid(), user.getEmail());
                    } else {
                        String error = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Authentication failed";
                        callback.onFailure(error);
                    }
                });
    }

    public void register(String email, String password, AuthCallback callback) {
        if (auth == null) {
            callback.onFailure("Firebase Authentication is not available. Please verify Firebase configuration.");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        FirebaseUser user = auth.getCurrentUser();
                        callback.onSuccess(user.getUid(), user.getEmail());
                    } else {
                        String error = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Registration failed";
                        callback.onFailure(error);
                    }
                });
    }

    public void logout() {
        if (auth != null) {
            auth.signOut();
        }
    }
}
