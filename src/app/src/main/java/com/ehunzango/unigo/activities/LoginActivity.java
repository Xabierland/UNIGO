package com.ehunzango.unigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.auth.FirebaseUser;

/**
 * Actividad para gestionar el inicio de sesión y registro de usuarios
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final String WEB_CLIENT_ID = "AIzaSyAU_hAK5SskHRFbzziliLq9M6iGA8Ew-Ng"; // Reemplazar con el ID real

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialButton googleSignInButton;
    private MaterialButton anonymousSignInButton;
    private View rootView;

    private FirebaseAuthService authService;
    private FirebaseUserService userService;
    
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
        // Mostrar un indicador de progreso
        View rootView = findViewById(android.R.id.content);
        Snackbar loadingSnackbar = Snackbar.make(rootView, "Iniciando sesión anónima...", Snackbar.LENGTH_INDEFINITE);
        loadingSnackbar.show();
        
        // Intentar el inicio de sesión anónimo
        authService.loginAnonymously(new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // Log para depuración
                Log.d(TAG, "Login anónimo exitoso: " + user.getUid());
                loadingSnackbar.dismiss();
                
                // Si el login fue exitoso, ir directamente a MainActivity
                showMessage(getString(R.string.success_login));
                navigateToMainActivity();
                
                // Intentar guardar el usuario en segundo plano
                userService.saveUser(user, new FirebaseUserService.UserCallback() {
                    @Override
                    public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                        // Solo log, ya navegamos a MainActivity
                        Log.d(TAG, "Usuario guardado en base de datos correctamente");
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // Solo log, ya navegamos a MainActivity
                        Log.e(TAG, "Error al guardar usuario en base de datos: " + errorMessage);
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                // Log para depuración
                Log.e(TAG, "Error en login anónimo: " + errorMessage);
                loadingSnackbar.dismiss();
                showError("Error en inicio de sesión anónimo: " + errorMessage);
            }
        });
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
        navigateTo(MainActivity.class);
        finish();
    }
}