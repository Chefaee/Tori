package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Activity class that holds the functionalities for the first screen.
 * It supplies the dropdown values for possible activities on a field as well as the change to the map activity.
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    String activeDropDownString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // handle everything regarding the activity dropdown
        Spinner activityDropdown = findViewById(R.id.activity_dropdown);
        // listener for dropdown
        activityDropdown.setOnItemSelectedListener(this);
        setDropdownValues();

        //set standard value if nothing selected to the first available item
        activeDropDownString = activityDropdown.getItemAtPosition(0).toString();

        Button startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            // check for active location transfer
            if (isLocationEnabled(this)) {
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("activity", activeDropDownString);
                startActivity(intent);
            } else {
                // show a toast that location transfer is missing
                Toast.makeText(this, getString(R.string.noLocationEnabled),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = false;
        boolean isNetworkEnabled = false;

        try {
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            // GPS provider status check failed
        }

        try {
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            // Network provider status check failed
        }

        return isGpsEnabled || isNetworkEnabled;
    }

    /**
     * This methods sets the values that are given in the activity dropdown.
     * To change the items add, update or remove an item in the {@link com.htwpeeps.tori.R.array#activity_array}.
     * Remember to update in all translations as well
     * It would be possible to read all possible activities from another source (a database) as long as it can be accessed as an array of strings.
     */
    private void setDropdownValues() {
        //access spinner from xml
        Spinner activityDropdown = findViewById(R.id.activity_dropdown);
        //access string array from strings.xml
        String[] activityItems = getResources().getStringArray(R.array.activity_array);

        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, activityItems);
        //set the spinners adapter to the previously created one.
        activityDropdown.setAdapter(adapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        activeDropDownString = parent.getItemAtPosition(position).toString();
    }

    public void onNothingSelected(AdapterView<?> arg0) {

    }
}