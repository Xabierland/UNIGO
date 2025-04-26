package com.ehunzango.unigo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.activities.LoginActivity;
import com.ehunzango.unigo.activities.MainActivity;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragmento para la pantalla de inicio de sesión
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    
    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordEditText;
    private TextView forgotPasswordText;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private FirebaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar servicio de autenticación
        authService = FirebaseAuthService.getInstance();
        
        // Inicializar vistas
        emailLayout = view.findViewById(R.id.emailLayout);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        forgotPasswordText = view.findViewById(R.id.forgotPasswordText);
        loginButton = view.findViewById(R.id.loginButton);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Configurar listeners
        setupListeners();
    }
    
    private void setupListeners() {
        // Botón de inicio de sesión
        loginButton.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUser();
            }
        });
        
        // Enlace de contraseña olvidada
        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Validar email
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }
        
        // Validar contraseña
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_invalid_password));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }
        
        return isValid;
    }
    
    private void loginUser() {
        // Mostrar progreso
        setLoadingState(true);
        
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        Log.d(TAG, "Intentando iniciar sesión con: " + email);
        
        // Intentar inicio de sesión con Firebase
        authService.loginWithEmail(email, password, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Inicio de sesión exitoso para: " + user.getUid());
                
                // Ir directamente a MainActivity tras inicio de sesión exitoso
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        navigateToMainActivity();
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error en inicio de sesión: " + errorMessage);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        if (getActivity() instanceof LoginActivity) {
                            ((LoginActivity) getActivity()).showError(errorMessage);
                        }
                    });
                }
            }
        });
    }
    
    private void showForgotPasswordDialog() {
        // Inflar la vista para el diálogo
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reset_password, null);
        TextInputEditText resetEmailEditText = dialogView.findViewById(R.id.resetEmailEditText);
        
        // Prellenar con el email actual si existe
        if (emailEditText.getText() != null && !TextUtils.isEmpty(emailEditText.getText().toString())) {
            resetEmailEditText.setText(emailEditText.getText().toString());
        }
        
        // Crear y mostrar el diálogo
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_reset_password_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_button_send, (dialog, which) -> {
                    String email = resetEmailEditText.getText().toString().trim();
                    
                    if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        // Enviar correo de recuperación
                        authService.resetPassword(email, task -> {
                            if (task.isSuccessful()) {
                                if (getActivity() instanceof LoginActivity) {
                                    ((LoginActivity) getActivity()).showMessage(getString(R.string.success_reset_password));
                                }
                            } else {
                                if (getActivity() instanceof LoginActivity) {
                                    ((LoginActivity) getActivity()).showError(task.getException() != null 
                                            ? task.getException().getMessage() 
                                            : "Error al enviar el correo de recuperación");
                                }
                            }
                        });
                    } else {
                        if (getActivity() instanceof LoginActivity) {
                            ((LoginActivity) getActivity()).showError(getString(R.string.error_invalid_email));
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }
    
    private void navigateToMainActivity() {
        if (getActivity() == null) return;
        
        try {
            // Mostrar mensaje de éxito
            if (getActivity() instanceof LoginActivity) {
                ((LoginActivity) getActivity()).showMessage(getString(R.string.success_login));
            }
            
            // Navegar directamente a MainActivity con pequeño delay para mostrar el mensaje
            new android.os.Handler().postDelayed(() -> {
                try {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error en navegación: " + e.getMessage());
                }
            }, 300);
        } catch (Exception e) {
            Log.e(TAG, "Error en navegación: " + e.getMessage());
        }
    }
    
    private void setLoadingState(boolean isLoading) {
        if (progressBar == null) return;
        
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        forgotPasswordText.setEnabled(!isLoading);
    }
}