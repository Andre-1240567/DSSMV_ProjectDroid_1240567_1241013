package pt.isep.dssmv.projectdroid.controllers;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import pt.isep.dssmv.projectdroid.BuildConfig;
import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.adapters.CinemaAdapter;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.model.Cinema;
import pt.isep.dssmv.projectdroid.network.RetrofitClient;
import pt.isep.dssmv.projectdroid.network.dto.FsqPlaceDto;
import pt.isep.dssmv.projectdroid.network.dto.FsqPlacesResponse;
import pt.isep.dssmv.projectdroid.utils.LocationHelper;
import pt.isep.dssmv.projectdroid.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CinemasActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private enum ViewMode { BOTH, MAP_ONLY, LIST_ONLY }
    private ViewMode currentViewMode = ViewMode.BOTH;

    private Button buttonBack;
    private Button buttonRefresh;
    private TextView textViewRadiusInfo;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private RecyclerView recyclerViewCinemas;
    private MapView mapView;
    private MaterialButtonToggleGroup toggleGroupViewMode;

    private CinemaAdapter adapter;
    private SharedPrefManager prefManager;

    private final List<Cinema> currentCinemas = new ArrayList<>();
    private final Map<String, Marker> cinemaMarkers = new HashMap<>();
    private GeoPoint currentUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure OSMDroid storage & user agent before layout inflation
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid"));

        setContentView(R.layout.activity_cinemas);

        prefManager = SharedPrefManager.getInstance(this);

        initViews();
        setupMap();
        setupRecyclerView();
        setupListeners();
        checkPermissionsAndLoadCinemas();
    }

    private void initViews() {
        buttonBack = findViewById(R.id.buttonCinemasBack);
        buttonRefresh = findViewById(R.id.buttonRefreshCinemas);
        textViewRadiusInfo = findViewById(R.id.textViewRadiusInfo);
        progressBar = findViewById(R.id.progressBarCinemas);
        textViewEmpty = findViewById(R.id.textViewCinemasEmpty);
        recyclerViewCinemas = findViewById(R.id.recyclerViewCinemas);
        mapView = findViewById(R.id.mapViewCinemas);
        toggleGroupViewMode = findViewById(R.id.toggleGroupViewMode);
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.getController().setZoom(13.5);
    }

    private void setupRecyclerView() {
        adapter = new CinemaAdapter(this::onCinemaSelectedFromList, this::openCinemaNavigation);
        recyclerViewCinemas.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCinemas.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonRefresh.setOnClickListener(v -> checkPermissionsAndLoadCinemas());

        toggleGroupViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.buttonModeBoth) {
                setViewMode(ViewMode.BOTH);
            } else if (checkedId == R.id.buttonModeMap) {
                setViewMode(ViewMode.MAP_ONLY);
            } else if (checkedId == R.id.buttonModeList) {
                setViewMode(ViewMode.LIST_ONLY);
            }
        });
    }

    private void setViewMode(ViewMode mode) {
        this.currentViewMode = mode;
        switch (mode) {
            case BOTH:
                mapView.setVisibility(View.VISIBLE);
                recyclerViewCinemas.setVisibility(currentCinemas.isEmpty() ? View.GONE : View.VISIBLE);
                break;
            case MAP_ONLY:
                mapView.setVisibility(View.VISIBLE);
                recyclerViewCinemas.setVisibility(View.GONE);
                break;
            case LIST_ONLY:
                mapView.setVisibility(View.GONE);
                recyclerViewCinemas.setVisibility(currentCinemas.isEmpty() ? View.GONE : View.VISIBLE);
                break;
        }
    }

    /**
     * AC01: Runtime permission request for ACCESS_FINE_LOCATION.
     */
    private void checkPermissionsAndLoadCinemas() {
        if (!LocationHelper.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        // FA2: Check if GPS / Location services are enabled
        if (!LocationHelper.isLocationServiceEnabled(this)) {
            showEnableGpsDialog();
            return;
        }

        fetchLocationAndCinemas();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndLoadCinemas();
            } else {
                // FA1: Location permission denied
                progressBar.setVisibility(View.GONE);
                textViewEmpty.setText(R.string.error_location_permission_denied);
                textViewEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(this, R.string.error_location_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showEnableGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_gps)
                .setMessage(R.string.dialog_message_gps)
                .setPositiveButton(R.string.btn_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /**
     * AC02 & AC03: Reads GPS coordinates, retrieves configured search radius,
     * updates the map center & radius circumference, and queries Foursquare.
     */
    private void fetchLocationAndCinemas() {
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        int radiusKm = prefManager.getCinemaRadiusKm();
        textViewRadiusInfo.setText(getString(R.string.cinemas_radius_info, radiusKm));

        LocationHelper.getCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationResult(double latitude, double longitude) {
                currentUserLocation = new GeoPoint(latitude, longitude);
                centerMapAndDrawRadius(currentUserLocation, radiusKm);
                queryFoursquareCinemas(latitude, longitude, radiusKm);
            }

            @Override
            public void onLocationError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                textViewEmpty.setText(getString(R.string.error_gps_coordinates, errorMessage));
                textViewEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Centers the map camera on the user's GPS coordinates, creates the user marker,
     * and draws the bluish translucent circle matching the user's selected radius.
     */
    private void centerMapAndDrawRadius(GeoPoint userPoint, int radiusKm) {
        mapView.getController().setZoom(computeZoomLevel(radiusKm));
        mapView.getController().setCenter(userPoint);

        // Clear existing overlays
        mapView.getOverlays().clear();
        cinemaMarkers.clear();

        // 1. Bluish radius circumference
        double radiusMeters = radiusKm * 1000.0;
        Polygon circle = new Polygon(mapView);
        circle.setPoints(Polygon.pointsAsCircle(userPoint, radiusMeters));
        circle.setFillColor(0x262196F3);   // ~15% alpha translucent light blue
        circle.setStrokeColor(0xFF2196F3); // Solid blue border
        circle.setStrokeWidth(3.0f);
        circle.setTitle(getString(R.string.cinemas_radius_info, radiusKm));
        mapView.getOverlays().add(circle);

        // 2. User location marker
        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(userPoint);
        userMarker.setTitle(getString(R.string.label_user_location));
        userMarker.setSnippet(getString(R.string.label_user_location_snippet));
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        Drawable userIcon = ContextCompat.getDrawable(this, R.drawable.ic_my_location);
        if (userIcon != null) {
            userMarker.setIcon(userIcon);
        }
        mapView.getOverlays().add(userMarker);

        mapView.invalidate();
    }

    private double computeZoomLevel(int radiusKm) {
        if (radiusKm <= 5) return 13.5;
        if (radiusKm <= 10) return 12.5;
        if (radiusKm <= 25) return 11.5;
        if (radiusKm <= 50) return 10.0;
        return 9.0;
    }

    private void queryFoursquareCinemas(double latitude, double longitude, int radiusKm) {
        String latLng = latitude + "," + longitude;
        int radiusMeters = Math.min(radiusKm * 1000, 100000); // Foursquare max 100km

        String authKey = BuildConfig.FOURSQUARE_API_KEY;
        if (authKey == null || authKey.trim().isEmpty() || authKey.contains("your_foursquare_api_key")) {
            progressBar.setVisibility(View.GONE);
            textViewEmpty.setText("Foursquare API key is missing. Please configure FOURSQUARE_API_KEY in your .env file.");
            textViewEmpty.setVisibility(View.VISIBLE);
            return;
        }

        RetrofitClient.getInstance().getFoursquareApiService()
                .searchNearbyCinemas(latLng, "cinema", "10024", radiusMeters, "DISTANCE", 20)
                .enqueue(new Callback<FsqPlacesResponse>() {
                    @Override
                    public void onResponse(Call<FsqPlacesResponse> call, Response<FsqPlacesResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<FsqPlaceDto> results = response.body().getResults();
                            List<Cinema> domainCinemas = mapToDomainCinemas(results);

                            currentCinemas.clear();
                            currentCinemas.addAll(domainCinemas);

                            if (domainCinemas.isEmpty()) {
                                textViewEmpty.setText(R.string.empty_nearby_cinemas);
                                textViewEmpty.setVisibility(View.VISIBLE);
                                adapter.setCinemas(currentCinemas);
                                setViewMode(currentViewMode);
                            } else {
                                textViewEmpty.setVisibility(View.GONE);
                                adapter.setCinemas(domainCinemas);
                                plotCinemaMarkersOnMap(domainCinemas);
                                setViewMode(currentViewMode);
                            }
                        } else {
                            textViewEmpty.setText(getString(R.string.error_foursquare_api, "HTTP " + response.code()));
                            textViewEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<FsqPlacesResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        textViewEmpty.setText(getString(R.string.error_foursquare_api, t.getMessage()));
                        textViewEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * Plots cinema pins on the OpenStreetMap overlay with popups displaying cinema details.
     */
    private void plotCinemaMarkersOnMap(List<Cinema> cinemas) {
        Drawable pinIcon = ContextCompat.getDrawable(this, R.drawable.ic_cinema_pin);

        for (Cinema cinema : cinemas) {
            GeoPoint cinemaPoint = new GeoPoint(cinema.getLatitude(), cinema.getLongitude());
            Marker marker = new Marker(mapView);
            marker.setPosition(cinemaPoint);
            marker.setTitle(cinema.getName());

            String snippet = cinema.getDistanceMeters() > 0
                    ? cinema.getDistanceMeters() + "m • " + cinema.getAddress()
                    : cinema.getAddress();
            marker.setSnippet(snippet);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            if (pinIcon != null) {
                marker.setIcon(pinIcon);
            }

            // Tapping a marker animates to it and displays the popup info bubble
            marker.setOnMarkerClickListener((m, map) -> {
                m.showInfoWindow();
                map.getController().animateTo(m.getPosition());
                return true;
            });

            // Tapping the info bubble itself triggers Google Maps navigation
            if (marker.getInfoWindow() != null && marker.getInfoWindow().getView() != null) {
                marker.getInfoWindow().getView().setOnClickListener(v -> openCinemaNavigation(cinema));
            }

            mapView.getOverlays().add(marker);
            cinemaMarkers.put(cinema.getFsqId(), marker);
        }

        mapView.invalidate();
    }

    /**
     * Triggered when a user taps a cinema card in the recycler view:
     * Centers the map on the selected cinema and displays its popup info window.
     */
    private void onCinemaSelectedFromList(Cinema cinema) {
        if (currentViewMode == ViewMode.LIST_ONLY) {
            toggleGroupViewMode.check(R.id.buttonModeBoth);
            setViewMode(ViewMode.BOTH);
        }

        Marker marker = cinemaMarkers.get(cinema.getFsqId());
        if (marker != null) {
            mapView.getController().animateTo(marker.getPosition());
            marker.showInfoWindow();
        }
    }

    private List<Cinema> mapToDomainCinemas(List<FsqPlaceDto> dtos) {
        List<Cinema> cinemas = new ArrayList<>();
        if (dtos == null) return cinemas;

        for (FsqPlaceDto dto : dtos) {
            try {
                Cinema cinema = new Cinema(dto.getFsqId(), dto.getName(), dto.getLatitude(), dto.getLongitude());
                cinema.setAddress(dto.getDisplayAddress());
                cinema.setDistanceMeters(dto.getDistance());
                cinemas.add(cinema);
            } catch (InvalidDataException ignored) {
            }
        }
        return cinemas;
    }

    /**
     * AC06: Dispatches an Implicit Intent to open navigation in Google Maps or web fallback.
     */
    private void openCinemaNavigation(Cinema cinema) {
        Uri gmmIntentUri = Uri.parse("geo:" + cinema.getLatitude() + "," + cinema.getLongitude()
                + "?q=" + Uri.encode(cinema.getName()));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException ex) {
            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + cinema.getLatitude() + "," + cinema.getLongitude());
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
