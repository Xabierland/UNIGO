package com.ehunzango.unigo.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ehunzango.unigo.R;
import com.ehunzango.unigo.activities.MainActivity;
import com.ehunzango.unigo.models.User;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.ehunzango.unigo.services.FirebaseUserService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment
{
    // Variables de clase
    private static final String TAG = "ProfileFragment";
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_STORAGE = 101;
    private ListenerFragmentProfile listener;

    // Vistas de visualización
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private CardView profilePhotoContainer;
    private MaterialButton logoutButton;

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
    private TextView editInfoText;
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

    // Foto de perfil
    private Uri currentPhotoUri;
    private String currentPhotoPath;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

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
        logoutButton = view.findViewById(R.id.logout_button);
        profilePhotoContainer = view.findViewById(R.id.profile_photo_container);

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
        editInfoText = view.findViewById(R.id.edit_info_text);
        progressBar = view.findViewById(R.id.progress_bar);

        // Inicializar FAB
        fab = view.findViewById(R.id.edit_fab);

        // Configurar launcher para tomar foto
        setupActivityResultLaunchers();

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

        // Configurar listener para el contenedor de foto de perfil
        profilePhotoContainer.setOnClickListener(v -> {
            if (isEditMode) {
                showPhotoOptionsDialog();
            }
        });

        // Cargar datos del usuario
        loadUserData();
    }

    private void setupActivityResultLaunchers() {
        // Launcher para solicitar permisos con mejor manejo
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean isGranted : permissions.values()) {
                        if (!isGranted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        // Todos los permisos concedidos, mostrar opciones de foto
                        showPhotoOptionsBottomSheet();
                    } else {
                        // Verificar si debemos mostrar explicación racional
                        if (checkShouldShowPermissionRationale(Manifest.permission.CAMERA)) {
                            // El usuario rechazó pero no seleccionó "No volver a preguntar"
                            showRationaleDialog();
                        } else {
                            // El usuario ha denegado permanentemente o estamos en Android < 6.0
                            showSettingsDialog();
                        }
                    }
                });

        // Launcher para tomar foto con la cámara
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        // La foto se ha guardado en currentPhotoUri
                        if (currentPhotoUri != null) {
                            savePhotoLocally(currentPhotoUri);
                        }
                    }
                });

        // Launcher para seleccionar imagen de la galería
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            savePhotoLocally(selectedImageUri);
                        }
                    }
                });
    }

    private void loadUserData() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            // Determinar tipo de usuario
            isGoogleUser = false; // Inicializar como false
            // Comprobar si el usuario inició sesión con Google
            for (com.google.firebase.auth.UserInfo profile : currentUser.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) {
                    isGoogleUser = true;
                    break;
                }
            }
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

            // Cargar foto de perfil si está disponible
            loadProfilePhoto(currentUser);

            // Cargar datos en los campos de edición
            nameEditText.setText(currentUser.getDisplayName());
            emailEditText.setText(currentUser.getEmail());
        }
    }

    private void loadProfilePhoto(FirebaseUser user) {
        if (getContext() == null) return;

        // Primero intentar cargar desde SharedPreferences (almacenamiento local)
        SharedPreferences prefs = getContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        String localPhotoPath = prefs.getString("profile_photo_path", null);
        
        if (localPhotoPath != null) {
            // Verificar si el archivo existe
            File photoFile = new File(localPhotoPath);
            if (photoFile.exists()) {
                // Cargar la imagen desde el archivo local
                Glide.with(this)
                     .load(photoFile)
                     .placeholder(R.drawable.ic_person)
                     .error(R.drawable.ic_person)
                     .centerCrop()
                     .into((android.widget.ImageView) profilePhotoContainer.findViewById(R.id.profile_photo));
                return;
            }
        }
        
        // Como fallback, intentar usar la URI de FirebaseUser si existe
        if (user.getPhotoUrl() != null) {
            // Verificar si es una URI local o remota
            String uriString = user.getPhotoUrl().toString();
            if (uriString.startsWith("file:")) {
                // Es una URI local
                File photoFile = new File(user.getPhotoUrl().getPath());
                if (photoFile.exists()) {
                    Glide.with(this)
                         .load(photoFile)
                         .placeholder(R.drawable.ic_person)
                         .error(R.drawable.ic_person)
                         .centerCrop()
                         .into((android.widget.ImageView) profilePhotoContainer.findViewById(R.id.profile_photo));
                    return;
                }
            }
        }
        
        // Si no hay imagen local, cargar la imagen por defecto
        Glide.with(this)
             .load(R.drawable.ic_person)
             .centerCrop()
             .into((android.widget.ImageView) profilePhotoContainer.findViewById(R.id.profile_photo));
    }

    private void enableEditMode() {
        isEditMode = true;

        // Cambiar ícono del FAB
        fab.setImageResource(R.drawable.ic_check);

        // Mostrar contenedor de edición y ocultar vista de perfil
        editModeContainer.setVisibility(View.VISIBLE);
        userNameTextView.setVisibility(View.GONE);
        userEmailTextView.setVisibility(View.GONE);

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
            editInfoText.setVisibility(View.GONE);
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

    private void showPhotoOptionsDialog() {
        if (getContext() == null) return;

        // Verificar permisos de manera más explícita
        if (checkAndRequestPermissions()) {
            // Permisos concedidos, mostrar diálogo de opciones
            showPhotoOptionsBottomSheet();
        }
    }

    private boolean checkAndRequestPermissions() {
        if (getContext() == null) return false;

        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(getContext(), 
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        
        boolean storagePermissionGranted;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Para Android 10+ solo necesitamos permisos de cámara
            storagePermissionGranted = true;
        } else {
            // Para versiones anteriores, necesitamos permisos de almacenamiento
            storagePermissionGranted = ContextCompat.checkSelfPermission(getContext(), 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        // Si todos los permisos están concedidos, devolver true
        if (cameraPermissionGranted && storagePermissionGranted) {
            return true;
        }

        // Recopilar permisos que necesitamos solicitar
        List<String> permissionsToRequest = new ArrayList<>();
        if (!cameraPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (!storagePermissionGranted && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        // Solicitar permisos
        if (!permissionsToRequest.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            return false;
        }

        return true;
    }

    private boolean checkShouldShowPermissionRationale(String permission) {
        if (getActivity() == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return getActivity().shouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    private void showRationaleDialog() {
        if (getContext() == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Permisos necesarios")
                .setMessage("Para cambiar tu foto de perfil, necesitamos acceder a tu cámara y almacenamiento.")
                .setPositiveButton("Solicitar de nuevo", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSettingsDialog() {
        if (getContext() == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Permisos requeridos")
                .setMessage("Has denegado los permisos necesarios. Por favor, habilítalos en la configuración de la aplicación.")
                .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                    dialog.dismiss();
                    openAppSettings();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openAppSettings() {
        if (getContext() == null) return;
        
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void showPhotoOptionsBottomSheet() {
        // Mostrar diálogo de opciones
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_photo_options, null);
        dialog.setContentView(dialogView);

        // Configurar listeners para las opciones
        dialogView.findViewById(R.id.option_camera).setOnClickListener(v -> {
            dialog.dismiss();
            dispatchTakePictureIntent();
        });

        dialogView.findViewById(R.id.option_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            dispatchPickImageIntent();
        });

        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        if (getContext() == null) return;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // Crear el archivo donde irá la foto
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error creando archivo de imagen", ex);
            showToast("Error al crear archivo para la foto");
            return;
        }

        // Continuar solo si el archivo se creó correctamente
        if (photoFile != null) {
            try {
                currentPhotoUri = FileProvider.getUriForFile(getContext(),
                        "com.ehunzango.unigo.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                takePictureLauncher.launch(takePictureIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error al configurar la cámara: " + e.getMessage(), e);
                showToast("Error al abrir la cámara");
            }
        }
    }

    private File createImageFile() throws IOException {
        // Crear un nombre de archivo único
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefijo
                ".jpg",         // sufijo
                storageDir      // directorio
        );

        // Guardar la ruta para usar con intents ACTION_VIEW
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchPickImageIntent() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setType("image/*");
        pickImageLauncher.launch(pickImageIntent);
    }

    private void savePhotoLocally(Uri photoUri) {
        if (getContext() == null) return;

        setLoadingState(true);
        showToast("Guardando foto...");

        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            setLoadingState(false);
            showToast("Error: Usuario no encontrado");
            return;
        }

        try {
            // Comprimir la imagen antes de guardarla
            InputStream imageStream = getContext().getContentResolver().openInputStream(photoUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            
            // Crear un nombre de archivo único para el usuario actual
            String userId = currentUser.getUid();
            String fileName = "profile_" + userId + ".jpg";
            
            // Guardar en almacenamiento interno de la aplicación
            File filesDir = getContext().getFilesDir();
            File photoFile = new File(filesDir, fileName);
            
            // Comprimir y guardar la imagen
            FileOutputStream outputStream = new FileOutputStream(photoFile);
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            outputStream.flush();
            outputStream.close();
            
            Log.d(TAG, "Foto guardada localmente en: " + photoFile.getAbsolutePath());
            
            // Actualizar la referencia local en SharedPreferences para acceso rápido
            SharedPreferences prefs = getContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
            prefs.edit().putString("profile_photo_path", photoFile.getAbsolutePath()).apply();
            
            // Actualizar también en FirebaseUser (opcional, usará URI local)
            Uri localPhotoUri = Uri.fromFile(photoFile);
            updateUserWithLocalPhoto(localPhotoUri);
            
            // Actualizar la imagen de perfil en la UI
            Glide.with(this)
                 .load(photoFile)
                 .placeholder(R.drawable.ic_person)
                 .error(R.drawable.ic_person)
                 .centerCrop()
                 .into((android.widget.ImageView) profilePhotoContainer.findViewById(R.id.profile_photo));
            
            setLoadingState(false);
            showToast("Foto de perfil actualizada");
            
        } catch (IOException e) {
            setLoadingState(false);
            Log.e(TAG, "Error al guardar imagen localmente", e);
            showToast("Error al guardar imagen: " + e.getMessage());
        } catch (Exception e) {
            setLoadingState(false);
            Log.e(TAG, "Error inesperado", e);
            showToast("Error inesperado: " + e.getMessage());
        }
    }
    
    private void updateUserWithLocalPhoto(Uri photoUri) {
        FirebaseUser user = authService.getCurrentUser();
        if (user == null) return;

        // Actualizar el perfil del usuario con la nueva URI de la foto local
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUri)
                .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Perfil actualizado con URI local");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar perfil con URI local", e);
                });
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        nameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading && !isGoogleUser && !isAnonymousUser);
        passwordEditText.setEnabled(!isLoading && !isGoogleUser && !isAnonymousUser);
        confirmPasswordEditText.setEnabled(!isLoading && !isGoogleUser && !isAnonymousUser);
        fab.setEnabled(!isLoading);
        logoutButton.setEnabled(!isLoading);
        profilePhotoContainer.setEnabled(!isLoading && isEditMode);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                        CONEXION CON MAIN ACTIVITY                        |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{
            listener = (ProfileFragment.ListenerFragmentProfile) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException("La clase " +context.toString() + " debe implementar el listener");
        }
    }

    public interface ListenerFragmentProfile
    {

    }
}