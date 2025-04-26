package com.ehunzango.unigo.models;

/**
 * Modelo de datos para el usuario de la aplicación
 */
public class User {
    private String uid;
    private String displayName;
    private String email;
    private String phoneNumber;
    private String photoUrl;
    private boolean isAnonymous;
    private String preferredTransportMode;

    // Constructor vacío requerido para Firebase
    public User() {
    }

    public User(String uid, String displayName, String email, String phoneNumber, 
               String photoUrl, boolean isAnonymous) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.photoUrl = photoUrl;
        this.isAnonymous = isAnonymous;
        this.preferredTransportMode = "walking"; // Valor por defecto
    }

    // Getters y setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public String getPreferredTransportMode() {
        return preferredTransportMode;
    }

    public void setPreferredTransportMode(String preferredTransportMode) {
        this.preferredTransportMode = preferredTransportMode;
    }
}