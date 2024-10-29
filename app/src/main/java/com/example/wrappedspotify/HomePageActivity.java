package com.example.wrappedspotify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomePageActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private OkHttpClient mOkHttpClient = new OkHttpClient();
    private Button fetchTopSongsButton, requestTokenButton, viewHistoryButton,spotifyAI;
    private String mAccessToken;

    private static final String REDIRECT_URI = "com.example.wrappedspotify://auth";
    private static final String CLIENT_ID = "045795b20f1745d5960bbbef296de6a1";
    private static final int REQUEST_CODE = 1337;

    public static int radioButtonPressed = 0;
    private List<String> topTracks;
    private List<String> topArtists;
    private List<String> artistIds;
    private List<String> trackURIs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        fetchTopSongsButton = findViewById(R.id.buttonFetchTopSongs);
        fetchTopSongsButton.setOnClickListener(v -> fetchSpotifyAccessToken());

        requestTokenButton = findViewById(R.id.buttonRequestNewToken);
        requestTokenButton.setOnClickListener(v -> authenticateSpotify());

        viewHistoryButton = findViewById(R.id.buttonViewHistory);
        viewHistoryButton.setOnClickListener(v -> viewHistory());

        spotifyAI = findViewById(R.id.buttonSpotifyAI);
        spotifyAI.setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, DescriptionActivity.class);
            startActivity(intent);
        });

        Button logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> logoutUser());

        Button editAccountButton = findViewById(R.id.buttonEditAccount);
        editAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, EditAccountActivity.class);
            startActivity(intent);
        });
                RadioGroup radioGroup = findViewById(R.id.radioGroup);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio1) {
                    radioButtonPressed = 1;
                } else if (checkedId == R.id.radio2) {
                    radioButtonPressed = 2;
                } else if (checkedId == R.id.radio3) {
                    radioButtonPressed = 3;
                }else if(checkedId == R.id.radio4){
                    radioButtonPressed = 0;
                }
            }
        });
    }

    private void fetchSpotifyAccessToken() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            mDatabase.child("users").child(userId).child("spotifyToken")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            mAccessToken = task.getResult().getValue(String.class);
                            if (mAccessToken != null && !mAccessToken.isEmpty()) {
                                fetchTopDataFromSpotify();

                            } else {
                                Toast.makeText(HomePageActivity.this, "Access token not found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(HomePageActivity.this, "Failed to fetch access token.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }

    }
    private void fetchTopArtists() {
        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/artists?limit=7")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SpotifyAPI", "Network error while fetching data", e);
                runOnUiThread(() -> Toast.makeText(HomePageActivity.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e("SpotifyAPI", "Failed to fetch data: " + responseData);
                    runOnUiThread(() -> Toast.makeText(HomePageActivity.this, "Click the Update Spotify Information " + response.message(), Toast.LENGTH_LONG).show());
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray items = jsonObject.getJSONArray("items");
                     topArtists = new ArrayList<>();
                     artistIds = new ArrayList<>();

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject artist = items.getJSONObject(i);
                        topArtists.add(artist.getString("name"));
                        artistIds.add(artist.getString("id"));
                    }

                    fetchArtistGenres();
                } catch (Exception e) {
                    Log.e("SpotifyAPI", "Error parsing Spotify data", e);
                    runOnUiThread(() -> Toast.makeText(HomePageActivity.this, "Error parsing Spotify data: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }


    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(HomePageActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void fetchTopDataFromSpotify() {
        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/tracks?limit=7")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SpotifyAPI", "Network error while fetching data", e);
                runOnUiThread(() -> Toast.makeText(HomePageActivity.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e("SpotifyAPI", "Failed to fetch data: " + responseData);
                    runOnUiThread(() -> Toast.makeText(HomePageActivity.this, "Click the Update Spotify Information " + response.message(), Toast.LENGTH_LONG).show());
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray items = jsonObject.getJSONArray("items");
                    topTracks = new ArrayList<>();
                    topArtists = new ArrayList<>();
                   artistIds = new ArrayList<>();
                    trackURIs = new ArrayList<>();

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject track = items.getJSONObject(i);
                        topTracks.add(track.getString("name"));
                        trackURIs.add(track.getString("preview_url"));

                    }

                        fetchTopArtists();
                } catch (Exception e) {
                    Log.e("SpotifyAPI", "Error parsing Spotify data", e);
                    runOnUiThread(() -> Toast.makeText(HomePageActivity.this, "Error parsing Spotify data: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void authenticateSpotify() {
        AuthorizationRequest request = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setShowDialog(false)
                .setScopes(new String[]{"user-read-email", "user-top-read"})
                .build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                case TOKEN:
                    storeSpotifyToken(response.getAccessToken());
                    break;
                case ERROR:
                    Log.e("Home page", "Auth error: " + response.getError());
                    Toast.makeText(getApplicationContext(), "Auth error: " + response.getError(), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e("Home page", "Auth canceled");
                    Toast.makeText(getApplicationContext(), "Auth canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void storeSpotifyToken(String token) {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(HomePageActivity.this, "Firebase Auth user is null. Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child(userId).child("spotifyToken").setValue(token)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(HomePageActivity.this, "Spotify account linked successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(HomePageActivity.this, HomePageActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(HomePageActivity.this, "Failed to link Spotify account.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void viewHistory() {

    Intent intent = new Intent(HomePageActivity.this, HistoricalListsActivity.class);
    startActivity(intent);

    }

    /*public void getSongMp3File(){

        if (mAccessToken == null) {
            Toast.makeText(this, "You need to get an access token first!", Toast.LENGTH_SHORT).show();
            return;
        }
        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/tracks")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();
        cancelCall();
        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("HTTP", "Failed to fetch data: " + e);
                Toast.makeText(MainActivity.this, "Failed to fetch data, watch Logcat for more details",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    String songs = jsonObject.toString();
                    String[] preview_URLS = new String[5];
                    int startIndex = songs.indexOf("preview_url");
                    int endIndex = songs.indexOf("\",\"track_number");
                    for(int z = 0; z < 5; z++){
                        preview_URLS[z] = songs.substring(startIndex + 14, endIndex);
                        startIndex = songs.indexOf("preview_url", startIndex + 1);
                        endIndex = songs.indexOf("\",\"track_number", endIndex + 1);
                    }
                    String test = "//";
                    setTextAsync(
                            //String.valueOf(preview_URLS[0].replace("\\","").equals("https://p.scdn.co/mp3-preview/70bbac665619a7472a1563bbfbed43b558fc8019?cid=cfe923b2d660439caf2b557b21f31221")),
                            preview_URLS[0].replace("\\","").replace(" ",""),
                            topSongsIDTV);
                    //playClip("https://p.scdn.co/mp3-preview/70bbac665619a7472a1563bbfbed43b558fc8019?cid=cfe923b2d660439caf2b557b21f31221");
                    playClip(preview_URLS[4].replace("\\","").replace(" ",""));
                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }*/

    private void fetchArtistGenres() {
        List<String> topGenres = new ArrayList<>();
        AtomicInteger callsCompleted = new AtomicInteger(0);
        for (String id : artistIds) {
            Request artistRequest = new Request.Builder()
                    .url("https://api.spotify.com/v1/artists/" + id)
                    .addHeader("Authorization", "Bearer " + mAccessToken)
                    .build();

            mOkHttpClient.newCall(artistRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("SpotifyAPI", "Error fetching artist details", e);
                    if (callsCompleted.incrementAndGet() == artistIds.size()) {
                        storeDataInFirebase(topTracks, topArtists, topGenres, trackURIs);  // Proceed even if some requests fail
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String responseData = response.body().string();
                        JSONObject artistObject = new JSONObject(responseData);
                        JSONArray genres = artistObject.getJSONArray("genres");
                        if (genres.length() > 0) {
                            synchronized (topGenres) {
                                topGenres.add(genres.getString(0));
                            }
                        }

                        if (callsCompleted.incrementAndGet() == artistIds.size()) {
                            storeDataInFirebase(topTracks, topArtists, topGenres, trackURIs);
                        }
                    } catch (Exception e) {
                        Log.e("SpotifyAPI", "Error parsing artist details", e);
                    }
                }
            });
        }
    }

    private void storeDataInFirebase(List<String> topTracks, List<String> topArtists, List<String> topGenres, List<String> trackURIs) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference fetchHistoryRef = mDatabase.child("users").child(userId).child("fetchHistory").push();
            Map<String, Object> spotifyData = new HashMap<>();
            spotifyData.put("topTracks", topTracks);
            spotifyData.put("topArtists", topArtists);
            spotifyData.put("topGenres", topGenres);
            spotifyData.put("trackURIs", trackURIs);
            spotifyData.put("fetchDate", new SimpleDateFormat("yyyy.MM.dd.hh").format(new java.util.Date()));

            fetchHistoryRef.setValue(spotifyData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(HomePageActivity.this, "Fetch history saved successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomePageActivity.this, "Failed to save fetch history.", Toast.LENGTH_SHORT).show();
                }
            });
            mDatabase.child("users").child(userId).child("spotifyData").setValue(spotifyData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(HomePageActivity.this, "Spotify data saved successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(HomePageActivity.this, SpotifyWrappedActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(HomePageActivity.this, "Failed to save Spotify data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }
    }

}
