package pt.isep.dssmv.projectdroid;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class CineTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Ensure FirebaseApp is safely initialized even if google-services.json is not present yet
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("pt.isep.dssmv.projectdroid")
                    .setApiKey("AIzaSyLocalDevMockKey1234567890abcdef")
                    .setProjectId("cinetrack-dssmv")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }
    }
}
