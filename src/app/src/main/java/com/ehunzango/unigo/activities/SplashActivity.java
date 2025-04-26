package com.ehunzango.unigo.activities;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.cardview.widget.CardView;
import androidx.core.view.GestureDetectorCompat;

import com.ehunzango.unigo.R;

/**
 * Actividad de inicio que muestra una animación de carga y permite
 * deslizar hacia arriba para acceder a la pantalla de login.
 */
public class SplashActivity extends BaseActivity {

    private CardView centerCircle;
    private ImageView logo1, logo2, logo3, logo4;
    private ImageView slideUpArrow;
    private View slideUpContainer;
    private GestureDetectorCompat gestureDetector;
    
    private boolean isAppReady = false;
    private static final int LOADING_DELAY = 3000; // 3 segundos para simulación de carga
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Ocultar la barra de acción
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }
    
    @Override
    protected void initViews() {
        centerCircle = findViewById(R.id.centerCircle);
        logo1 = findViewById(R.id.logo1);
        logo2 = findViewById(R.id.logo2);
        logo3 = findViewById(R.id.logo3);
        logo4 = findViewById(R.id.logo4);
        slideUpArrow = findViewById(R.id.slideUpArrow);
        slideUpContainer = findViewById(R.id.slideUpContainer);
        
        // Configurar los logos (se deberían agregar recursos en drawable)
        // Por ahora, usamos placeholders
        logo1.setImageResource(android.R.drawable.ic_menu_compass);
        logo2.setImageResource(android.R.drawable.ic_menu_directions);
        logo3.setImageResource(android.R.drawable.ic_menu_mapmode);
        logo4.setImageResource(android.R.drawable.ic_menu_mylocation);
        
        // Mostrar logos
        logo1.setVisibility(View.VISIBLE);
        logo2.setVisibility(View.VISIBLE);
        logo3.setVisibility(View.VISIBLE);
        logo4.setVisibility(View.VISIBLE);
        
        // Iniciar animaciones
        startOrbitAnimations();
        startPulseAnimation();
        
        // Configurar detector de gestos
        setupGestureDetector();
        
        // Simular tiempo de carga
        simulateLoading();
    }
    
    private void startOrbitAnimations() {
        // Calcular las posiciones y comenzar las rotaciones
        centerCircle.post(() -> {
            int centerX = (int) (centerCircle.getX() + (float) centerCircle.getWidth() / 2);
            int centerY = (int) (centerCircle.getY() + (float) centerCircle.getHeight() / 2);
            float orbitRadius = 200f; // Radio de la órbita
            
            // Posicionar los logos alrededor del círculo central
            positionLogoInOrbit(logo1, centerX, centerY, 0, orbitRadius);
            positionLogoInOrbit(logo2, centerX, centerY, 90, orbitRadius);
            positionLogoInOrbit(logo3, centerX, centerY, 180, orbitRadius);
            positionLogoInOrbit(logo4, centerX, centerY, 270, orbitRadius);
            
            // Iniciar animaciones de órbita
            startOrbitAnimation(logo1, 15000, true);  // 15 segundos por revolución
            startOrbitAnimation(logo2, 20000, true);  // 20 segundos por revolución
            startOrbitAnimation(logo3, 25000, true);  // 25 segundos por revolución
            startOrbitAnimation(logo4, 18000, true);  // 18 segundos por revolución
        });
    }
    
    private void positionLogoInOrbit(ImageView logo, float centerX, float centerY, 
                                      float angleDegrees, float radius) {
        float angleRadians = (float) Math.toRadians(angleDegrees);
        
        // Calcular la posición X e Y basada en el ángulo y radio
        float x = centerX + radius * (float) Math.cos(angleRadians) - logo.getWidth() / 2;
        float y = centerY + radius * (float) Math.sin(angleRadians) - logo.getHeight() / 2;
        
        // Establecer la posición
        logo.setX(x);
        logo.setY(y);
        logo.setTag(R.id.tag_orbit_angle, angleDegrees); // Guardar el ángulo para animación
        logo.setTag(R.id.tag_orbit_radius, radius); // Guardar el radio para animación
    }
    
    private void startOrbitAnimation(final ImageView logo, long duration, final boolean clockwise) {
        final float centerX = centerCircle.getX() + centerCircle.getWidth() / 2;
        final float centerY = centerCircle.getY() + centerCircle.getHeight() / 2;
        final float radius = (float) logo.getTag(R.id.tag_orbit_radius);
        
        ValueAnimator animator = ValueAnimator.ofFloat(0, 360);
        animator.setDuration(duration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        
        animator.addUpdateListener(animation -> {
            float angle = (float) animation.getAnimatedValue();
            if (!clockwise) {
                angle = 360 - angle;
            }
            
            float angleRadians = (float) Math.toRadians(angle);
            float x = centerX + radius * (float) Math.cos(angleRadians) - logo.getWidth() / 2;
            float y = centerY + radius * (float) Math.sin(angleRadians) - logo.getHeight() / 2;
            
            logo.setX(x);
            logo.setY(y);
            
            // Rotar el logo sobre su propio eje
            logo.setRotation(angle);
        });
        
        animator.start();
    }
    
    private void startPulseAnimation() {
        // Animación pulsante para el círculo central
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f);
        
        ObjectAnimator pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                centerCircle, scaleX, scaleY);
        
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.start();
    }
    
    private void setupGestureDetector() {
        // Inicialmente ocultar la flecha
        slideUpArrow.setAlpha(0f);
        
        // Detector de gestos para el deslizamiento hacia arriba
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (!isAppReady) return false;
                
                float diffY = e1.getY() - e2.getY();
                // Si el deslizamiento es hacia arriba con suficiente velocidad
                if (diffY > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    navigateToLogin();
                    return true;
                }
                return false;
            }
        });
        
        // Aplicar detector de gestos a toda la vista
        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }
    
    private void simulateLoading() {
        // Simular tiempo de carga de la aplicación
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isAppReady = true;
            
            // Mostrar la flecha con animación
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(slideUpArrow, "alpha", 0f, 1f);
            fadeIn.setDuration(500);
            fadeIn.start();
            
            // Animación para indicar deslizamiento hacia arriba
            startSlideUpHintAnimation();
            
        }, LOADING_DELAY);
    }
    
    private void startSlideUpHintAnimation() {
        // Animación para sugerir el deslizamiento hacia arriba
        ObjectAnimator moveAnimator = ObjectAnimator.ofFloat(
                slideUpArrow, "translationY", 0f, -20f, 0f);
        moveAnimator.setDuration(1500);
        moveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        moveAnimator.setInterpolator(new LinearInterpolator());
        moveAnimator.start();
    }
    
    private void navigateToLogin() {
        // Falta implementar LoginActivity
        //navigateTo(MainActivity.class);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        finish();
    }
}