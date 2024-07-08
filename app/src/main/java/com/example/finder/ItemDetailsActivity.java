package com.example.finder;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemDetailsActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    private Uri selectedImageUri;

    private ImageButton backButton;
    private Button selectImageButton;
    private Button submitButton;
    private ImageView selectedImageView;
    private EditText descriptionEditText;
    private EditText dateEditText;
    private Spinner categorySpinner;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Location lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        // Initialize OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        backButton = findViewById(R.id.backButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        submitButton = findViewById(R.id.submitButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        dateEditText = findViewById(R.id.dateEditText);
        categorySpinner = findViewById(R.id.categorySpinner);

        // Setup spinner with categories from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.item_categories,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Set onClick listeners
        backButton.setOnClickListener(view -> finish());
        selectImageButton.setOnClickListener(view -> openGallery());
        submitButton.setOnClickListener(view -> submitItemDetails());

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

    private void submitItemDetails() {
        String description = descriptionEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString().trim();

        // Validate date format (assuming yyyy-MM-dd format)
        if (!isValidDate(date)) {
            Toast.makeText(this, "Date should be in yyyy-MM-dd format.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty() || date.isEmpty() || category.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "All fields are required, including an image.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to submit an item.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Request location permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Extract features from the selected image
        extractImageFeatures(description, date, category, currentUser.getUid());
    }

    private boolean isValidDate(String date) {
        // Simple validation for yyyy-MM-dd format
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private void extractImageFeatures(String description, String date, String category, String userId) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);

            // Convert the bitmap to a Mat object
            Mat imgMat = new Mat();
            org.opencv.android.Utils.bitmapToMat(bitmap, imgMat);
            Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGBA2GRAY);

            // Extract features using ORB
            ORB orb = ORB.create();
            MatOfKeyPoint keypoints = new MatOfKeyPoint();
            Mat descriptors = new Mat();
            orb.detectAndCompute(imgMat, new Mat(), keypoints, descriptors);

            // Convert descriptors to a list of doubles
            List<Double> descriptorList = new ArrayList<>();
            for (int i = 0; i < descriptors.rows(); i++) {
                for (int j = 0; j < descriptors.cols(); j++) {
                    descriptorList.add(descriptors.get(i, j)[0]);
                }
            }

            uploadImageAndSubmit(description, date, category, userId, descriptorList);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to extract image features: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageAndSubmit(String description, String date, String category, String userId, List<Double> descriptorList) {
        // Check last known location
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Could not determine your location. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference storageRef = storage.getReference().child("item_images/" + System.currentTimeMillis() + ".jpg");
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();

                        // Create item details map
                        Map<String, Object> itemDetails = new HashMap<>();
                        itemDetails.put("description", description);
                        itemDetails.put("date", date);
                        itemDetails.put("category", category);
                        itemDetails.put("imageUrl", imageUrl);
                        itemDetails.put("userId", userId);
                        itemDetails.put("descriptors", descriptorList); // Add descriptors
                        itemDetails.put("timestamp", FieldValue.serverTimestamp()); // Add timestamp
                        itemDetails.put("latitude", lastKnownLocation.getLatitude()); // Add latitude
                        itemDetails.put("longitude", lastKnownLocation.getLongitude()); // Add longitude

                        // Add item details to Firestore under "items" collection
                        firestore.collection("items").add(itemDetails)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(ItemDetailsActivity.this, "Item submitted successfully!", Toast.LENGTH_SHORT).show();
                                    finish(); // Close activity after submission
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ItemDetailsActivity.this, "Failed to submit item details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error submitting item details", e);
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ItemDetailsActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error uploading image", e);
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start location updates
        startLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop location updates
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000); // 5 seconds

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                lastKnownLocation = locationResult.getLastLocation();
            }
        }, null);
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
