package com.example.wrappedspotify;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SpotifyWrappedActivity extends AppCompatActivity {

    private ListView listViewData;
    private Button nextButton;
    private TextView categoryHeader;
    private DatabaseReference mDatabase;

    private List<String> topTracks;
    private List<String> topArtists;
    private List<String> topGenres;
    private List<String> topURIS;
    //COMMENT COMMIT TEST
    private int currentDisplay = 0;
    private static boolean running = false;


    private static TimerTask timerTask;
    private static MediaPlayer mediaPlayer;
    private static Timer timer;
    private static ImageView heart;

    private static ImageView rainbow;
    private static ImageView lantern;
    private static TextView changing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_wrapped);

        listViewData = findViewById(R.id.listViewData);
        nextButton = findViewById(R.id.nextButton);
        categoryHeader = findViewById(R.id.categoryHeader);
        heart = findViewById(R.id.chocolatePic);
        rainbow = findViewById(R.id.rainbowPic);
        lantern = findViewById(R.id.lanternPic);
        changing = findViewById(R.id.dynamic);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mediaPlayer = new MediaPlayer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                stopClip(mediaPlayer);
            }
        };
        timer = new Timer();

        if (user != null) {
            String userId = user.getUid();
            mDatabase.child("users").child(userId).child("spotifyData").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {
                        };
                        topTracks = dataSnapshot.child("topTracks").getValue(t);
                        topArtists = dataSnapshot.child("topArtists").getValue(t);
                        topGenres = dataSnapshot.child("topGenres").getValue(t);
                        topURIS = dataSnapshot.child("trackURIs").getValue(t);

                        updateDisplay();
                    } else {
                        Toast.makeText(SpotifyWrappedActivity.this, "No Spotify data found.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(SpotifyWrappedActivity.this, "Failed to fetch Spotify data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User is not signed in.", Toast.LENGTH_SHORT).show();
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDisplay = (currentDisplay + 1) % 3;
                updateDisplay();
            }
        });
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomePageActivity.radioButtonPressed = 0;
                Intent intent = new Intent(SpotifyWrappedActivity.this, HomePageActivity.class);
                startActivity(intent);
            }
        });

        @SuppressLint("WrongViewCast") ConstraintLayout constraintLayout = findViewById(R.id.mainSpotifyLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.start();

//        RadioGroup radioGroup = findViewById(R.id.radioGroup);
//
//        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                if (checkedId == R.id.radio1) {
//                    constraintLayout.setBackgroundColor(Color.rgb(1,50,32));
//                } else if (checkedId == R.id.radio2) {
//                    constraintLayout.setBackgroundColor(Color.RED);
//                } else if (checkedId == R.id.radio3) {
//                    constraintLayout.setBackgroundColor(Color.rgb(255, 160, 0));
//                }else if(checkedId == R.id.radio4){
//                    constraintLayout.setBackground(ContextCompat.getDrawable(SpotifyWrappedActivity.this, R.drawable.gradient_list)
//                    );
//                }
//            }
//        });
        if (HomePageActivity.radioButtonPressed == 0) {
            constraintLayout.setBackground(ContextCompat.getDrawable(SpotifyWrappedActivity.this, R.drawable.gradient_list));
            heart.setVisibility(View.INVISIBLE);
            rainbow.setVisibility(View.INVISIBLE);
            lantern.setVisibility(View.INVISIBLE);
        } else if (HomePageActivity.radioButtonPressed == 1) {
            constraintLayout.setBackground(ContextCompat.getDrawable(SpotifyWrappedActivity.this, R.drawable.patrick));
            categoryHeader.setTextColor(Color.RED);
            heart.setVisibility(View.INVISIBLE);
            lantern.setVisibility(View.INVISIBLE);
            changing.setTextColor(Color.RED);
            changing.setText(" Happy \n St. Patrick's Day!");
        } else if (HomePageActivity.radioButtonPressed == 2) {
            constraintLayout.setBackground(ContextCompat.getDrawable(SpotifyWrappedActivity.this, R.drawable.valentine));
            categoryHeader.setTextColor(Color.BLACK);
            rainbow.setVisibility(View.INVISIBLE);
            lantern.setVisibility(View.INVISIBLE);
            changing.setTextColor(Color.BLACK);
            changing.setText(" Happy \n Valentine's Day!");
        } else if (HomePageActivity.radioButtonPressed == 3) {
            constraintLayout.setBackground(ContextCompat.getDrawable(SpotifyWrappedActivity.this, R.drawable.halloween));
            heart.setVisibility(View.INVISIBLE);
            rainbow.setVisibility(View.INVISIBLE);
            categoryHeader.setTextColor(Color.parseColor("#000000"));
            changing.setTextColor(Color.BLACK);
            changing.setText("Happy Halloween!");
        }
    }

    private void updateDisplay() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.textViewItem) {

            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.textViewItem);
                textView.setText((position + 1) + ". " + getItem(position));
                if (HomePageActivity.radioButtonPressed == 0) {
                    textView.setTextColor(Color.parseColor("#FFCC00"));
                } else if (HomePageActivity.radioButtonPressed == 1) {
                    textView.setTextColor(Color.RED);
                } else if (HomePageActivity.radioButtonPressed == 2) {
                    textView.setTextColor(Color.BLACK);
                } else if (HomePageActivity.radioButtonPressed == 3) {
                    textView.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        switch (currentDisplay) {
            case 0:
                categoryHeader.setText("Top Tracks");
                adapter.addAll(topTracks);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);



                listViewData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDisplay == 0) {
                            timerTask.cancel();
                            stopClip(mediaPlayer);
                            playClip(topURIS.get(position), mediaPlayer, timer);
                        }
                    }
                });
                break;
            case 1:
                categoryHeader.setText("Top Artists");
                adapter.addAll(topArtists);
                break;
            case 2:
                categoryHeader.setText("Top Genres");
                adapter.addAll(topGenres);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + currentDisplay);
        }

        listViewData.setAdapter(adapter);
    }


    public static void playClip(String songMp3File, MediaPlayer mediaPlayer, Timer timer) {
        //MediaPlayer mediaPlayer = new MediaPlayer();
        //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    stopClip(mediaPlayer);
                }
            };

            mediaPlayer.setDataSource(songMp3File);
            // below line is use to prepare
            // and start our media player.
            mediaPlayer.prepare();
            mediaPlayer.start();
            //ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            //executorService.schedule(MainActivity::stopClip(mediaPlayer),10, TimeUnit.SECONDS);
            /*new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            stopClip(mediaPlayer);
                        }
                    },
                    10000
            );*/
            timer.schedule(timerTask, 10000);
            //System.out.println("TESTINGTESTING");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stopClip(MediaPlayer mediaPlayer) {
        if (mediaPlayer.isPlaying()) {
            //System.out.println("HERE WATCH");
            mediaPlayer.stop();
            mediaPlayer.reset();
            //mediaPlayer.release();
            //mediaPlayer.release();
            //mediaPlayer.reset();
        }
    }


}
