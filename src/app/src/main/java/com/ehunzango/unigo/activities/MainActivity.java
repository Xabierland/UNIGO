package com.ehunzango.unigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.ehunzango.unigo.services.FirebaseUserService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseUser;

/**
 * Actividad principal de la aplicación después del inicio de sesión.
 * Gestiona los fragmentos de la interfaz principal.
 */
public class MainActivity extends BaseActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuthService authService;
    private FirebaseUserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar vistas
        initViews();
        
        // Inicializar servicios
        authService = FirebaseAuthService.getInstance();
        userService = FirebaseUserService.getInstance();
        
        // Verificar si hay un usuario autenticado
        if (!authService.isUserLoggedIn()) {
            // No hay usuario autenticado, redirigir a LoginActivity
            navigateToLoginActivity();
            finish();
            return;
        }
        
        // Configurar navegación
        setupNavigation();
    }

    @Override
    protected void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }
    
    private void setupNavigation() {
        // Configurar listener para la navegación inferior
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                // Navegar al fragmento correspondiente basado en el ítem seleccionado
                if (itemId == R.id.nav_map) {
                    // Cargar fragmento de mapa
                    showToast("Mapa - Por implementar");
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // Cargar fragmento de perfil
                    showToast("Perfil - Por implementar");
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    // Cargar fragmento de ajustes
                    showToast("Ajustes - Por implementar");
                    return true;
                }
                
                return false;
            }
        });
        
        // Establecer ítem por defecto
        bottomNav.setSelectedItemId(R.id.nav_map);
    }
    
    /**
     * Método para cerrar la sesión del usuario.
     * Se puede llamar desde los fragmentos si es necesario.
     */
    public void logout() {
        // Mostrar diálogo de confirmación
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.dialog_button_send, (dialog, which) -> {
                    // Cerrar sesión
                    authService.logout();
                    
                    // Navegar a LoginActivity
                    navigateToLoginActivity();
                    finish();
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }
    
    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}