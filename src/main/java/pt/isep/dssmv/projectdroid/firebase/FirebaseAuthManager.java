package pt.isep.dssmv.projectdroid.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {

    private static FirebaseAuthManager instance;
    private final FirebaseAuth auth;

    public interface AuthCallback {
        void onSuccess(String userId, String email);
        void onFailure(String errorMessage);
    }

    private FirebaseAuthManager() {
        this.auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void login(String email, String password, AuthCallback callback) {
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
        auth.signOut();
    }
}
