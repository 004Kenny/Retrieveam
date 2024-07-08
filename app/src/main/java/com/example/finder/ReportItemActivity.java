package com.example.finder;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.finder.PhotoAdapter;
import com.example.finder.Photo;

import java.util.ArrayList;

public class ReportItemActivity extends AppCompatActivity {

    private ImageButton backButton;
    private Button takePhotosButton;
    private Button uploadPhotosButton;

    private RecyclerView photoRecyclerView;
    private PhotoAdapter photoAdapter;
    private ArrayList<Photo> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_item);

        // Initialize views
        backButton = findViewById(R.id.backButton);
        takePhotosButton = findViewById(R.id.takePhotosButton);
        uploadPhotosButton = findViewById(R.id.uploadPhotosButton);

        // Initialize RecyclerView and LayoutManager
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(photoList);
        photoRecyclerView.setAdapter(photoAdapter);
        photoRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Add sample photos (replace with your logic to add photos dynamically)
        photoList.add(new Photo("https://cdn.usegalileo.ai/stability/83e9a630-a52d-4f8d-9b03-5b5647444ce9.png"));
        photoList.add(new Photo("https://cdn.usegalileo.ai/stability/0d40ddeb-5c0f-43b3-a8ae-7a1a3e8813ca.png"));
        photoList.add(new Photo("https://cdn.usegalileo.ai/stability/e8abd3ec-db5e-4548-8821-4bfd911b88fc.png"));
        photoList.add(new Photo("https://cdn.usegalileo.ai/stability/c3438cf9-3cd3-46d5-94b1-ae889e679092.png"));

        // Notify adapter of changes
        photoAdapter.notifyDataSetChanged();

        // Set click listener for uploadPhotosButton
        uploadPhotosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReportItemActivity.this, ItemDetailsActivity.class);
                startActivity(intent);
            }
        });
    }
}
