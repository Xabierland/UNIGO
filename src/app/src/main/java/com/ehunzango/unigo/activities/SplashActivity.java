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
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.cardview.widget.CardView;
import androidx.core.view.GestureDetectorCompat;

import com.ehunzango.unigo.R;

import java.util.Random;

/**
 * Actividad de inicio que muestra una animación de carga y permite
 * deslizar hacia arriba para acceder a la pantalla de login.
 */
public class SplashActivity extends BaseActivity {

    private CardView centerCircle;
    private ImageView logo1, logo2, logo3;
    private ImageView slideUpArrow;
    private View slideUpContainer;
    private GestureDetectorCompat gestureDetector;
    
    private boolean isAppReady = false;
    private static final int LOADING_DELAY = 3000; // 3 segundos para simulación de carga
    
    // Variables para el movimiento flotante
    private int screenWidth;
    private int screenHeight;
    private Random random = new Random();
    
    // Vectores de velocidad para cada logo
    private float[] velocityX = new float[3];
    private float[] velocityY = new float[3];
    
    // Constantes para el movimiento
    private static final float MIN_VELOCITY = 2.5f;
    private static final float MAX_VELOCITY = 7.5f;
    private static final float ROTATION_FACTOR = 0.2f;
    
    // Handler y Runnable para la animación
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable animationRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Llamar a initViews después de setContentView para que las vistas ya estén infladas
        initViews();
    }
    
    @Override
    protected void onDestroy() {
        // Detener la animación cuando la actividad se destruye
        animationHandler.removeCallbacks(animationRunnable);
        super.onDestroy();
    }
    
    @Override
    protected void initViews() {
        centerCircle = findViewById(R.id.centerCircle);
        logo1 = findViewById(R.id.logo1);
        logo2 = findViewById(R.id.logo2);
        logo3 = findViewById(R.id.logo3);
        slideUpArrow = findViewById(R.id.slideUpArrow);
        slideUpContainer = findViewById(R.id.slideUpContainer);

        // Definir un tamaño fijo para todos los logos
        int logoSize = 96; // tamaño en dp
        int logoSizePx = (int) (logoSize * getResources().getDisplayMetrics().density);

        // Configurar layout para cada logo
        for (ImageView logo : new ImageView[]{logo1, logo2, logo3}) {
            if (logo != null) {
                logo.getLayoutParams().width = logoSizePx;
                logo.getLayoutParams().height = logoSizePx;
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                logo.requestLayout();
            }
        }

        // Por ahora, usamos placeholders
        logo1.setImageResource(R.drawable.ic_opendata);
        logo2.setImageResource(R.drawable.ic_ehu);
        logo3.setImageResource(R.drawable.ic_ehunzango);
        
        // Mostrar logos
        logo1.setVisibility(View.VISIBLE);
        logo2.setVisibility(View.VISIBLE);
        logo3.setVisibility(View.VISIBLE);
        
        // Obtener dimensiones de la pantalla cuando el layout esté listo
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Eliminar el listener para que no se llame múltiples veces
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    
                    // Obtener dimensiones de la pantalla
                    screenWidth = rootView.getWidth();
                    screenHeight = rootView.getHeight();
                    
                    // Iniciar animaciones una vez que tenemos las dimensiones
                    initLogoPositions();
                    initLogoVelocities();
                    startFloatingAnimation();
                }
            }
        );
        
        // Iniciar animación del círculo central
        startPulseAnimation();
        
        // Configurar detector de gestos
        setupGestureDetector();
        
        // Simular tiempo de carga
        simulateLoading();
    }
    
    private void initLogoPositions() {
        // Posicionar logos en lugares aleatorios de la pantalla
        // pero evitando el círculo central
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int safeRadius = centerCircle.getWidth() / 2 + 50; // Radio de seguridad alrededor del centro
        
        for (ImageView logo : new ImageView[]{logo1, logo2, logo3}) {
            if (logo != null) {
                float x, y;
                float distance;
                
                // Generar posición aleatoria que no esté demasiado cerca del centro
                do {
                    x = random.nextInt(screenWidth - logo.getWidth());
                    y = random.nextInt(screenHeight - logo.getHeight());
                    
                    // Calcular distancia al centro
                    float dx = x + logo.getWidth()/2 - centerX;
                    float dy = y + logo.getHeight()/2 - centerY;
                    distance = (float) Math.sqrt(dx * dx + dy * dy);
                    
                } while (distance < safeRadius);
                
                logo.setX(x);
                logo.setY(y);
                
                // Rotación inicial aleatoria
                logo.setRotation(random.nextInt(360));
            }
        }
    }
    
    private void initLogoVelocities() {
        // Iniciar con velocidades aleatorias
        for (int i = 0; i < 3; i++) {
            // Velocidad entre MIN_VELOCITY y MAX_VELOCITY (positiva o negativa)
            velocityX[i] = MIN_VELOCITY + random.nextFloat() * (MAX_VELOCITY - MIN_VELOCITY);
            velocityY[i] = MIN_VELOCITY + random.nextFloat() * (MAX_VELOCITY - MIN_VELOCITY);
            
            // 50% de probabilidad de que la velocidad sea negativa
            if (random.nextBoolean()) velocityX[i] = -velocityX[i];
            if (random.nextBoolean()) velocityY[i] = -velocityY[i];
        }
    }
    
    private void startFloatingAnimation() {
        final ImageView[] logos = {logo1, logo2, logo3};
        
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                // Mover cada logo según su velocidad
                for (int i = 0; i < logos.length; i++) {
                    ImageView logo = logos[i];
                    if (logo == null) continue;
                    
                    // Obtener posición actual
                    float x = logo.getX();
                    float y = logo.getY();
                    
                    // Calcular nueva posición
                    float newX = x + velocityX[i];
                    float newY = y + velocityY[i];
                    
                    // Comprobar colisión con los bordes
                    // Borde derecho
                    if (newX + logo.getWidth() > screenWidth) {
                        velocityX[i] = -Math.abs(velocityX[i]); // Invertir y garantizar que sea negativa
                        newX = screenWidth - logo.getWidth();
                    }
                    // Borde izquierdo
                    else if (newX < 0) {
                        velocityX[i] = Math.abs(velocityX[i]); // Invertir y garantizar que sea positiva
                        newX = 0;
                    }
                    
                    // Borde inferior
                    if (newY + logo.getHeight() > screenHeight) {
                        velocityY[i] = -Math.abs(velocityY[i]); // Invertir y garantizar que sea negativa
                        newY = screenHeight - logo.getHeight();
                    }
                    // Borde superior
                    else if (newY < 0) {
                        velocityY[i] = Math.abs(velocityY[i]); // Invertir y garantizar que sea positiva
                        newY = 0;
                    }
                    
                    // Comprobar colisión con el círculo central
                    float centerX = centerCircle.getX() + centerCircle.getWidth() / 2;
                    float centerY = centerCircle.getY() + centerCircle.getHeight() / 2;
                    float logoX = newX + logo.getWidth() / 2;
                    float logoY = newY + logo.getHeight() / 2;
                    
                    float dx = logoX - centerX;
                    float dy = logoY - centerY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    
                    // Radio del círculo central + radio del logo
                    float minDistance = (centerCircle.getWidth() / 2) + (logo.getWidth() / 2);
                    
                    if (distance < minDistance) {
                        // Calcular vector normal de colisión
                        float nx = dx / distance;
                        float ny = dy / distance;
                        
                        // Reflejar vector de velocidad sobre el vector normal
                        float dotProduct = 2 * (velocityX[i] * nx + velocityY[i] * ny);
                        velocityX[i] = velocityX[i] - dotProduct * nx;
                        velocityY[i] = velocityY[i] - dotProduct * ny;
                        
                        // Ajustar posición para evitar que se quede dentro del círculo
                        float correctionDistance = minDistance - distance;
                        newX = newX + correctionDistance * nx;
                        newY = newY + correctionDistance * ny;
                    }
                    
                    // Aplicar nueva posición
                    logo.setX(newX);
                    logo.setY(newY);
                    
                    // Aplicar rotación basada en la velocidad
                    float currentRotation = logo.getRotation();
                    float rotationChange = (Math.abs(velocityX[i]) + Math.abs(velocityY[i])) * ROTATION_FACTOR;
                    logo.setRotation(currentRotation + rotationChange);
                }
                
                // Programar la próxima iteración
                animationHandler.postDelayed(this, 16); // ~60fps
            }
        };
        
        // Iniciar la animación
        animationHandler.post(animationRunnable);
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
        // Detener la animación antes de navegar
        animationHandler.removeCallbacks(animationRunnable);
        
        // Falta implementar LoginActivity
        //navigateTo(LoginActivity.class);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        finish();
    }
}
