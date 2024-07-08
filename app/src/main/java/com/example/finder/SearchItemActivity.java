package com.example.finder;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

public class SearchItemActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageButton backButton;
    private ImageView selectedImageView;
    private Button selectImageButton;
    private Button searchButton;
    private ProgressBar loadingSpinner;
    private Uri selectedImageUri;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        // Initialize OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        backButton = findViewById(R.id.backButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        searchButton = findViewById(R.id.searchButton);
        loadingSpinner = findViewById(R.id.loadingSpinner);

        // Set onClick listeners
        backButton.setOnClickListener(view -> onBackPressed());
        selectImageButton.setOnClickListener(view -> openGallery());
        searchButton.setOnClickListener(view -> searchItem());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                Glide.with(this).load(bitmap).into(selectedImageView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void searchItem() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            matchImage(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void matchImage(Bitmap bitmap) {
        loadingSpinner.setVisibility(View.VISIBLE);

        // Convert the bitmap to a Mat object
        Mat imgMat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, imgMat);
        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGBA2GRAY);

        // Extract features using ORB
        ORB orb = ORB.create();
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();
        orb.detectAndCompute(imgMat, new Mat(), keypoints, descriptors);

        // Check if descriptors is empty
        if (descriptors.empty()) {
            loadingSpinner.setVisibility(View.GONE);
            Toast.makeText(this, "No features detected in the selected image.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve stored images from Firestore
        firestore.collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            boolean matchFound = false;
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                // Get the stored image features
                                List<Double> storedDescriptorsList = (List<Double>) document.get("descriptors");
                                if (storedDescriptorsList != null) {
                                    // Convert the stored descriptors to a Mat object
                                    Mat storedDescriptorsMat = new Mat(storedDescriptorsList.size() / descriptors.cols(), descriptors.cols(), CvType.CV_32F);
                                    for (int i = 0; i < storedDescriptorsList.size(); i++) {
                                        storedDescriptorsMat.put(i / descriptors.cols(), i % descriptors.cols(), storedDescriptorsList.get(i));
                                    }

                                    // Match features using BFMatcher
                                    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                                    MatOfDMatch matches = new MatOfDMatch();
                                    matcher.match(descriptors, storedDescriptorsMat, matches);

                                    // Check if a match is found
                                    double maxDist = 0;
                                    double minDist = 100;

                                    // Calculate min and max distances between keypoints
                                    for (int i = 0; i < matches.rows(); i++) {
                                        double dist = matches.get(i, 0)[0];
                                        if (dist < minDist) minDist = dist;
                                        if (dist > maxDist) maxDist = dist;
                                    }

                                    // Consider matches if the distance is less than 2 * minDist
                                    for (int i = 0; i < matches.rows(); i++) {
                                        if (matches.get(i, 0)[0] < 2 * minDist) {
                                            matchFound = true;
                                            break;
                                        }
                                    }

                                    if (matchFound) break;
                                }
                            }
                            if (matchFound) {
                                Toast.makeText(this, "Potential match found!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "No match found.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error retrieving images from Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
