package pt.isep.dssmv.projectdroid.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * Lifecycle-aware monitor for real-time network connectivity changes using modern
 * ConnectivityManager.NetworkCallback (Android 8.0+ / API 26+).
 * Avoids legacy and deprecated BroadcastReceiver CONNECTIVITY_ACTION.
 */
public class NetworkMonitor {

    public interface NetworkStatusListener {
        void onNetworkChanged(boolean isConnected);
    }

    private final ConnectivityManager connectivityManager;
    private final NetworkStatusListener listener;
    private final Handler mainHandler;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isRegistered = false;

    public NetworkMonitor(@NonNull Context context, @NonNull NetworkStatusListener listener) {
        this.connectivityManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Checks if active network currently has internet capability.
     */
    public boolean isCurrentlyConnected() {
        if (connectivityManager == null) return false;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return false;
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Registers the network callback. Call in onStart().
     */
    public void startMonitoring() {
        if (connectivityManager == null || isRegistered) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                mainHandler.post(() -> listener.onNetworkChanged(true));
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Check if any other network (e.g. mobile data after wifi lost) is available
                boolean stillConnected = isCurrentlyConnected();
                mainHandler.post(() -> listener.onNetworkChanged(stillConnected));
            }
        };

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback);
            isRegistered = true;
        } catch (SecurityException ignored) {
            // Missing permission or restricted sandbox fallback
        }
    }

    /**
     * Unregisters the network callback. Call in onStop().
     */
    public void stopMonitoring() {
        if (connectivityManager == null || !isRegistered || networkCallback == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception ignored) {
        } finally {
            isRegistered = false;
            networkCallback = null;
        }
    }
}
