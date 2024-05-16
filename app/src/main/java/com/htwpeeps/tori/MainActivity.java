package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApiCall apiCall = new ApiCall(1, 2, 3, "plow", result -> {
            // Hier könnt ihr Mädels dann mit dem AckerIndex machen was ihr wollt :)
            if (result.fieldIndex != null) {
                System.out.println(result.fieldIndex + ", " + result.responseCode);
            } else if (result.responseCode == null ) {
                System.out.println("Es kann keine Netzwekverbindung zum Server aufgebaut werden :(");
            } else {
                System.out.println("Es gibt Probleme mit dem Server. Http-Response Code " + result.responseCode);
            }

        });
        apiCall.execute();

    }
}