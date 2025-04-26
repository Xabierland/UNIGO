package com.ehunzango.unigo.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar la autenticación con Firebase
 */
public class FirebaseAuthService {

    private final FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static FirebaseAuthService instance;

    // Interfaz para callbacks de autenticación
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    // Constructor privado para Singleton
    private FirebaseAuthService() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Método para obtener la instancia única
    public static FirebaseAuthService getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthService();
        }
        return instance;
    }

    // Inicializar cliente de Google Sign In
    public void initGoogleSignIn(Context context, String webClientId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    // Obtener intent para iniciar el flujo de inicio de sesión con Google
    public Intent getGoogleSignInIntent() {
        return mGoogleSignInClient.getSignInIntent();
    }

    // Procesar resultado de inicio de sesión con Google
    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask, AuthCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Autenticar con Firebase usando las credenciales de Google
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            callback.onSuccess(task.getResult().getUser());
                        } else {
                            callback.onError("Error al autenticar con Google: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                        }
                    });
        } catch (ApiException e) {
            callback.onError("Error en inicio de sesión con Google: " + e.getMessage());
        }
    }

    // Registro con correo y contraseña
    public void registerWithEmail(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError("Error al registrar: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                    }
                });
    }

    // Inicio de sesión con correo y contraseña
    public void loginWithEmail(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError("Error al iniciar sesión: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                    }
                });
    }

    // Inicio de sesión anónimo
    public void loginAnonymously(AuthCallback callback) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError("Error al iniciar sesión anónima: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                    }
                });
    }

    // Enviar código de verificación para autenticación por teléfono
    public void sendPhoneVerificationCode(Activity activity, String phoneNumber, 
                                         PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // Verificar código de teléfono
    public void verifyPhoneCode(String verificationId, String code, AuthCallback callback) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError("Error al verificar el código: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                    }
                });
    }

    // Cerrar sesión
    public void logout() {
        mAuth.signOut();
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut();
        }
    }

    // Verificar si hay un usuario logueado
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // Obtener el usuario actual
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Resetear contraseña
    public void resetPassword(String email, OnCompleteListener<Void> listener) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }
}