
package com.example.wrappedspotify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;


public class LinkSpotifyActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "com.example.wrappedspotify://auth";
    private static final String CLIENT_ID = "045795b20f1745d5960bbbef296de6a1";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Button buttonLinkSpotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_spotify);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        buttonLinkSpotify = findViewById(R.id.buttonLinkSpotify);

        buttonLinkSpotify.setOnClickListener(view -> authenticateSpotify());
    }

    private void authenticateSpotify() {
        final AuthorizationRequest request =  new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setShowDialog(false)
                .setScopes(new String[] { "user-read-email", "user-top-read"    })
                .setCampaign("your-campaign-token")
                .setShowDialog(true)
                .build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                case TOKEN:
                    storeSpotifyToken(response.getAccessToken());
                    break;
                case ERROR:
                    Log.e("LinkSpotifyActivity", "Auth error: " + response.getError());
                    Toast.makeText(getApplicationContext(), "Auth error: " + response.getError(), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e("LinkSpotifyActivity", "Auth canceled");
                    Toast.makeText(getApplicationContext(), "Auth canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void storeSpotifyToken(String token) {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(LinkSpotifyActivity.this, "Firebase Auth user is null. Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child(userId).child("spotifyToken").setValue(token)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LinkSpotifyActivity.this, "Spotify account linked successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LinkSpotifyActivity.this, HomePageActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LinkSpotifyActivity.this, "Failed to link Spotify account.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}