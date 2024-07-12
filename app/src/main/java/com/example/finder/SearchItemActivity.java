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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
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
    private String targetDocumentId = "jk848ivSiDNEbawRlUDJ"; // Change this to the target document's ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
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

        // Proceed to compare with the target document
        compareDescriptorsWithTarget(descriptors);
    }

    private void compareDescriptorsWithTarget(Mat descriptors) {
        firestore.collection("items")
                .document(targetDocumentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Double> storedDescriptorList = (List<Double>) documentSnapshot.get("descriptors");

                    if (storedDescriptorList != null && !storedDescriptorList.isEmpty()) {
                        // Convert stored descriptors back to Mat
                        Mat storedDescriptors = new Mat(storedDescriptorList.size() / descriptors.cols(), descriptors.cols(), CvType.CV_8U);
                        int index = 0;
                        for (int i = 0; i < storedDescriptors.rows(); i++) {
                            for (int j = 0; j < storedDescriptors.cols(); j++) {
                                storedDescriptors.put(i, j, storedDescriptorList.get(index++).byteValue());
                            }
                        }

                        // Perform matching
                        MatOfDMatch matches = new MatOfDMatch();
                        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                        matcher.match(descriptors, storedDescriptors, matches);

                        // Check if a match is found
                        double minDist = Double.MAX_VALUE;

                        // Calculate min distance between keypoints
                        for (int i = 0; i < matches.rows(); i++) {
                            double dist = matches.get(i, 0)[0];
                            if (dist < minDist) minDist = dist;
                        }

                        // Set your custom threshold
                        double distanceThreshold = 0.000000000001; // Adjust this threshold as needed

                        // Consider matches if the distance is less than the threshold
                        boolean matchFound = (minDist > distanceThreshold);

                        loadingSpinner.setVisibility(View.GONE);
                        if (matchFound) {
                            runOnUiThread(() -> Toast.makeText(SearchItemActivity.this, "Potential match found!", Toast.LENGTH_SHORT).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(SearchItemActivity.this, "No match found.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        loadingSpinner.setVisibility(View.GONE);
                        runOnUiThread(() -> Toast.makeText(SearchItemActivity.this, "Error retrieving descriptors from target document", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Log.e(TAG, "Error retrieving descriptors from target document", e);
                    runOnUiThread(() -> Toast.makeText(SearchItemActivity.this, "Error retrieving descriptors", Toast.LENGTH_SHORT).show());
                });
    }


    // Helper class for Firestore document
    public static class Item {
        public List<Double> descriptors;

        public Item(List<Double> descriptors) {
            this.descriptors = descriptors;
        }
    }
}
