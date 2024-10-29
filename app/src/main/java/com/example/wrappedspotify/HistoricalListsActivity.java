package com.example.wrappedspotify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoricalListsActivity extends AppCompatActivity {

    private ListView listViewHistory;

    private DatabaseReference mDatabase;




    private List<String> fetchHistoryList = new ArrayList<>();

    private List<String> keys = new ArrayList<>();


    private SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_lists);

        listViewHistory = findViewById(R.id.listViewHistory);
        loadFetchHistory();
        mDatabase = FirebaseDatabase.getInstance().getReference();


        Button homeButton = findViewById(R.id.buttonHome);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HistoricalListsActivity.this, HomePageActivity.class);
            startActivity(intent);
        });
    }

    private void loadFetchHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference fetchHistoryRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId).child("fetchHistory");

            fetchHistoryRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    fetchHistoryList.clear();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String fetchDate = snapshot.child("fetchDate").getValue(String.class);
                            keys.add(snapshot.child("key").getValue(String.class));
                            Log.e("hi","tell"+keys);

                            if (fetchDate != null) {
                                fetchHistoryList.add(fetchDate);
                            }
                        }
                        updateListView();
                    } else {
                        Toast.makeText(HistoricalListsActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(HistoricalListsActivity.this, "Failed to load history: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchDataForDateAndNavigate(String selectedDate) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference dateRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId).child("fetchHistory");
            Query query = dateRef.orderByChild("fetchDate").equalTo(selectedDate);

                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<String> topTracks = new ArrayList<>();
                        List<String> topArtists = new ArrayList<>();
                        List<String> topGenres = new ArrayList<>();
                        List<String> trackURIs = new ArrayList<>();
                        int current = 0;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if(current==0) {

                                DataSnapshot tracksSnapshot = snapshot.child("topTracks");
                                for (DataSnapshot track : tracksSnapshot.getChildren()) {
                                    topTracks.add(track.getValue(String.class));
                                }

                                DataSnapshot artistsSnapshot = snapshot.child("topArtists");
                                for (DataSnapshot artist : artistsSnapshot.getChildren()) {
                                    topArtists.add(artist.getValue(String.class));
                                }

                                DataSnapshot genresSnapshot = snapshot.child("topGenres");
                                for (DataSnapshot genre : genresSnapshot.getChildren()) {
                                    topGenres.add(genre.getValue(String.class));
                                }

                                DataSnapshot urisSnapshot = snapshot.child("trackURIs");
                                for (DataSnapshot uri : urisSnapshot.getChildren()) {
                                    trackURIs.add(uri.getValue(String.class));
                                }
                            }
                            else{
                                break;
                            }
                            current+=1;
                        }

                        Map<String, Object> spotifyData = new HashMap<>();
                        spotifyData.put("topTracks", topTracks);
                        spotifyData.put("topArtists", topArtists);
                        spotifyData.put("topGenres", topGenres);
                        spotifyData.put("trackURIs", trackURIs);
                        spotifyData.put("fetchDate", selectedDate);

                        Log.e("Tracks","tracks"+topTracks);
                        Log.e("Tracks","tracks"+topArtists);
                        Log.e("Tracks","tracks"+topGenres);
                        Log.e("Tracks","tracks"+trackURIs);





                        Intent intent = new Intent(HistoricalListsActivity.this, SpotifyWrappedActivity.class);
                        mDatabase.child("users").child(userId).child("spotifyData").setValue(spotifyData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(HistoricalListsActivity.this, "Failed to save Spotify data.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(HistoricalListsActivity.this, "No data found for this date.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(HistoricalListsActivity.this, "Failed to fetch data for the selected date.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.textViewItem, fetchHistoryList);
        listViewHistory.setAdapter(adapter);

        listViewHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedDate = fetchHistoryList.get(position);
                fetchDataForDateAndNavigate(selectedDate);
            }
        });
    }



}
