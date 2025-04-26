package com.ehunzango.unigo.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.adapters.AuthPagerAdapter;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.ehunzango.unigo.services.FirebaseUserService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * Actividad para gestionar el inicio de sesión y registro de usuarios
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final String WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID"; // Reemplazar con el ID real

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialButton googleSignInButton;
    private MaterialButton phoneSignInButton;
    private MaterialButton anonymousSignInButton;
    private View rootView;

    private FirebaseAuthService authService;
    private FirebaseUserService userService;
    
    // Para verificación por teléfono
    private String verificationId;
    private Dialog phoneVerificationDialog;
    
    // Para inicio de sesión con Google
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Inicializar vistas
        initViews();
        
        // Inicializar servicios
        authService = FirebaseAuthService.getInstance();
        userService = FirebaseUserService.getInstance();
        
        // Configurar Google Sign In
        authService.initGoogleSignIn(this, WEB_CLIENT_ID);
        
        // Configurar launcher para Google Sign In
        setupGoogleSignInLauncher();
        
        // Verificar si ya hay un usuario autenticado
        checkCurrentUser();
    }

    @Override
    protected void initViews() {
        rootView = findViewById(android.R.id.content);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        phoneSignInButton = findViewById(R.id.phoneSignInButton);
        anonymousSignInButton = findViewById(R.id.anonymousSignInButton);
        
        // Configurar ViewPager con TabLayout
        setupViewPager();
        
        // Configurar listeners
        setupClickListeners();
    }
    
    private void setupViewPager() {
        AuthPagerAdapter authPagerAdapter = new AuthPagerAdapter(this);
        viewPager.setAdapter(authPagerAdapter);
        
        // Vincular TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.login_tab);
                    break;
                case 1:
                    tab.setText(R.string.register_tab);
                    break;
            }
        }).attach();
    }
    
    private void setupClickListeners() {
        // Botón de Google
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        
        // Botón de teléfono
        phoneSignInButton.setOnClickListener(v -> showPhoneVerificationDialog());
        
        // Botón anónimo
        anonymousSignInButton.setOnClickListener(v -> signInAnonymously());
    }
    
    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Procesar resultado
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    authService.handleGoogleSignInResult(task, new FirebaseAuthService.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            // Guardar en base de datos
                            userService.saveUser(user, new FirebaseUserService.UserCallback() {
                                @Override
                                public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                                    showMessage(getString(R.string.success_login));
                                    navigateToMainActivity();
                                }
                                
                                @Override
                                public void onError(String errorMessage) {
                                    showError("Inicio de sesión exitoso pero error al guardar datos: " + errorMessage);
                                    navigateToMainActivity();
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                });
    }
    
    private void checkCurrentUser() {
        if (authService.isUserLoggedIn()) {
            // Ya hay un usuario autenticado, ir directamente a MainActivity
            navigateToMainActivity();
            finish();
        }
    }
    
    // Métodos para iniciar sesión
    
    private void signInWithGoogle() {
        Intent signInIntent = authService.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }
    
    private void signInAnonymously() {
        authService.loginAnonymously(new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // Guardar en base de datos
                userService.saveUser(user, new FirebaseUserService.UserCallback() {
                    @Override
                    public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                        showMessage(getString(R.string.success_login));
                        navigateToMainActivity();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        showError("Inicio de sesión anónimo exitoso pero error al guardar datos: " + errorMessage);
                        navigateToMainActivity();
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }
    
    private void showPhoneVerificationDialog() {
        // Inflar vista del diálogo
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_phone_verification, null);
        
        // Inicializar vistas del diálogo
        View phoneNumberContainer = dialogView.findViewById(R.id.phoneNumberContainer);
        View verificationContainer = dialogView.findViewById(R.id.verificationContainer);
        TextInputEditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);
        TextInputEditText codeEditText = dialogView.findViewById(R.id.codeEditText);
        MaterialButton sendCodeButton = dialogView.findViewById(R.id.sendCodeButton);
        MaterialButton verifyCodeButton = dialogView.findViewById(R.id.verifyCodeButton);
        MaterialButton resendCodeButton = dialogView.findViewById(R.id.resendCodeButton);
        View progressBar = dialogView.findViewById(R.id.progressBar);
        
        // Crear diálogo
        phoneVerificationDialog = new Dialog(this);
        phoneVerificationDialog.setContentView(dialogView);
        phoneVerificationDialog.setCancelable(true);
        
        // Ajustar ancho del diálogo
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(phoneVerificationDialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        phoneVerificationDialog.getWindow().setAttributes(layoutParams);
        
        // Configurar botones
        sendCodeButton.setOnClickListener(v -> {
            String phoneNumber = phoneEditText.getText().toString().trim();
            
            if (phoneNumber.isEmpty()) {
                showError(getString(R.string.error_invalid_phone));
                return;
            }
            
            // Mostrar progreso
            progressBar.setVisibility(View.VISIBLE);
            sendCodeButton.setEnabled(false);
            phoneEditText.setEnabled(false);
            
            // Enviar código de verificación
            authService.sendPhoneVerificationCode(this, phoneNumber, 
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            // Autenticación automática en algunos dispositivos
                            progressBar.setVisibility(View.GONE);
                            phoneVerificationDialog.dismiss();
                            
                            // Iniciar sesión con la credencial
                            authService.verifyPhoneCode("", "", new FirebaseAuthService.AuthCallback() {
                                @Override
                                public void onSuccess(FirebaseUser user) {
                                    userService.saveUser(user, new FirebaseUserService.UserCallback() {
                                        @Override
                                        public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                                            showMessage(getString(R.string.success_login));
                                            navigateToMainActivity();
                                        }
                                        
                                        @Override
                                        public void onError(String errorMessage) {
                                            showError("Verificación exitosa pero error al guardar datos: " + errorMessage);
                                            navigateToMainActivity();
                                        }
                                    });
                                }
                                
                                @Override
                                public void onError(String errorMessage) {
                                    showError(errorMessage);
                                }
                            });
                        }
                        
                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            // Error de verificación
                            progressBar.setVisibility(View.GONE);
                            sendCodeButton.setEnabled(true);
                            phoneEditText.setEnabled(true);
                            showError("Error en la verificación: " + e.getMessage());
                        }
                        
                        @Override
                        public void onCodeSent(@NonNull String vId, 
                                              @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            // Código enviado con éxito
                            progressBar.setVisibility(View.GONE);
                            verificationId = vId;
                            
                            // Cambiar vista para introducir código
                            phoneNumberContainer.setVisibility(View.GONE);
                            verificationContainer.setVisibility(View.VISIBLE);
                        }
                    });
        });
        
        verifyCodeButton.setOnClickListener(v -> {
            String code = codeEditText.getText().toString().trim();
            
            if (code.isEmpty()) {
                showError(getString(R.string.error_empty_code));
                return;
            }
            
            // Mostrar progreso
            progressBar.setVisibility(View.VISIBLE);
            verifyCodeButton.setEnabled(false);
            resendCodeButton.setEnabled(false);
            codeEditText.setEnabled(false);
            
            // Verificar código
            authService.verifyPhoneCode(verificationId, code, new FirebaseAuthService.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    progressBar.setVisibility(View.GONE);
                    phoneVerificationDialog.dismiss();
                    
                    // Guardar en base de datos
                    userService.saveUser(user, new FirebaseUserService.UserCallback() {
                        @Override
                        public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                            showMessage(getString(R.string.success_verification));
                            navigateToMainActivity();
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            showError("Verificación exitosa pero error al guardar datos: " + errorMessage);
                            navigateToMainActivity();
                        }
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    verifyCodeButton.setEnabled(true);
                    resendCodeButton.setEnabled(true);
                    codeEditText.setEnabled(true);
                    showError(errorMessage);
                }
            });
        });
        
        // Mostrar diálogo
        phoneVerificationDialog.show();
    }
    
    // Métodos para comunicarse con los fragmentos
    
    public void onLoginSuccess(FirebaseUser user) {
        showMessage(getString(R.string.success_login));
        navigateToMainActivity();
    }
    
    public void onRegisterSuccess(FirebaseUser user) {
        showMessage(getString(R.string.success_register));
        navigateToMainActivity();
    }
    
    public void showError(String errorMessage) {
        Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + errorMessage);
    }
    
    public void showMessage(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }
    
    private void navigateToMainActivity() {
        // Por ahora, terminar la actividad ya que MainActivity no está implementada
        // En el futuro, reemplazar por:
        // navigateTo(MainActivity.class);
        Toast.makeText(this, "Inicio de sesión exitoso. MainActivity aún no implementada.", Toast.LENGTH_LONG).show();
        finish();
    }
}