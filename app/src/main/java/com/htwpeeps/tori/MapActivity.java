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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
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

/**
 * This Class holds all necessary functionalities for the second screen with the map.
 */
public class MapActivity extends AppCompatActivity {
    public static final int LOCATION_UPDATE_INTERVAL = 5;
    //necessary for the map
    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay mLocationOverlay;
    private static final int PERMISSION_REQUEST_CODE = 1;

    // placeholder values for starting point, for now they are set to frauenkirche in dresden
    double latitude = 51.051873;
    double longitude = 13.741522;

    //API from Google for location
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Button endButton = (Button) findViewById(R.id.end_button);
        endButton.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));


        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        initializeMap();

        // update longitude and latitude
        updateGPS();

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
        GeoPoint startPoint = new GeoPoint(latitude, longitude);

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

    private void updateGPS(){

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }


        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            updateMap(mapController);
        });

    }

    private void updateMap(IMapController mapController){
        GeoPoint point = new GeoPoint(latitude, longitude);
        mapController.setCenter(point);
        //this moves the map to actually center the start point
        mapController.animateTo(point);
    }

}
