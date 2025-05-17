package com.ehunzango.unigo.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.services.FirebaseAuthService;
import com.ehunzango.unigo.services.RouteService;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Actividad de inicio que muestra una animación de carga, verifica el estado
 * de autenticación del usuario y navega a la pantalla correspondiente.
 */
public class SplashActivity extends BaseActivity {

    private static final String TAG = "SplashActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 31;
    private CardView centerCircle;
    private ImageView[] logos; // Array para todos los logos
    private static final int LOGO_TYPES = 3; // Tipos de logos (opendata, ehu, ehunzango)
    private static final int INSTANCES_PER_LOGO = 3; // Instancias de cada tipo de logo
    private static final int TOTAL_LOGOS = LOGO_TYPES * INSTANCES_PER_LOGO; // Total de logos
    private ImageView slideUpArrow;
    private View slideUpContainer;
    private GestureDetectorCompat gestureDetector;

    private boolean isAppReady = false;
    private static final int MIN_SPLASH_TIME = 2000; // 2 segundos mínimos para la pantalla splash
    private AtomicBoolean authCheckComplete = new AtomicBoolean(false);

    // Variables para el movimiento flotante
    private int screenWidth;
    private int screenHeight;
    private Random random = new Random();

    // Vectores de velocidad para cada logo
    private float[] velocityX;
    private float[] velocityY;

    // Constantes para el movimiento
    private static final float MIN_VELOCITY = 2.5f;
    private static final float MAX_VELOCITY = 7.5f;
    private static final float ROTATION_FACTOR = 0.2f;

    // Handler y Runnable para la animación
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable animationRunnable;

    // FirebaseAuthService
    private FirebaseAuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Inicializar servicio de autenticación
        authService = FirebaseAuthService.getInstance();

        // Inicializar arrays para velocidades
        velocityX = new float[TOTAL_LOGOS];
        velocityY = new float[TOTAL_LOGOS];

        // Inicializar array de logos
        logos = new ImageView[TOTAL_LOGOS];

        // Llamar a initViews después de setContentView para que las vistas ya estén infladas
        initViews();

        // Solicitar permisos de ubicación
        checkLocationPermission();

        // Inicializar servicio de rutas
        RouteService routeService = RouteService.getInstance();
        routeService.initialize(this, new RouteService.RouteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Servicio de rutas inicializado correctamente");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al inicializar servicio de rutas: " + errorMessage);
            }
        });

        // Iniciar verificación de autenticación
        checkAuthentication();
    }

    private boolean checkLocationPermission() {
        // Si el permiso está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permiso de ubicación ya concedido");
            return true;
        }
        
        // Si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Se requieren permisos de ubicación");
            builder.setMessage("Para mostrar tu ubicación en el mapa y calcular rutas, necesitamos acceso a tu ubicación.");
            
            LinearLayout layoutName = new LinearLayout(getBaseContext());
            layoutName.setOrientation(LinearLayout.VERTICAL);
            
            builder.setView(layoutName);
            
            builder.setPositiveButton("Abrir ajustes", (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            });
            
            builder.setNegativeButton("Cancelar", (dialog, which) -> {
                dialog.dismiss();
            });
            
            builder.show();
        } else {
            // Solicitar permiso directamente
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de ubicación concedido");
            } else {
                Log.d(TAG, "Permiso de ubicación denegado");
            }
        }
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
        slideUpArrow = findViewById(R.id.slideUpArrow);
        slideUpContainer = findViewById(R.id.slideUpContainer);

        // Definir un tamaño fijo para todos los logos
        int logoSize = 96; // tamaño en dp
        int logoSizePx = (int) (logoSize * getResources().getDisplayMetrics().density);

        // Obtener una referencia al ViewGroup raíz
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);

        // Crear las ImageViews para los logos programáticamente
        for (int i = 0; i < TOTAL_LOGOS; i++) {
            logos[i] = new ImageView(this);

            // Configurar layout para cada logo
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    logoSizePx, logoSizePx);
            logos[i].setLayoutParams(params);
            logos[i].setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Asignar imagen según el tipo de logo (se repiten en grupos de INSTANCES_PER_LOGO)
            int logoType = i / INSTANCES_PER_LOGO;
            switch (logoType) {
                case 0: // Primer tipo (opendata)
                    logos[i].setImageResource(R.drawable.ic_opendata);
                    break;
                case 1: // Segundo tipo (ehu)
                    logos[i].setImageResource(R.drawable.ic_ehu);
                    break;
                case 2: // Tercer tipo (ehunzango)
                default:
                    logos[i].setImageResource(R.drawable.ic_ehunzango);
                    break;
            }

            // Hacer visible el logo
            logos[i].setVisibility(View.VISIBLE);

            // Añadir el logo al layout
            rootView.addView(logos[i]);
        }

        // Obtener dimensiones de la pantalla cuando el layout esté listo
        final View rootContentView = findViewById(android.R.id.content);
        rootContentView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Eliminar el listener para que no se llame múltiples veces
                        rootContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        // Obtener dimensiones de la pantalla
                        screenWidth = rootContentView.getWidth();
                        screenHeight = rootContentView.getHeight();

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
    }

    /**
     * Verifica el estado de autenticación y navega a la pantalla correspondiente
     * después de un tiempo mínimo para mostrar la pantalla de splash.
     */
    private void checkAuthentication() {
        // Establecer tiempo mínimo para la pantalla splash
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Marcar que el tiempo mínimo ha pasado y proceder si la verificación está lista
            isAppReady = true;
            proceedIfReady();
        }, MIN_SPLASH_TIME);

        // Verificar estado de autenticación
        FirebaseUser currentUser = authService.getCurrentUser();

        if (currentUser != null) {
            // Usuario existe, verificar que sigue siendo válido
            Log.d(TAG, "Usuario existente, verificando validez: " + currentUser.getUid());

            currentUser.reload()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Usuario recargado con éxito: " + currentUser.getUid());
                        // Usuario válido, marcar verificación como completada
                        authCheckComplete.set(true);
                        proceedIfReady();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al recargar usuario: " + e.getMessage());
                        // Usuario inválido (cuenta eliminada, deshabilitada, etc.)
                        authService.logout();
                        authCheckComplete.set(true);
                        proceedIfReady();
                    });
        } else {
            // No hay usuario autenticado
            Log.d(TAG, "No hay usuario autenticado");
            authCheckComplete.set(true);
            proceedIfReady();
        }
    }

    /**
     * Procede a navegar a la siguiente pantalla si todas las comprobaciones están listas
     */
    private void proceedIfReady() {
        if (isAppReady && authCheckComplete.get()) {
            // Verificar si hay un usuario válido
            if (authService.isUserLoggedIn()) {
                // Usuario autenticado, ir a MainActivity
                Log.d(TAG, "Navegando a MainActivity");
                navigateToMainActivity();
            } else {
                // No hay usuario, mostrar flecha para continuar a login
                Log.d(TAG, "Mostrando opción para ir a LoginActivity");
                showContinueOption();
            }
        }
    }

    /**
     * Muestra la flecha para continuar hacia la pantalla de login
     */
    private void showContinueOption() {
        // Mostrar la flecha con animación
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(slideUpArrow, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();

        // Animación para indicar deslizamiento hacia arriba
        startSlideUpHintAnimation();
    }

    private void initLogoPositions() {
        // Posicionar logos en lugares aleatorios de la pantalla
        // pero evitando el círculo central
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int safeRadius = centerCircle.getWidth() / 2 + 50; // Radio de seguridad alrededor del centro

        for (int i = 0; i < TOTAL_LOGOS; i++) {
            ImageView logo = logos[i];
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
        for (int i = 0; i < TOTAL_LOGOS; i++) {
            // Velocidad entre MIN_VELOCITY y MAX_VELOCITY (positiva o negativa)
            velocityX[i] = MIN_VELOCITY + random.nextFloat() * (MAX_VELOCITY - MIN_VELOCITY);
            velocityY[i] = MIN_VELOCITY + random.nextFloat() * (MAX_VELOCITY - MIN_VELOCITY);

            // 50% de probabilidad de que la velocidad sea negativa
            if (random.nextBoolean()) velocityX[i] = -velocityX[i];
            if (random.nextBoolean()) velocityY[i] = -velocityY[i];
        }
    }

    private void startFloatingAnimation() {
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                // Mover cada logo según su velocidad
                for (int i = 0; i < TOTAL_LOGOS; i++) {
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
        // Navegar a LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        finish();
    }

    private void navigateToMainActivity() {
        // Navegar directamente a MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}