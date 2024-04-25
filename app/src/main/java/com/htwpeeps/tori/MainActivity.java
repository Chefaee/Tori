package com.htwpeeps.tori;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ApiCall(1, 2, 3, "vier") {
            @Override
            protected void onPostExecute(Integer fieldIndex) {
                // Hier können Sie den fieldIndex verwenden
                if (fieldIndex != null) {
                    System.out.println(fieldIndex);
                } else {
                    System.out.println("ist Null :(");
                }
            }
        }.execute();

    }
}