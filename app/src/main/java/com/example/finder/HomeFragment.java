package com.example.finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import android.widget.ImageButton;
import android.widget.ImageView;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ImageButton settingsButton = view.findViewById(R.id.settingsButton);
        ImageView bannerImageView = view.findViewById(R.id.bannerImageView);

        // Load the image using Glide
        Glide.with(this)
                .load("https://cdn.usegalileo.ai/stability/d93c4d79-5a90-44fd-9987-01703881c325.png")
                .into(bannerImageView);

        settingsButton.setOnClickListener(v -> {
            // Handle settings button click
        });

        view.findViewById(R.id.start_searching_button).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchItemActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.report_lost_item_button).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ReportItemActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
