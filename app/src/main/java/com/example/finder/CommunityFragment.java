package com.example.finder;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CommunityFragment extends Fragment {

    private static final int MAX_DESCRIPTION_LENGTH = 120;

    private Button selectImageButton;
    private EditText descriptionEditText;
    private ImageView selectedImageView;
    private MaterialButton instagramButton;
    private MaterialButton twitterButton;
    private Uri selectedImageUri; // Store the URI of the selected image

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        selectImageButton = view.findViewById(R.id.selectImageButton);
        selectedImageView = view.findViewById(R.id.selectedImageView);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        instagramButton = view.findViewById(R.id.instagram_button);
        twitterButton = view.findViewById(R.id.twitter_button);

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedImageUri = data.getData();
                            selectedImageView.setImageURI(selectedImageUri);
                        }
                    }
                });
        // Select Image button click listener
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
        // Description input validation
        descriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this implementation, but required to override
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Editable editableText = descriptionEditText.getText(); // Get the Editable
                int currentLength = editableText.length();
                if (currentLength > MAX_DESCRIPTION_LENGTH) {
                    editableText.delete(MAX_DESCRIPTION_LENGTH, currentLength); // Use Editable's delete method
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                // Not needed for this implementation, but required to override
            }
        });
        // Share to Instagram
        instagramButton.setOnClickListener(v -> shareToInstagram(selectedImageUri, descriptionEditText.getText().toString()));
        // Share to X (formerly Twitter)
        twitterButton.setOnClickListener(v -> shareToX(selectedImageUri, descriptionEditText.getText().toString()));
        return view;
    }

    // Function to share to Instagram
    private void shareToInstagram(Uri imageUri, String description) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT, description);
            intent.setPackage("com.instagram.android"); // Explicit intent for Instagram
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Instagram app not found", Toast.LENGTH_SHORT).show();
        }
    }
    // Function to share to X
    private void shareToX(Uri imageUri, String description) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);

            // Try sharing as an image first
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT, description);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant read permission

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // If X can't handle the image directly, fall back to sharing as a link
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, description + " " + imageUri.toString());
                intent.putExtra("android.intent.extra.TEXT", description + " " + imageUri.toString());

                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // If no suitable app is found, open in browser
                    String tweetUrl = "https://twitter.com/intent/tweet?text=" + Uri.encode(description) + "&url=" + Uri.encode(imageUri.toString());
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
                    startActivity(browserIntent);
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error sharing to X", Toast.LENGTH_SHORT).show();
        }
    }
}
