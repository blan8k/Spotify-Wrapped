package com.example.wrappedspotify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class SpotifyWrappedFragment extends Fragment {

    private TextView textViewTopTracks;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spotify_wrapped, container, false);
        textViewTopTracks = view.findViewById(R.id.textViewTopTracks);

        Bundle args = getArguments();
        if (args != null) {
            List<String> topTracks = args.getStringArrayList("topTracks");
            if (topTracks != null) {
                StringBuilder sb = new StringBuilder();
                for (String track : topTracks) {
                    sb.append(track).append("\n");
                }
                textViewTopTracks.setText(sb.toString());
            }
        }
        return view;
    }
}
