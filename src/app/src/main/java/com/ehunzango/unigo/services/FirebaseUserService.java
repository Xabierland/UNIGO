package com.ehunzango.unigo.services;

import androidx.annotation.NonNull;

import com.ehunzango.unigo.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Servicio para gestionar los usuarios en la base de datos de Firebase
 */
public class FirebaseUserService {

    private final DatabaseReference mDatabase;
    private static FirebaseUserService instance;

    // Interfaz para callbacks de operaciones de usuario
    public interface UserCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    // Constructor privado para Singleton
    private FirebaseUserService() {
        mDatabase = FirebaseDatabase.getInstance().getReference("users");
    }

    // Método para obtener la instancia única
    public static FirebaseUserService getInstance() {
        if (instance == null) {
            instance = new FirebaseUserService();
        }
        return instance;
    }

    // Crear o actualizar usuario en la base de datos
    public void saveUser(FirebaseUser firebaseUser, UserCallback callback) {
        if (firebaseUser == null) {
            callback.onError("Usuario no válido");
            return;
        }

        // Crear objeto usuario a partir del FirebaseUser
        User user = new User(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                firebaseUser.getPhoneNumber(),
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null,
                firebaseUser.isAnonymous()
        );

        // Guardar en la base de datos
        mDatabase.child(user.getUid()).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Error al guardar usuario: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                    }
                });
    }

    // Obtener usuario por ID
    public void getUserById(String userId, UserCallback callback) {
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onError("Usuario no encontrado");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError("Error al obtener usuario: " + databaseError.getMessage());
            }
        });
    }

    // Obtener usuario actual
    public void getCurrentUser(UserCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            getUserById(currentUser.getUid(), callback);
        } else {
            callback.onError("No hay usuario logueado");
        }
    }

    // Actualizar preferencia de modo de transporte
    public void updatePreferredTransportMode(String userId, String transportMode, OnCompleteListener<Void> listener) {
        mDatabase.child(userId).child("preferredTransportMode").setValue(transportMode)
                .addOnCompleteListener(listener);
    }

    // Actualizar nombre de usuario
    public void updateDisplayName(String userId, String displayName, OnCompleteListener<Void> listener) {
        mDatabase.child(userId).child("displayName").setValue(displayName)
                .addOnCompleteListener(listener);
    }

    // Actualizar URL de foto de perfil
    public void updatePhotoUrl(String userId, String photoUrl, OnCompleteListener<Void> listener) {
        mDatabase.child(userId).child("photoUrl").setValue(photoUrl)
                .addOnCompleteListener(listener);
    }

    // Eliminar usuario
    public void deleteUser(String userId, OnCompleteListener<Void> listener) {
        mDatabase.child(userId).removeValue()
                .addOnCompleteListener(listener);
    }
}