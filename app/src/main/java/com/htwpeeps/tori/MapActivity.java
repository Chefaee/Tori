package com.htwpeeps.tori;

// standard android import
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

// ui imports
import android.widget.Button;
import android.widget.TextView;

// androidx imports
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

// location api from google
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

//open street map imports
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * This Class holds all necessary functionalities for the second screen with the map.
 * This includes requesting permissions for location tracking, the tracking itself as well as the drawing of the map.
 */
public class MapActivity extends AppCompatActivity {

    //necessary for the map
    //the map itself
    private MapView map;
    // controller to determine which part is currently shown
    private IMapController mapController;
    //overlay where the position as well as the line is drawn
    private MyLocationNewOverlay mLocationOverlay;
    // lin eof the last points
    private Polyline polylinePath;
    //necessary if permissions have to be asked for
    private static final int PERMISSION_REQUEST_CODE = 1;

    //API from Google for location
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    // these are later multiplied with 1000 for millisec
    public static final int LOCATION_UPDATE_INTERVAL = 5;
    public static final int LOCATION_UPDATE_PAUSED_INTERVAL = 10;

    // location
    LocationCallback locationCallBack;
    // placeholder values used for starting point, for now they are set to frauenkirche in dresden
    GeoPoint lastKnownPoint = new GeoPoint(51.051873, 13.741522);

    List<GeoPoint> last10Points = new ArrayList<>();

    boolean trackingIsPaused = false;

    //todo remove if not necessary
    boolean trackingIsInField = false;
    boolean changedIsInField = false;
    boolean movedWhilePaused = false;

    private String activity = "";


    //todo check and fix possible problems when exiting and entering the map again
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        TextView status = (TextView) findViewById(R.id.status_textView);
        status.setText("");

        Button endButton = (Button) findViewById(R.id.end_button);
        endButton.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            activity = extras.getString("activity");
        }

        locationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        1000 * LOCATION_UPDATE_INTERVAL
                ).setGranularity(Granularity.GRANULARITY_FINE)
                        .setMinUpdateIntervalMillis(1000 * LOCATION_UPDATE_INTERVAL).build();

        Button pauseButton = (Button) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(v -> {
            if (!trackingIsPaused) {
                trackingIsPaused = true;

                //creates a new locationRequest with a longer interval between location updates, as paused location should not change
                locationRequest =
                        new LocationRequest.Builder(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                1000 * LOCATION_UPDATE_PAUSED_INTERVAL
                        ).setGranularity(Granularity.GRANULARITY_FINE)
                                .setMinUpdateIntervalMillis(1000 * LOCATION_UPDATE_PAUSED_INTERVAL).build();

                //called to update the request
                tracking();

                //ui updates
                status.setText(getString(R.string.pause_status));
                pauseButton.setText(getString(R.string.cont_button));
            } else {
                trackingIsPaused = false;
                // recreates the original locationRequest with the standard interval for tracking
                locationRequest =
                        new LocationRequest.Builder(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                1000 * LOCATION_UPDATE_INTERVAL
                        ).setGranularity(Granularity.GRANULARITY_FINE)
                                .setMinUpdateIntervalMillis(1000 * LOCATION_UPDATE_INTERVAL).build();

                //called to update the request
                tracking();

                //ui updates
                status.setText("");
                pauseButton.setText(getString(R.string.pause_button));

            }

        });


        initializeMap();

        /* This is called everytime a location request is made and returned a result.
         * As long as a valid location is returned all functions to update values and ui as well as the api are called.
         */
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                System.out.println(Instant.now().getEpochSecond());

                if (locationResult.getLastLocation() == null) {
                    return;
                }

                UpdatePointValues(locationResult.getLastLocation());
                updateMap();
                drawPathLine();
                sendApiCall();
            }
        };

        updateFirstGPS();
        tracking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // for now the tracking is stopped when the activity ends
        stopTracking();
    }

    /**
     * Creates an osm-map in the view.
     * Therefore the view element is accessed and filed with map tiles, any map controls are disabled.
     * Then the map is set to the starting point, which is most of the times the placeholder from the start.
     * Everything is set to a fixed zoom, all necessary layers (for displaying lines and location) are created, the icon is replaced with a custom icon and the line is defined.
     */
    private void initializeMap() {
        Context ctx = getApplicationContext();
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
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);

        // overwrites the standard icons with a blue point
        Drawable currentDraw = ResourcesCompat.getDrawable(getResources(), R.mipmap.map_point_marker_smaller, null);
        Bitmap currentIcon;
        if (currentDraw != null) {
            currentIcon = ((BitmapDrawable) currentDraw).getBitmap();
            this.mLocationOverlay.setPersonIcon(currentIcon);
            this.mLocationOverlay.setPersonAnchor(0.5f, 0.5f);
            this.mLocationOverlay.setDirectionIcon(currentIcon);
            this.mLocationOverlay.setDirectionAnchor(0.5f, 0.5f);
        }

        // add layer to display the location
        this.mLocationOverlay.enableMyLocation();
        this.mLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(this.mLocationOverlay);

        // to style the line, one has to access the paint and update the values on that
        polylinePath = new Polyline();
        Paint polypaint = polylinePath.getOutlinePaint();
        polypaint.setColor(Color.parseColor("#5ce1e6"));
        polypaint.setStrokeWidth(6.5f);
        polypaint.setPathEffect(new DashPathEffect(new float[]{7.5f, 7.5f}, 0));

        // adds the line layer
        map.getOverlays().add(polylinePath);

    }

    /**
     * Upon start of tracking, the list of the last known points is initialized at the current location.
     * It also initializes the value for the lastKnownPoint
     * This should prevent an unwanted line upon the update to the first location
     *
     * @param location current location returned by the location request
     */
    private void initializeLastPointList(Location location) {
        lastKnownPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        last10Points.add(0, lastKnownPoint);
        last10Points.add(1, lastKnownPoint);
        last10Points.add(2, lastKnownPoint);
        last10Points.add(3, lastKnownPoint);
        last10Points.add(4, lastKnownPoint);
        last10Points.add(5, lastKnownPoint);
        last10Points.add(6, lastKnownPoint);
        last10Points.add(7, lastKnownPoint);
        last10Points.add(8, lastKnownPoint);
        last10Points.add(9, lastKnownPoint);
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
            System.out.println(Instant.now().getEpochSecond());
            UpdatePointValues(location);
            updateMap();
            drawPathLine();
            //sendApiCall();
        });

    }

    /**
     * The first location update is different to all following as the function to initialize the lastPointList must be called.
     * Afterwards the map is updated and the api called
     */
    private void updateFirstGPS() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }


        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            initializeLastPointList(location);
            updateMap();
            sendApiCall();
        });

    }

    /**
     * This function centers the map on the current position and if it deems it necessary moves the map tiles accordingly.
     */
    private void updateMap() {
        mapController.setCenter(lastKnownPoint);
        //this moves the map to actually center the start point
        mapController.animateTo(lastKnownPoint);
        System.out.println("Map:" + Instant.now().getEpochSecond());
    }

    /**
     * Updates the line on the map.
     * There are always only 10 points in use, but the length of the line might differ depending on the speed the device is moving.
     */
    private void drawPathLine() {
        polylinePath.setPoints(last10Points);
    }

    /**
     * The current location is saved and added at the end of the pointsList. The then oldest point is removed.
     *
     * @param location current location returned by location request
     */
    private void UpdatePointValues(Location location) {
        lastKnownPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        last10Points.remove(0);
        last10Points.add(9, lastKnownPoint);
    }

    /**
     * This function checks for permissions before setting the fusedLocationProviderClient to request updates with the current locationRequest and a callback.
     * It needs to be called everytime the location request changes (e.g. in interval or priority)
     */
    private void tracking() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        //updateGPS();
    }
    /**
     * Stops all tracking requests.
     */
    private void stopTracking() {

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }


    /**
     * Taking the saved lastPoint, the current time and the set activity, this function calls to the server and relays these information.
     * It waits for an answer to work with.
     */
    private void sendApiCall() {
        ApiCall apiCall = new ApiCall(lastKnownPoint.getLatitude(), lastKnownPoint.getLongitude(), Instant.now().getEpochSecond(), activity, result -> {
            // Here you girls can do whatever frontend-stuff you want with fieldIndex, for example:
            if (result.fieldIndex != null) {
                System.out.println(result.fieldIndex + ", " + result.responseCode);
            } else if (result.responseCode == null) {
                System.out.println("Cant establish network connection to server :(");
            } else {
                System.out.println("There are problems with the server." +
                        "Http-Response Code " + result.responseCode);
            }
        });
        apiCall.execute();
    }

}
