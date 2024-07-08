package com.example.finder;

public class ArchivedObject {
    private String description;
    private long timestamp;
    private String location;
    private String imageUrl;
    private String userId; // Add user ID to link each object to its uploader

    public ArchivedObject() {
        // Default constructor required for calls to DataSnapshot.getValue(ArchivedObject.class)
    }

    public ArchivedObject(String description, long timestamp, String location, String imageUrl, String userId) {
        this.description = description;
        this.timestamp = timestamp;
        this.location = location;
        this.imageUrl = imageUrl;
        this.userId = userId;
    }

    // Getters and setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
