package com.example.finder;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

public class FirestoreManager {

    private FirebaseFirestore firestore;

    public FirestoreManager() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void storeImage(Bitmap imageBitmap) {
        // Store captured image to Firestore
        Map<String, Object> imageData = new HashMap<>();
        // Example fields, adjust as per your data model
        imageData.put("imageUrl", "example_image_url");
        imageData.put("description", "Example image description");

        firestore.collection("images")
                .add(imageData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FirestoreManager", "Image added with ID: " + documentReference.getId());
                    // Handle success, if needed
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error adding image", e);
                    // Handle failure, if needed
                });
    }

    public void fetchImages() {
        // Fetch images from Firestore, if needed
        firestore.collection("images")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("FirestoreManager", document.getId() + " => " + document.getData());
                            // Handle retrieved documents
                        }
                    } else {
                        Log.e("FirestoreManager", "Error getting documents: ", task.getException());
                    }
                });
    }
}
