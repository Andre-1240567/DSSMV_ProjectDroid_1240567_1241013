package pt.isep.dssmv.projectdroid;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;

public class CineTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enforce consistent high-contrast Light palette across all Android versions
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
