package com.example.finder;

import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    public DatabaseHelper() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    // Method to save archived object
    public void saveArchivedObject(ArchivedObject archivedObject, Uri imageUri, String userId) {
        if (imageUri != null) {
            StorageReference imageRef = storageRef.child("images/" + imageUri.getLastPathSegment());
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    archivedObject.setImageUrl(uri.toString());
                                    archivedObject.setUserId(userId);
                                    saveToFirestore(archivedObject);
                                });
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure (optional)
                    });
        } else {
            archivedObject.setUserId(userId);
            saveToFirestore(archivedObject);
        }
    }

    // Save to Firestore
    private void saveToFirestore(ArchivedObject archivedObject) {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("description", archivedObject.getDescription());
        objectMap.put("timestamp", archivedObject.getTimestamp());
        objectMap.put("location", archivedObject.getLocation());
        objectMap.put("imageUrl", archivedObject.getImageUrl());
        objectMap.put("userId", archivedObject.getUserId());

        db.collection("archived_objects")
                .add(objectMap)
                .addOnSuccessListener(documentReference -> {
                    // Handle successful addition
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }

    // Method to create collections if they don't exist
    public void createCollectionsIfNotExist() {
        createCollectionIfNotExist("archived_objects");
        createCollectionIfNotExist("item_images");
    }

    private void createCollectionIfNotExist(String collectionName) {
        db.collection(collectionName).document("dummy_document")
                .set(new HashMap<>(), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Collection exists or was successfully created
                    System.out.println(collectionName + " collection exists or was created successfully.");
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    System.out.println("Failed to create " + collectionName + " collection: " + e.getMessage());
                });
    }
}
