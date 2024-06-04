package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

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
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("activity", activeDropDownString);
            startActivity(intent);
        });

        ApiCall apiCall = new ApiCall(
                51.89700414986401,
                -1.368870281720362,
                1716995439,
                "plow",
                result -> {

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