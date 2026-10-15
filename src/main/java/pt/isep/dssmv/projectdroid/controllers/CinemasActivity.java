package pt.isep.dssmv.projectdroid.controllers;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;

public class CinemasActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private Button buttonBack;
    private Button buttonRefresh;
    private TextView textViewRadiusInfo;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private RecyclerView recyclerViewCinemas;

    private CinemaAdapter adapter;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cinemas);

        prefManager = SharedPrefManager.getInstance(this);

        initViews();
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
    }

    private void setupRecyclerView() {
        adapter = new CinemaAdapter(this::openCinemaNavigation);
        recyclerViewCinemas.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCinemas.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonRefresh.setOnClickListener(v -> checkPermissionsAndLoadCinemas());
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
     * and calls Foursquare Places API.
     */
    private void fetchLocationAndCinemas() {
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        recyclerViewCinemas.setVisibility(View.GONE);

        int radiusKm = prefManager.getCinemaRadiusKm();
        textViewRadiusInfo.setText(getString(R.string.cinemas_radius_info, radiusKm));

        LocationHelper.getCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationResult(double latitude, double longitude) {
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

    private void queryFoursquareCinemas(double latitude, double longitude, int radiusKm) {
        String latLng = latitude + "," + longitude;
        int radiusMeters = Math.min(radiusKm * 1000, 100000); // Foursquare max 100km

        String authKey = BuildConfig.FOURSQUARE_API_KEY;

        RetrofitClient.getInstance().getFoursquareApiService()
                .searchNearbyCinemas(authKey, latLng, "10024", radiusMeters, "DISTANCE", 20)
                .enqueue(new Callback<FsqPlacesResponse>() {
                    @Override
                    public void onResponse(Call<FsqPlacesResponse> call, Response<FsqPlacesResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<FsqPlaceDto> results = response.body().getResults();
                            List<Cinema> domainCinemas = mapToDomainCinemas(results);

                            if (domainCinemas.isEmpty()) {
                                textViewEmpty.setText(R.string.empty_nearby_cinemas);
                                textViewEmpty.setVisibility(View.VISIBLE);
                                recyclerViewCinemas.setVisibility(View.GONE);
                            } else {
                                adapter.setCinemas(domainCinemas);
                                recyclerViewCinemas.setVisibility(View.VISIBLE);
                                textViewEmpty.setVisibility(View.GONE);
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
}
