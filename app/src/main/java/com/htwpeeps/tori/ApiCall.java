package com.htwpeeps.tori;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiCall extends AsyncTask<Void, Void, Integer> {

    private static final String apiUrl = "http://192.168.0.104:3000/api/checkPos";

    private int latitude;
    private int longitude;
    private int timestamp;
    private String activity;

    public ApiCall(int latitude, int longitude, int timestamp, String activity) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.activity = activity;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        HttpURLConnection connection = establishConnection(apiUrl);
        if (connection == null) {
            return null;
        }

        // JSON-Daten erstellen
        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("latitude", latitude);
            jsonRequest.put("longitude", longitude);
            jsonRequest.put("timestamp", timestamp);
            jsonRequest.put("activity", activity);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject response = performApiCall(connection, jsonRequest);

        int fieldIndex;

        try {
            fieldIndex = response.getInt("fieldIndex");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return fieldIndex;
    }

    private static HttpURLConnection establishConnection(String apiUrl) {
        try {
            // Verbindung zur API herstellen
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Verbindungseinstellungen konfigurieren
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            return connection;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject performApiCall(HttpURLConnection connection, JSONObject jsonRequest) {
        try {
            // JSON-Daten senden
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(jsonRequest.toString());
            outputStream.flush();
            outputStream.close();

            // Antwort von der API empfangen
            int responseCode = connection.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Antwort verarbeiten (als JSON)
            return new JSONObject(response.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
