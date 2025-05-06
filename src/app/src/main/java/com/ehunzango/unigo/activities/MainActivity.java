package com.ehunzango.unigo.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.fragments.ProfileFragment;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Actividad principal de la aplicación después del inicio de sesión.
 * Gestiona los fragmentos de la interfaz principal.
 */
public class MainActivity extends BaseActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuthService authService;

    // Aqui se guarda el nombre del fragmento actual, así cuando se recarge la actividad (al rotar la pantalla) no se pierde
    private FragmentType actualFragment = FragmentType.MAP;

    // Si usamos un enum es más facil referenciarlos
    public enum FragmentType
    {
        // enum -> int : enum.ordinal()
        // int -> enum : FragmentType.values()[int]
        MAP,
        PROFILE,
        SETTINGS
    }

    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                             INICIALIZACION                               |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar vistas
        initViews();
        
        // Inicializar servicios
        authService = FirebaseAuthService.getInstance();

        // Obtener los datos del saved instance
        if(savedInstanceState != null)
        {
            actualFragment = FragmentType.values()[savedInstanceState.getInt("actualFragment")];
        }
        
        // Configurar navegación
        setupNavigation();

        // Abrir fragmento actual
        openCurrentFragment();
    }

    @Override
    protected void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }
    
    private void setupNavigation()
    {
        // Establecer ítem seleccionado
        switch (actualFragment)
        {
            case PROFILE:
                bottomNav.setSelectedItemId(R.id.nav_profile);
                break;

            case SETTINGS:
                bottomNav.setSelectedItemId(R.id.nav_settings);
                break;

            case MAP:
            default:
                bottomNav.setSelectedItemId(R.id.nav_map);
                break;
        }

        // Configurar listener para la navegación inferior
        // Primero se selecciona el item correspondiente, para que no salte el listener y carge el fragmento dos veces
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                // Navegar al fragmento correspondiente basado en el ítem seleccionado
                if (itemId == R.id.nav_map)
                {
                    // Cargar fragmento de mapa
                    openMapFragment();
                    return true;
                }
                else if (itemId == R.id.nav_profile)
                {
                    // Cargar fragmento de perfil
                    openProfileFragment();
                    return true;
                }
                else if (itemId == R.id.nav_settings)
                {
                    // Cargar fragmento de ajustes
                    openSettingsFragment();
                    return true;
                }

                return false;
            }
        });
    }

    private void openCurrentFragment()
    {
        switch (actualFragment)
        {
            case PROFILE:
                openProfileFragment();
                break;

            case SETTINGS:
                openSettingsFragment();
                break;

            case MAP:
            default:
                openMapFragment();
                break;
        }
    }



    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                      GESTIÓN DE SESION DE USUARIO                        |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

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


    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                            RECUPERACION DE DATOS                         |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    public void onSaveInstanceState(Bundle bundle)
    {
        super.onSaveInstanceState(bundle);

        bundle.putInt("actualFragment", actualFragment.ordinal());
    }

    public void onRestoreInstanceState (Bundle bundle)
    {

    }

    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                     APERTURA Y GESTIÓN DE ACTIVIDADES                    |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+


    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                     APERTURA Y GESTIÓN DE FRAGMENTOS                     |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    private void openMapFragment()
    {
        actualFragment = FragmentType.MAP;
        showToast("Mapa - Por implementar");
    }

    private void openProfileFragment() {
        actualFragment = FragmentType.PROFILE;
        replaceFragment(R.id.fragment_container, new ProfileFragment(), "profile", false);
    }

    private void openSettingsFragment()
    {
        actualFragment = FragmentType.SETTINGS;
        showToast("Ajustes - Por implementar");
    }


    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |               IMPLEMENTACION DE LISTENERS DE FRAGMENTOS                  |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+


    // ----- LISTENER MAP


    // ----- LISTENER PROFILE


    // ----- LISTENER SETTINGS








}