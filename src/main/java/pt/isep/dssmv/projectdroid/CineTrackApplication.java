package pt.isep.dssmv.projectdroid;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;

public class CineTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved user theme preference (Light, Dark, or System default)
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        prefManager.applyThemeMode(prefManager.getThemeMode());

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
