package com.ehunzango.unigo.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.activities.BaseActivity;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private ListenerFragmentSettings listener;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Idioma: si no hay valor, usar el del sistema
        if (!sharedPreferences.contains("idioma")) {
            String defaultLang = Locale.getDefault().getLanguage();
            if (defaultLang.equals("es") || defaultLang.equals("eu") || defaultLang.equals("en")) {
                editor.putString("idioma", defaultLang);
            } else {
                editor.putString("idioma", "es"); // fallback
            }
        }

        // Tema: si no hay valor, usar el del sistema
        if (!sharedPreferences.contains("tema")) {
            editor.putString("tema", "system");
        }

        editor.apply();

        addPreferencesFromResource(R.xml.pref_config);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                        CONEXION CON MAIN ACTIVITY                        |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SettingsFragment.ListenerFragmentSettings) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("La clase " + context.toString() + " debe implementar el listener");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        System.out.println("Preference changed: " + key);

        assert key != null;
        switch (key) {
            case "idioma":
                String langCode = sharedPreferences.getString(key, "es");
                setAppLocale(langCode);
                break;
            case "tema":
                String themePref = sharedPreferences.getString(key, "system");
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
                break;
        }
    }

    private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        // Update the context
        requireActivity().getBaseContext().getResources().updateConfiguration(
                config,
                requireActivity().getBaseContext().getResources().getDisplayMetrics()
        );

        // Restart activity to apply changes
        ((BaseActivity) requireActivity()).restartAppWithSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public interface ListenerFragmentSettings {

    }

}