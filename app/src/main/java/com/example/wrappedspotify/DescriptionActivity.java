package com.example.wrappedspotify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DescriptionActivity extends AppCompatActivity {

    private EditText etMusicTastes;
    private TextView tvDescription;
    private DatabaseReference mDatabase;
    private FirebaseUser user;
    private List<String> topTracks;
    private Button btnGenerate;

    private Button home;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvDescription = findViewById(R.id.tvDescription);
        btnGenerate = findViewById(R.id.btnGenerate);
        user = FirebaseAuth.getInstance().getCurrentUser();

        fetchSpotifyData();

        home = findViewById(R.id.btnBackToHome);
        home.setOnClickListener(v -> {
            Intent intent = new Intent(DescriptionActivity.this, HomePageActivity.class);
            startActivity(intent);
        });


        btnGenerate.setOnClickListener(view -> {
            if (topTracks != null && !topTracks.isEmpty()) {
                getOpenAIDescription();
            } else {
                Toast.makeText(DescriptionActivity.this, "Please wait until the data is loaded.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSpotifyData() {
        if (user != null) {
            String userId = user.getUid();
            mDatabase.child("users").child(userId).child("spotifyData").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
                        topTracks = dataSnapshot.child("topTracks").getValue(t);
                    } else {
                        Toast.makeText(DescriptionActivity.this, "No Spotify data found.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(DescriptionActivity.this, "Failed to fetch Spotify data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getOpenAIDescription() {
        executorService.submit(() -> {
            String descriptionResult = generateDescription();
            runOnUiThread(() -> tvDescription.setText(descriptionResult));
        });
    }

    private String generateDescription() {
        String url = "https://api.openai.com/v1/chat/completions";
        String apiKey = BuildConfig.OPENAI_API_KEY;


        String model = "gpt-3.5-turbo";
        String songs = String.join(", ", topTracks.subList(0, Math.min(3, topTracks.size())));

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            String body = String.format("{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}, {\"role\": \"user\", \"content\": \"Describe the personal style, and fashion of someone who listens to %s. keep the response very short and simple and give a list of 3 artists you would recommend \"}]}", model, songs.replace("\"", "\\\""));

            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();
                return extractMessageFromJSONResponse(response.toString());
            } else {
                return "Error: Server returned HTTP response code: " + responseCode;
            }
        } catch (IOException e) {
            Log.e("API Call Failure", "Error: " + e.getMessage());
            return "Network error: " + e.getMessage();
        }
    }

    private String extractMessageFromJSONResponse(String response) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(response);
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray choices = jsonObject.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                if (message != null) {
                    return message.get("content").getAsString();
                }
            }
        }
        return "No valid response found.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
