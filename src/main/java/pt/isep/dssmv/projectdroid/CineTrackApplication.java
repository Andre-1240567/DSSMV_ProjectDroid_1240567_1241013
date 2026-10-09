package pt.isep.dssmv.projectdroid;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class CineTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:579731470100:android:82a9c3049898b008d73401")
                    .setApiKey("AIzaSyBY71dY9g-mq7AW46vZVm2O6Panw_u_l4Q")
                    .setProjectId("cinetrack-dssmv")
                    .setStorageBucket("cinetrack-dssmv.firebasestorage.app")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }
    }
}
