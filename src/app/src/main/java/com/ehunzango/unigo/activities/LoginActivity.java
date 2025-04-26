package com.ehunzango.unigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
 * Actividad para gestionar el inicio de sesión (anónimo, Google)
 * y alojar los fragments de login/registro por email.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final String WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID"; // Reemplazar por tu ID real

    private View rootView;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialButton googleSignInButton;
    private MaterialButton anonymousSignInButton;

    private FirebaseAuthService authService;
    private FirebaseUserService userService;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();

        authService = FirebaseAuthService.getInstance();
        userService = FirebaseUserService.getInstance();

        // Obtiene automáticamente el web client ID del JSON procesado
        String webClientId = getString(R.string.default_web_client_id);
        authService.initGoogleSignIn(this, webClientId);

        setupGoogleSignInLauncher();
    }


    @Override
    protected void initViews() {
        rootView = findViewById(android.R.id.content);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        anonymousSignInButton = findViewById(R.id.anonymousSignInButton);

        // Configura las pestañas de Login/Register
        AuthPagerAdapter adapter = new AuthPagerAdapter(this);
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {
            tab.setText(pos == 0
                ? getString(R.string.login_tab)
                : getString(R.string.register_tab));
        }).attach();

        // Botones de Google y anónimo
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        anonymousSignInButton.setOnClickListener(v -> signInAnonymously());
    }

    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Llamada directa sin variable intermedia
                Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());

                authService.handleGoogleSignInResult(task, new FirebaseAuthService.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        // Mismo patrón: navegar YA y guardar en background
                        showMessage(getString(R.string.success_login));
                        navigateToMainActivity();
                        userService.saveUser(user, new FirebaseUserService.UserCallback() {
                            @Override
                            public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                                Log.d(TAG, "Usuario Google guardado correctamente");
                            }
                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Error guardando usuario Google: " + errorMessage);
                            }
                        });
                    }
                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                });
            }
        );
    }

    private void signInWithGoogle() {
        Intent intent = authService.getGoogleSignInIntent();
        googleSignInLauncher.launch(intent);
    }

    private void signInAnonymously() {
        // Mostramos snackbar con texto inline
        Snackbar loading = Snackbar.make(
            rootView,
            "Iniciando sesión anónima…",
            Snackbar.LENGTH_INDEFINITE
        );
        loading.show();

        authService.loginAnonymously(new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                loading.dismiss();
                showMessage(getString(R.string.success_login));
                navigateToMainActivity();
                userService.saveUser(user, new FirebaseUserService.UserCallback() {
                    @Override
                    public void onSuccess(com.ehunzango.unigo.models.User userModel) {
                        Log.d(TAG, "Usuario anónimo guardado correctamente");
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error guardando usuario anónimo: " + errorMessage);
                    }
                });
            }
            @Override
            public void onError(String errorMessage) {
                loading.dismiss();
                showError("Error en inicio de sesión anónimo: " + errorMessage);
            }
        });
    }

    /** Muestra un Snackbar de error */
    public void showError(String errorMessage) {
        Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG).show();
        Log.e(TAG, errorMessage);
    }

    /** Muestra un Snackbar informativo */
    public void showMessage(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    /** Navega a MainActivity y limpia el back stack */
    public void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}
