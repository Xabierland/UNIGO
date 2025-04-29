package com.ehunzango.unigo.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.activities.MainActivity;
import com.ehunzango.unigo.models.User;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.ehunzango.unigo.services.FirebaseUserService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Fragmento para mostrar y gestionar el perfil del usuario.
 * Contiene la opción de cierre de sesión y edición de datos.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // Vistas de visualización
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private CircleImageView userAvatarImageView;
    private Button logoutButton;
    
    // Vistas de edición
    private View editModeContainer;
    private TextInputLayout nameInputLayout;
    private TextInputEditText nameEditText;
    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordEditText;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText confirmPasswordEditText;
    private ProgressBar progressBar;
    
    // Botón flotante para editar/guardar
    private FloatingActionButton fab;
    
    // Variables para controlar el estado
    private boolean isEditMode = false;
    private boolean isGoogleUser = false;
    private boolean isAnonymousUser = false;
    
    // Servicios
    private FirebaseAuthService authService;
    private FirebaseUserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar servicios
        authService = FirebaseAuthService.getInstance();
        userService = FirebaseUserService.getInstance();
        
        // Inicializar vistas de visualización
        userNameTextView = view.findViewById(R.id.user_name_text);
        userEmailTextView = view.findViewById(R.id.user_email_text);
        userAvatarImageView = view.findViewById(R.id.user_avatar_image);
        logoutButton = view.findViewById(R.id.logout_button);
        
        // Inicializar vistas de edición
        editModeContainer = view.findViewById(R.id.edit_mode_container);
        nameInputLayout = view.findViewById(R.id.name_input_layout);
        nameEditText = view.findViewById(R.id.name_edit_text);
        emailInputLayout = view.findViewById(R.id.email_input_layout);
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordInputLayout = view.findViewById(R.id.password_input_layout);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        confirmPasswordInputLayout = view.findViewById(R.id.confirm_password_input_layout);
        confirmPasswordEditText = view.findViewById(R.id.confirm_password_edit_text);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Inicializar FAB
        fab = view.findViewById(R.id.edit_fab);
        
        // Configurar listener para botón de cierre de sesión
        logoutButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
        
        // Configurar listener para el FAB
        fab.setOnClickListener(v -> {
            if (isEditMode) {
                // Guardar cambios
                saveChanges();
            } else {
                // Entrar en modo edición
                enableEditMode();
            }
        });
        
        // Cargar datos del usuario
        loadUserData();
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            // Determinar tipo de usuario
            isGoogleUser = currentUser.getProviders() != null && 
                           currentUser.getProviders().contains("google.com");
            isAnonymousUser = currentUser.isAnonymous();
            
            // Mostrar el nombre del usuario si está disponible
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                userNameTextView.setText(currentUser.getDisplayName());
            } else {
                userNameTextView.setText(R.string.anonymous_user);
            }
            
            // Mostrar el email si está disponible
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                userEmailTextView.setText(currentUser.getEmail());
            } else if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                userEmailTextView.setText(currentUser.getPhoneNumber());
            } else {
                userEmailTextView.setVisibility(View.GONE);
            }
            
            // Cargar datos en los campos de edición
            nameEditText.setText(currentUser.getDisplayName());
            emailEditText.setText(currentUser.getEmail());
        }
    }
    
    private void enableEditMode() {
        isEditMode = true;
        
        // Cambiar ícono del FAB
        fab.setImageResource(R.drawable.ic_check);
        
        // Mostrar contenedor de edición y ocultar vista de perfil
        editModeContainer.setVisibility(View.VISIBLE);
        userNameTextView.setVisibility(View.GONE);
        userEmailTextView.setVisibility(View.GONE);
        logoutButton.setVisibility(View.GONE);
        
        // Configurar campos según tipo de usuario
        if (isAnonymousUser) {
            // Usuario anónimo no puede editar nada
            showToast("Los usuarios anónimos no pueden modificar sus datos");
            disableEditMode();
            return;
        } else if (isGoogleUser) {
            // Usuario de Google solo puede editar nombre
            emailInputLayout.setEnabled(false);
            passwordInputLayout.setVisibility(View.GONE);
            confirmPasswordInputLayout.setVisibility(View.GONE);
        } else {
            // Usuario email/contraseña puede editar todo
            emailInputLayout.setEnabled(true);
            passwordInputLayout.setVisibility(View.VISIBLE);
            confirmPasswordInputLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void disableEditMode() {
        isEditMode = false;
        
        // Cambiar ícono del FAB
        fab.setImageResource(R.drawable.ic_edit);
        
        // Ocultar contenedor de edición y mostrar vista de perfil
        editModeContainer.setVisibility(View.GONE);
        userNameTextView.setVisibility(View.VISIBLE);
        userEmailTextView.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);
        
        // Limpiar errores
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
    }
    
    private void saveChanges() {
        if (!validateInputs()) {
            return;
        }
        
        setLoadingState(true);
        
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showToast("Error: Usuario no encontrado");
            setLoadingState(false);
            return;
        }
        
        String newName = nameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String newPassword = passwordEditText.getText().toString().trim();
        
        // Actualizar nombre de usuario
        if (!TextUtils.equals(newName, currentUser.getDisplayName())) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();
            
            currentUser.updateProfile(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        userNameTextView.setText(newName);
                        userService.updateDisplayName(currentUser.getUid(), newName, task -> {
                            Log.d(TAG, "Nombre actualizado en base de datos");
                        });
                    })
                    .addOnFailureListener(e -> {
                        showToast("Error al actualizar el nombre: " + e.getMessage());
                        Log.e(TAG, "Error al actualizar nombre: " + e.getMessage());
                    });
        }
        
        // Si no es Google, actualizar email y contraseña
        if (!isGoogleUser && !isAnonymousUser) {
            // Actualizar email si cambió
            if (!TextUtils.equals(newEmail, currentUser.getEmail())) {
                currentUser.updateEmail(newEmail)
                        .addOnSuccessListener(aVoid -> {
                            userEmailTextView.setText(newEmail);
                            showToast("Email actualizado correctamente");
                        })
                        .addOnFailureListener(e -> {
                            showToast("Error al actualizar email: " + e.getMessage());
                            Log.e(TAG, "Error al actualizar email: " + e.getMessage());
                        });
            }
            
            // Actualizar contraseña si se proporcionó
            if (!TextUtils.isEmpty(newPassword)) {
                currentUser.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid -> {
                            showToast("Contraseña actualizada correctamente");
                            passwordEditText.setText("");
                            confirmPasswordEditText.setText("");
                        })
                        .addOnFailureListener(e -> {
                            showToast("Error al actualizar contraseña: " + e.getMessage());
                            Log.e(TAG, "Error al actualizar contraseña: " + e.getMessage());
                        });
            }
        }
        
        setLoadingState(false);
        disableEditMode();
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validar nombre
        String name = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError(getString(R.string.error_empty_name));
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }
        
        // Si no es Google, validar email y contraseña
        if (!isGoogleUser && !isAnonymousUser) {
            // Validar email
            String email = emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.setError(getString(R.string.error_invalid_email));
                isValid = false;
            } else {
                emailInputLayout.setError(null);
            }
            
            // Validar contraseña (solo si se ha introducido)
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            
            if (!TextUtils.isEmpty(password)) {
                if (password.length() < 6) {
                    passwordInputLayout.setError(getString(R.string.error_invalid_password));
                    isValid = false;
                } else {
                    passwordInputLayout.setError(null);
                }
                
                if (!TextUtils.equals(password, confirmPassword)) {
                    confirmPasswordInputLayout.setError(getString(R.string.error_passwords_not_match));
                    isValid = false;
                } else {
                    confirmPasswordInputLayout.setError(null);
                }
            }
        }
        
        return isValid;
    }
    
    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        nameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading && !isGoogleUser && !isAnonymousUser);
        passwordEditText.setEnabled(!isLoading && !isGoogleUser && !isAnonymousUser);
        confirmPasswordEditText.setEnabled(!isLoading && !isGoogleUser && !isAnonymousUser);
        fab.setEnabled(!isLoading);
    }
    
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}