package pt.isep.dssmv.projectdroid.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

public class LocationHelper {

    public interface LocationCallback {
        void onLocationResult(double latitude, double longitude);
        void onLocationError(String errorMessage);
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isLocationServiceEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        if (!hasLocationPermission(context)) {
            callback.onLocationError("Location permission not granted");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            callback.onLocationError("Location service is unavailable on this device");
            return;
        }

        if (!isLocationServiceEnabled(context)) {
            callback.onLocationError("Location services (GPS) are turned off");
            return;
        }

        // Try getting last known location first for immediate response
        Location bestLocation = null;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (bestLocation != null) {
            callback.onLocationResult(bestLocation.getLatitude(), bestLocation.getLongitude());
            return;
        }

        // Otherwise request a single fresh location update
        String provider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER
                : LocationManager.NETWORK_PROVIDER;

        final LocationListener[] listenerHolder = new LocationListener[1];
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                timeoutHandler.removeCallbacksAndMessages(null);
                try {
                    locationManager.removeUpdates(this);
                } catch (Exception ignored) {
                }
                if (location != null) {
                    callback.onLocationResult(location.getLatitude(), location.getLongitude());
                } else {
                    callback.onLocationError("Unable to acquire location coordinates");
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        listenerHolder[0] = locationListener;

        try {
            locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper());

            // Timeout after 8 seconds
            timeoutHandler.postDelayed(() -> {
                try {
                    if (listenerHolder[0] != null) {
                        locationManager.removeUpdates(listenerHolder[0]);
                    }
                } catch (Exception ignored) {
                }
                callback.onLocationError("Location request timed out. Please verify GPS reception.");
            }, 8000);

        } catch (Exception e) {
            callback.onLocationError("Failed to request GPS updates: " + e.getMessage());
        }
    }
}
