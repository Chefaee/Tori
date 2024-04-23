package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

/**
 * Activity class that holds the functionalities for the first screen.
 * It supplies the dropdown values for possible activities on a field as well as the change to the map activity.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));


        setDropdownValues();
        ApiCall apiCall = new ApiCall(1, 2, 3, "plow", result -> {
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

    /**
     * This methods sets the values that are given in the activity dropdown.
     * To change the items add, update or remove an item in the {@link com.htwpeeps.tori.R.array#activity_array}.
     * Remember to update in all translations as well
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
}