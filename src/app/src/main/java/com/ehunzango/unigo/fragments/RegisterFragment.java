package com.ehunzango.unigo.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.activities.LoginActivity;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.ehunzango.unigo.services.FirebaseUserService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Fragmento para la pantalla de registro con correo y contraseña,
 * navegando inmediatamente a MainActivity y guardando el usuario en segundo plano.
 */
public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    private TextInputLayout nameLayout;
    private TextInputEditText nameEditText;
    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordEditText;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordEditText;
    private MaterialButton registerButton;
    private ProgressBar progressBar;

    private FirebaseAuthService authService;
    private FirebaseUserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar servicios
        authService = FirebaseAuthService.getInstance();
        userService = FirebaseUserService.getInstance();

        // Inicializar vistas
        nameLayout = view.findViewById(R.id.nameLayout);
        nameEditText = view.findViewById(R.id.nameEditText);
        emailLayout = view.findViewById(R.id.emailLayout);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);
        registerButton = view.findViewById(R.id.registerButton);
        progressBar = view.findViewById(R.id.progressBar);

        // Configurar listeners
        registerButton.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_empty_name));
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_invalid_password));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword) || !confirmPassword.equals(password)) {
            confirmPasswordLayout.setError(getString(R.string.error_passwords_not_match));
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void registerUser() {
        setLoadingState(true);
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "Iniciando registro para: " + email);

        authService.registerWithEmail(email, password, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Registro exitoso en Firebase Auth: " + user.getUid());

                // Actualizar perfil con el nombre
                UserProfileChangeRequest profileUpdates =
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (getActivity() instanceof LoginActivity) {
                            getActivity().runOnUiThread(() -> {
                                setLoadingState(false);

                                // Navegar inmediatamente a MainActivity
                                LoginActivity activity = (LoginActivity) getActivity();
                                activity.showMessage(activity.getString(R.string.success_login));
                                activity.navigateToMainActivity();

                                // Persistir usuario en segundo plano
                                userService.saveUser(user, new FirebaseUserService.UserCallback() {
                                    @Override
                                    public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                                        Log.d(TAG, "Usuario registrado y guardado correctamente");
                                    }
                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "Error al guardar usuario tras registro: " + errorMessage);
                                    }
                                });
                            });
                        }
                    });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error en registro: " + errorMessage);
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

    private void setLoadingState(boolean isLoading) {
        if (progressBar == null) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        nameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);
    }
}
