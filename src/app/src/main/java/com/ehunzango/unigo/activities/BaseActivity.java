package com.ehunzango.unigo.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.ehunzango.unigo.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * Actividad base que proporciona funcionalidades comunes para todas las actividades
 * de la aplicación UNIGO.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyAppTheme();
        applyAppLocale();
        super.onCreate(savedInstanceState);
        // Ya no llamamos a initViews() aquí para permitir que las actividades hijas
        // establezcan su setContentView() primero
    }

    /**
     * Método abstracto que cada actividad debe implementar para
     * inicializar sus vistas específicas
     */
    protected abstract void initViews();

    /**
     * Método para mostrar un Toast corto
     * 
     * @param message Mensaje a mostrar
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Método para mostrar un Toast largo
     * 
     * @param message Mensaje a mostrar
     */
    protected void showLongToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Método para mostrar un Snackbar
     * 
     * @param view View donde se mostrará el Snackbar
     * @param message Mensaje a mostrar
     */
    protected void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Método para mostrar un Snackbar con acción
     * 
     * @param view View donde se mostrará el Snackbar
     * @param message Mensaje a mostrar
     * @param actionText Texto del botón de acción
     * @param action Acción a realizar cuando se pulsa el botón
     */
    protected void showSnackbarWithAction(View view, String message, String actionText, View.OnClickListener action) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(actionText, action)
                .show();
    }

    /**
     * Método para navegar a otra actividad
     * 
     * @param targetActivity Clase de la actividad de destino
     */
    protected void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }

    /**
     * Método para navegar a otra actividad con parámetros
     * 
     * @param targetActivity Clase de la actividad de destino
     * @param bundle Parámetros a pasar a la actividad
     */
    protected void navigateTo(Class<?> targetActivity, Bundle bundle) {
        Intent intent = new Intent(this, targetActivity);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * Método para cerrar la actividad actual
     */
    protected void finishActivity() {
        finish();
    }

    /**
     * Método para solicitar permisos en tiempo de ejecución
     * 
     * @param permissions Permisos a solicitar
     * @param requestCode Código de solicitud
     */
    protected void requestRuntimePermissions(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    /**
     * Método para mostrar un diálogo de confirmación
     * 
     * @param title Título del diálogo
     * @param message Mensaje del diálogo
     * @param positiveAction Acción para el botón positivo
     * @param negativeAction Acción para el botón negativo
     */
    protected void showConfirmationDialog(String title, String message, 
                                         Runnable positiveAction, 
                                         Runnable negativeAction) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, id) -> {
                    if (positiveAction != null) {
                        positiveAction.run();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, id) -> {
                    if (negativeAction != null) {
                        negativeAction.run();
                    }
                });
        builder.create().show();
    }

    /**
     * Método para gestionar la pila de fragmentos
     * 
     * @param containerId ID del contenedor donde se añadirá el fragmento
     * @param fragment Fragmento a añadir
     * @param tag Tag del fragmento
     * @param addToBackStack Si se debe añadir a la pila de retroceso
     */
    protected void replaceFragment(int containerId, androidx.fragment.app.Fragment fragment, 
                                  String tag, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction transaction = 
                fragmentManager.beginTransaction();
        
        transaction.replace(containerId, fragment, tag);
        
        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }
        
        transaction.commit();
    }

    /**
     * Método para limpiar la pila de fragmentos
     */
    protected void clearFragmentBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
            fm.popBackStack();
        }
    }

    /**
     * Método para verificar la conexión a Internet
     * 
     * @return true si hay conexión, false en caso contrario
     */
    protected boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = 
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Método para reiniciar la app con las preferencias seleccionadas
     */
    public void restartAppWithSettings() {
        Intent mainIntent = new Intent(this, MainActivity.class);

        // Volver al fragmento de settings
        mainIntent.putExtra("startFragment", MainActivity.FragmentType.SETTINGS.ordinal());
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void applyAppLocale() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String langCode = prefs.getString("idioma", "en");

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();

        if (!locale.equals(config.getLocales().get(0))) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }

    private void applyAppTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = prefs.getString("tema", "system");

        switch (themePref) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

}