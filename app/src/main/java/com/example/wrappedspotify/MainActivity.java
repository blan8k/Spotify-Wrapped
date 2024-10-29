package com.example.wrappedspotify;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String REDIRECT_URI = "com.example.wrappedspotify://auth";
    private static final String CLIENT_ID = "045795b20f1745d5960bbbef296de6a1";

    public static final int AUTH_TOKEN_REQUEST_CODE = 0;
    public static final int AUTH_CODE_REQUEST_CODE = 1;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mAccessToken, mAccessCode;
    private Call mCall;

    Button spotifyWrappedBtn;
    private TextView tokenTextView, codeTextView, profileTextView, topArtistTextView,
            topSongTextView, topGenreTextView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tokenTextView = (TextView) findViewById(R.id.token_text_view);
        codeTextView = (TextView) findViewById(R.id.code_text_view);
        profileTextView = (TextView) findViewById(R.id.response_text_view);
        topArtistTextView = (TextView) findViewById(R.id.topArtistsView);
        topSongTextView = (TextView) findViewById(R.id.topSongsTV);
        topGenreTextView = (TextView) findViewById(R.id.genresTV);
        Button tokenBtn = (Button) findViewById(R.id.token_btn);
        Button codeBtn = (Button) findViewById(R.id.code_btn);
        Button profileBtn = (Button) findViewById(R.id.profile_btn);
        Button topArtistsBtn = (Button) findViewById(R.id.topArtists);
        Button topSongsBtn = (Button) findViewById(R.id.topSongsBtn);
        Button topGenreBtn = (Button) findViewById(R.id.genresBtn);
        spotifyWrappedBtn = findViewById(R.id.spotify_wrapped);

        fetchSpotifyAccessToken();


        spotifyWrappedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyWrappedBtn.setVisibility(View.GONE);
                Fragment fragment = new SpotifyWrappedFragment();
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.activity_main, fragment).commit();

            }
        });
        tokenBtn.setOnClickListener((v) -> {
            getToken();
        });

        codeBtn.setOnClickListener((v) -> {
            getCode();
        });

        profileBtn.setOnClickListener((v) -> {
            onGetUserProfileClicked();
        });
        topArtistsBtn.setOnClickListener((v)-> getTopArtists());
        topSongsBtn.setOnClickListener(v -> getTopSongs());
        topGenreBtn.setOnClickListener(v -> getTopGenres());

    }

    /**
     * Get token from Spotify
     * This method will open the Spotify login activity and get the token
     * What is token?
     * https://developer.spotify.com/documentation/general/guides/authorization-guide/
     */
    public void getToken() {
        final AuthorizationRequest request = getAuthenticationRequest(AuthorizationResponse.Type.TOKEN);
        AuthorizationClient.openLoginActivity(MainActivity.this, AUTH_TOKEN_REQUEST_CODE, request);
    }

    /**
     * Get code from Spotify
     * This method will open the Spotify login activity and get the code
     * What is code?
     * https://developer.spotify.com/documentation/general/guides/authorization-guide/
     */
    public void getCode() {
        final AuthorizationRequest request = getAuthenticationRequest(AuthorizationResponse.Type.CODE);
        AuthorizationClient.openLoginActivity(MainActivity.this, AUTH_CODE_REQUEST_CODE, request);
    }


    /**
     * When the app leaves this activity to momentarily get a token/code, this function
     * fetches the result of that external activity to get the response from Spotify
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);

        // Check which request code is present (if any)
        if (AUTH_TOKEN_REQUEST_CODE == requestCode) {
            mAccessToken = response.getAccessToken();

        } else if (AUTH_CODE_REQUEST_CODE == requestCode) {
            mAccessCode = response.getCode();
        }
    }

    private void fetchSpotifyAccessToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.child("spotifyToken").addListenerForSingleValueEvent(new ValueEventListener() {

                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        mAccessToken = dataSnapshot.getValue(String.class);
                        tokenTextView.setText(mAccessToken);
                    } else {
                        Toast.makeText(MainActivity.this, "Access token not found in database.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Failed to fetch access token.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get user profile
     * This method will get the user profile using the token
     */
    public void onGetUserProfileClicked() {
        if (mAccessToken == null) {
            Toast.makeText(this, "You need to get an access token first!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me")
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
                    setTextAsync(jsonObject.toString(3), profileTextView);
                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void getTopArtists() {
        if (mAccessToken == null) {
            Toast.makeText(this, "You need to get an access token first!", Toast.LENGTH_SHORT).show();
            return;
        }
        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/artists")
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
                    String artist = jsonObject.toString();
                    String[] topArtists = new String[5];
                    String one = artist.substring(artist.indexOf("\"name\":")  + 8,
                            artist.indexOf("\",\"popularity\":"));
                    topArtists[0] = one;

                    int i = artist.indexOf("\"name\":") + 1;
                    int j = artist.indexOf("\",\"popularity\":") + 1;
                    for(int index = 1; index < 5; index++){
                        topArtists[index] = artist.substring(artist.indexOf("\"name\"", i) + 8,
                                artist.indexOf("\",\"popularity\":", j));
                        i = artist.indexOf("\"name\"", i) + 1;
                        j = artist.indexOf("\",\"popularity\":", j) + 1;
                    }
                    String list = "Top artists: " + topArtists[0] + ", " + topArtists[1] + ", "
                            + topArtists[2] + ", " + topArtists[3] + ", " + topArtists[4];
                    setTextAsync(list, topArtistTextView);
                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    public void getTopSongs() {
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
                    String[] topSongs = new String[5];
                    String one = songs.substring(songs.indexOf("\"name\":")  + 8,
                            songs.indexOf("\",\"popularity\":"));
                    int i = songs.indexOf(",\"name\":") + 1;
                    int j = 0;
                    int type = songs.indexOf("\"type\":") + 1;
                    for(int index = 0; index < 5; index++){
                        if(songs.indexOf("\",\"popularity\":", j) <
                                songs.indexOf("\"type\":", type)){
                            topSongs[index] = songs.substring(songs.indexOf(",\"name\":", i) + 9,
                                    songs.indexOf("\",\"popularity\":", j));
                            j = songs.indexOf("\",\"popularity\":", j) + 1;
                        }else{
                            index--;
                        }
                        /*topSongs[index] = songs.substring(songs.indexOf("\"name\"", i) + 8,
                                songs.indexOf("\",\"popularity\":", j));
                        i = songs.indexOf(",\"name\"", i) + 1;
                        j = songs.indexOf("\",\"popularity\":", j) + 1;*/
                        i = songs.indexOf(",\"name\":", i) + 1;
                        type = songs.indexOf("\"type\":", type) + 1;
                    }
                    String list = "Top songs: " + topSongs[0] + ", " + topSongs[1] + ", "
                            + topSongs[2] + ", " + topSongs[3] + ", " + topSongs[4];
                    setTextAsync(list, topSongTextView);
                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void getTopGenres(){
        if (mAccessToken == null) {
            Toast.makeText(this, "You need to get an access token first!", Toast.LENGTH_SHORT).show();
            return;
        }
        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/top/artists")
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
                    String artist = jsonObject.toString();

                    ArrayList<String> topGenres = new ArrayList();
                    ArrayList<String> genres = new ArrayList<>();

                    String one = artist.substring(artist.indexOf("genres") + 11,
                            artist.indexOf("\"],"));
                    int i = 0;
                    int j = 0;
                    for(int index = 0; index < 5; index++){
                        String[] each =  artist.substring(artist.indexOf("genres", i) + 9,
                                artist.indexOf("\"],", j) + 1).split(",");
                        for(int k = 0; k < each.length; k++){
                            if(!topGenres.contains(each[k])){
                                topGenres.add(each[k]);
                            }
                        }
                        i = artist.indexOf("genres", i) + 1;
                        j = artist.indexOf("\"],", j) + 1;;
                    }
                    String list = "Top Genres:";
                    for(int z = 0; z < topGenres.size(); z++){
                        list = list + " " + topGenres.get(z).substring(1,topGenres.get(z).length() - 1) + ",";
                    }
                    setTextAsync(list.substring(0,list.length() - 1), topGenreTextView);
                } catch (JSONException e) {
                    Log.d("JSON", "Failed to parse data: " + e);
                    Toast.makeText(MainActivity.this, "Failed to parse data, watch Logcat for more details",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    /**
     * Creates a UI thread to update a TextView in the background
     * Reduces UI latency and makes the system perform more consistently
     *
     * @param text the text to set
     * @param textView TextView object to update
     */
    private void setTextAsync(final String text, TextView textView) {
        runOnUiThread(() -> textView.setText(text));
    }

    /**
     * Get authentication request
     *
     * @param type the type of the request
     * @return the authentication request
     */
    private AuthorizationRequest getAuthenticationRequest(AuthorizationResponse.Type type) {
        return new AuthorizationRequest.Builder(CLIENT_ID, type, getRedirectUri().toString())
                .setShowDialog(false)
                .setScopes(new String[] { "user-read-email", "user-top-read"    }) // <--- Change the scope of your requested token here
                .setCampaign("your-campaign-token")
                .build();
    }

    /**
     * Gets the redirect Uri for Spotify
     *
     * @return redirect Uri object
     */
    private Uri getRedirectUri() {
        return Uri.parse(REDIRECT_URI);
    }

    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCall();
        super.onDestroy();
    }


}