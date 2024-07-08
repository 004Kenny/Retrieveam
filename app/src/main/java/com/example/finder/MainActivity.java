package com.example.finder;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        ImageView bannerImageView = findViewById(R.id.bannerImageView);

        // Load the image using Glide
        Glide.with(this)
                .load("https://cdn.usegalileo.ai/stability/d93c4d79-5a90-44fd-9987-01703881c325.png")
                .into(bannerImageView);

        settingsButton.setOnClickListener(v -> {
            // Handle settings button click
        });


        findViewById(R.id.start_searching_button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchItemActivity.class);
            startActivity(intent);
        });


        findViewById(R.id.report_lost_item_button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportItemActivity.class);
            startActivity(intent);
        });


    }
}
