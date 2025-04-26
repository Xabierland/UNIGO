package com.ehunzango.unigo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.activities.MainActivity;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Fragmento para mostrar y gestionar el perfil del usuario.
 * Contiene la opción de cierre de sesión.
 */
public class ProfileFragment extends Fragment {

    private TextView userNameTextView;
    private TextView userEmailTextView;
    private CircleImageView userAvatarImageView;
    private Button logoutButton;
    
    private FirebaseAuthService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar servicio de autenticación
        authService = FirebaseAuthService.getInstance();
        
        // Inicializar vistas
        userNameTextView = view.findViewById(R.id.user_name_text);
        userEmailTextView = view.findViewById(R.id.user_email_text);
        userAvatarImageView = view.findViewById(R.id.user_avatar_image);
        logoutButton = view.findViewById(R.id.logout_button);
        
        // Cargar datos del usuario
        loadUserData();
        
        // Configurar listener para botón de cierre de sesión
        logoutButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null) {
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
        }
    }
}