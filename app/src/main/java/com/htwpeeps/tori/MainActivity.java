package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
}