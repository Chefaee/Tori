package com.htwpeeps.tori;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiCall {

    private static final String apiUrl = "http://192.168.0.104:3000/api/checkPos";

    private final int latitude;
    private final int longitude;
    private final int timestamp;
    private final String activity;

    private final ApiCallback callBack;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ApiCall(int latitude, int longitude, int timestamp, String activity, ApiCallback callback) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.activity = activity;

        this.callBack = callback;
    }


    public void execute() {
        executorService.execute(() -> {
            //Integer result = doInBackground();
            ResponseObject result = doInBackground();
            handler.post(() -> {
                callBack.onResult(result);
            });
        });
    }

    protected ResponseObject doInBackground() {
        if (!isApiReachable()) {
            // Api ist nicht erreichbar
            return new ResponseObject(null, null);
        }

        HttpURLConnection connection = establishConnection(apiUrl);
        if (connection == null) {
            // Sollte eigentlich nicht passieren
            return new ResponseObject(null, null);
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

        Pair<JSONObject, Integer> response = performApiCall(connection, jsonRequest);
        assert response != null;
        JSONObject responseObject = response.first;

        int fieldIndex;

        try {
            fieldIndex = responseObject.getInt("fieldIndex");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return new ResponseObject(fieldIndex, response.second);
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

    private static Pair<JSONObject, Integer> performApiCall(HttpURLConnection connection, JSONObject jsonRequest) {
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
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Antwort verarbeiten (als JSON)
            return new Pair<>(new JSONObject(response.toString()), responseCode);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface ApiCallback {
        void onResult(ResponseObject result);
    }

    private boolean isApiReachable() {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // Verwendung von HEAD-Methode, um nur Header zu erhalten
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

