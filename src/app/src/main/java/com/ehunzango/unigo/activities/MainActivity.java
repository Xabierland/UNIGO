package com.ehunzango.unigo.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.fragments.MapFragment;
import com.ehunzango.unigo.fragments.ProfileFragment;
import com.ehunzango.unigo.fragments.SettingsFragment;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.util.Date;
import java.util.Locale;

/**
 * Actividad principal de la aplicación después del inicio de sesión.
 * Gestiona los fragmentos de la interfaz principal.
 */
public class MainActivity extends       BaseActivity

                          implements    SettingsFragment.ListenerFragmentSettings,
                                        ProfileFragment.ListenerFragmentProfile,
                                        MapFragment.ListenerFragmentMap
{

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

        // Obtener el fragmento desde el intent (en caso de reinicio por ajustes)
        if (getIntent() != null && getIntent().hasExtra("startFragment")) {
            int fragmentOrdinal = getIntent().getIntExtra("startFragment", FragmentType.MAP.ordinal());
            actualFragment = FragmentType.values()[fragmentOrdinal];
            getIntent().removeExtra("startFragment");
        }
        // Obtener los datos del saved instance (desde el estado guardado, si no hay intent)
        else if (savedInstanceState != null)
        {
            actualFragment = FragmentType.values()[savedInstanceState.getInt("actualFragment")];
        }

        //              +--------------------------------------------------------+
        //              |                       PERMISOS                         |
        //              +                                                        +

        checkLocationPermission();

        //              +                                                        +
        //              |                       PERMISOS                         |
        //              +--------------------------------------------------------+


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

    private boolean checkLocationPermission()
    {
        // Si el permiso está concedido
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            // Nada
            //Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            return true;
        }
        // Si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
        {

            Toast.makeText(this, "Solicitando permiso otra vez", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Se requieren permisos de ubicación para poder usar el mapa"); //TODO: Idiomas

            LinearLayout layoutName = new LinearLayout(getBaseContext());
            layoutName.setOrientation(LinearLayout.VERTICAL);

            builder.setView(layoutName);

            builder.setPositiveButton("Abrir ajustes", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            });

            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });


            builder.show();
        }

        // Solicitar permiso
        else
        {
            Toast.makeText(this, "Solicitando permiso", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 31);
        }

        return false;
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
        replaceFragment(R.id.fragment_container, new MapFragment(), "map", false);
    }

    private void openProfileFragment() {
        actualFragment = FragmentType.PROFILE;
        replaceFragment(R.id.fragment_container, new ProfileFragment(), "profile", false);
    }

    private void openSettingsFragment()
    {
        actualFragment = FragmentType.SETTINGS;
        replaceFragment(R.id.fragment_container, new SettingsFragment(), "settings", false);
    }


    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |               IMPLEMENTACION DE LISTENERS DE FRAGMENTOS                  |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+


    // ----- LISTENER MAP

    @Override
    public boolean askLocationPermission()
    {
        return checkLocationPermission();
    }

    // ----- LISTENER PROFILE


    // ----- LISTENER SETTINGS








}