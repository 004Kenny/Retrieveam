package com.example.finder;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.opencv.android.OpenCVLoader;
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
        if (OpenCVLoader.initDebug()) {
            Log.i("OpenCV", "OpenCV loaded successfully");
        } else {
            Log.e("OpenCV", "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
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

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Set onClick listeners
        backButton.setOnClickListener(view -> onBackPressed());
        selectImageButton.setOnClickListener(view -> openGallery());
        submitButton.setOnClickListener(view -> submitItemDetails());

        // Location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    lastKnownLocation = location;
                }
            }
        };

        // Request location permission
        requestLocationPermission();
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
        String category = categorySpinner.getSelectedItem().toString();

        if (description.isEmpty() || date.isEmpty() || category.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Please fill all fields and select an image.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Convert the bitmap to a Mat object
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap != null) {
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
                    descriptorList.add((double) descriptors.get(i, j)[0]);
                }
            }

            // Upload image to Firebase Storage
            StorageReference storageRef = storage.getReference().child("images/" + selectedImageUri.getLastPathSegment());
            storageRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Save item details to Firestore
                Map<String, Object> item = new HashMap<>();
                item.put("description", description);
                item.put("date", date);
                item.put("category", category);
                item.put("imageUrl", uri.toString());
                item.put("userId", userId);
                item.put("timestamp", FieldValue.serverTimestamp());
                item.put("descriptors", descriptorList);
                if (lastKnownLocation != null) {
                    List<Double> locationList = new ArrayList<>();
                    locationList.add(lastKnownLocation.getLatitude());
                    locationList.add(lastKnownLocation.getLongitude());
                    item.put("location", locationList);
                }

                firestore.collection("items")
                        .add(item)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(ItemDetailsActivity.this, "Item submitted successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ItemDetailsActivity.this, "Error submitting item.", Toast.LENGTH_SHORT).show());
            })).addOnFailureListener(e -> Toast.makeText(ItemDetailsActivity.this, "Error uploading image.", Toast.LENGTH_SHORT).show());
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}
