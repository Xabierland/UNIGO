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

/**
 * Fragmento para la pantalla de inicio de sesión con correo y contraseña,
 * navegando inmediatamente a MainActivity y guardando el usuario en segundo plano.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private ProgressBar progressBar;

    private FirebaseAuthService authService;
    private FirebaseUserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar servicios
        authService = FirebaseAuthService.getInstance();
        userService = FirebaseUserService.getInstance();

        // Inicializar vistas
        emailLayout = view.findViewById(R.id.emailLayout);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        progressBar = view.findViewById(R.id.progressBar);

        // Configurar listeners
        loginButton.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUser();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

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

        return isValid;
    }

    private void loginUser() {
        setLoadingState(true);
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "Intentando iniciar sesión con: " + email);

        authService.loginWithEmail(email, password, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Inicio de sesión exitoso para: " + user.getUid());

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
                                Log.d(TAG, "Usuario guardado en base de datos correctamente");
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Error al guardar usuario en base de datos: " + errorMessage);
                            }
                        });
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

    private void setLoadingState(boolean isLoading) {
        if (progressBar == null) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
    }
}
