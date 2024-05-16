package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApiCall apiCall = new ApiCall(1, 2, 3, "plow", fieldIndex -> {
            // Hier könnt ihr Mädels dann mit dem AckerIndex machen was ihr wollt :)
            if (fieldIndex != null) {
                System.out.println(fieldIndex);
            } else {
                System.out.println("ist Null :(");
            }
        });
        apiCall.execute();

    }
}