package com.htwpeeps.tori;

import static android.util.Log.println;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This Class holds all necessary functionalities for the second screen with the map.
 */
public class MapActivity extends AppCompatActivity {

    //necessary for the map
    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay mLocationOverlay;
    private static final int PERMISSION_REQUEST_CODE = 1;

    //API from Google for location
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    public static final int LOCATION_UPDATE_INTERVAL = 5;
    public static final int LOCATION_UPDATE_PAUSED_INTERVAL = 10;

    // tracking
    /*
     last point
     starttime since no change
     is paused
     is field
     fieldchange?

     moved while paused?
     */
    // placeholder values used for starting point, for now they are set to frauenkirche in dresden
    LocationCallback locationCallBack;
    GeoPoint lastKnownPoint = new GeoPoint(51.051873, 13.741522);

    List<GeoPoint> last10Points = new ArrayList<>();

    boolean trackingIsPaused = false;
    boolean trackingIsInField = false;
    boolean changedIsInField = false;
    boolean movedWhilePaused = false;

    private String activity = "plowing";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        TextView status = (TextView) findViewById(R.id.status_textView);
        status.setText("");

        Button endButton = (Button) findViewById(R.id.end_button);
        endButton.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        Button pauseButton = (Button) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(v -> {
            if (!trackingIsPaused){
                trackingIsPaused = true;
                status.setText(getString(R.string.pause_status));
                locationRequest.setInterval(1000 * LOCATION_UPDATE_PAUSED_INTERVAL);
                pauseButton.setText(getString(R.string.cont_button));
            }
            else {
                trackingIsPaused = false;
                status.setText("");
                locationRequest.setInterval(1000 * LOCATION_UPDATE_INTERVAL);
                pauseButton.setText( getString(R.string.pause_button));

            }

        });



        initializeMap();
        initializeLastPointList();
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                UpdatePointValues(locationResult.getLastLocation());
                updateMap(mapController);
                sendApiCall();
            }
        };

        updateGPS();
        tracking();
    }

    private void initializeMap() {
        Context ctx = getApplicationContext();
        //TODO refactor deprication
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string
        map = findViewById(R.id.osmmap);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //do not show the zoom controls
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        //this disables zooming and scrolling
        map.setOnTouchListener((v, event) -> true);

        //creates the start point with the given values
        GeoPoint startPoint = lastKnownPoint;

        mapController = map.getController();
        mapController.setZoom(19.5);
        mapController.setCenter(startPoint);
        //this moves the map to actually center the start point
        mapController.animateTo(startPoint);

        //shows the current location on the map
        //TODO style to a point not a person
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        this.mLocationOverlay.enableMyLocation();
        this.mLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(this.mLocationOverlay);
    }

    private void initializeLastPointList() {
        last10Points.add(0, new GeoPoint(51.051873, 13.741522));
        last10Points.add(1, new GeoPoint(51.051873, 13.741522));
        last10Points.add(2, new GeoPoint(51.051873, 13.741522));
        last10Points.add(3, new GeoPoint(51.051873, 13.741522));
        last10Points.add(4, new GeoPoint(51.051873, 13.741522));
        last10Points.add(5, new GeoPoint(51.051873, 13.741522));
        last10Points.add(6, new GeoPoint(51.051873, 13.741522));
        last10Points.add(7, new GeoPoint(51.051873, 13.741522));
        last10Points.add(8, new GeoPoint(51.051873, 13.741522));
        last10Points.add(9, new GeoPoint(51.051873, 13.741522));
    }

    private void updateGPS() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }


        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            UpdatePointValues(location);
            updateMap(mapController);
            sendApiCall();
        });

    }

    private void updateMap(IMapController mapController) {
        mapController.setCenter(lastKnownPoint);
        //this moves the map to actually center the start point
        mapController.animateTo(lastKnownPoint);
    }

    private void UpdatePointValues(Location location) {
        System.out.println(new Date());
        lastKnownPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        last10Points.remove(0);
        last10Points.add(9, lastKnownPoint);
    }

    private void tracking() {

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }


    private void sendApiCall(){
        ApiCall apiCall = new ApiCall((int) lastKnownPoint.getLatitude(), (int) lastKnownPoint.getLongitude(), (int) Instant.now().getEpochSecond(), activity, result -> {
            // Here you girls can do whatever frontend-stuff you want with fieldIndex, for example:
            if (result.fieldIndex != null) {
                System.out.println(result.fieldIndex + ", " + result.responseCode);
            } else if (result.responseCode == null ) {
                System.out.println("Cant establish network connection to server :(");
            } else {
                System.out.println("There are problems with the server." +
                        "Http-Response Code " + result.responseCode);
            }
        });
        apiCall.execute();
    }

}
