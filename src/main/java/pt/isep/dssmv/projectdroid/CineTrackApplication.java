package pt.isep.dssmv.projectdroid;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class CineTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
