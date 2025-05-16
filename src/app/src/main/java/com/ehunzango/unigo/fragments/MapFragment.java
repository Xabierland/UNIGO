package com.ehunzango.unigo.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ehunzango.unigo.R;
import com.ehunzango.unigo.adapters.ImageSpinnerAdapter;
import com.ehunzango.unigo.adapters.SpinnerImageItem;
import com.ehunzango.unigo.router.RouteFinder;
import com.ehunzango.unigo.router.adapters.NETEXAdapter;
import com.ehunzango.unigo.router.entities.Line;
import com.ehunzango.unigo.router.entities.Node;
import com.ehunzango.unigo.services.RouteService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MapFragment extends Fragment {
    private static final String TAG = "MapFragment"; // Para facilitar la depuración

    private ListenerFragmentMap listener;

    private GoogleMap mapGoogle;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userPosition;
    private LocationCallback locationCallback;
    private boolean isInNavigationMode = false;
    private ArrayList<LatLng> currentRoutePoints;
    private Marker userMarker;
    private boolean mapReady = false; // Flag para saber si el mapa ya está preparado
    private Polyline currentRoute; // Referencia a la ruta actual

    // Ubicación por defecto - Campus de Álava (por si no se puede obtener la ubicación real)
    private static final LatLng DEFAULT_LOCATION = new LatLng(42.8395, -2.6724);

    private HashMap<FacultyIdentifier, FacultyInfo> facultyHashMap = new HashMap<>();
    private enum FacultyIdentifier {
        Escuela_Ingenieria_Vitoria_Gasteiz,
        Facultad_Economia_Empresa,
        Facultad_Educacion_Deporte,
        Facultad_Farmacia,
        Facultad_Letras,
        Facultad_Relaciones_Laborales_Trabajo_Social,
        Unidad_Docente_Medicina,
        Aulas_Experiencia_Alava
    }
    private class FacultyInfo {
        public FacultyIdentifier id;
        public String name;
        private MarkerOptions markerOptions;
        public int iconId;
        public LatLng position;
        public Marker marker;
        public FacultyInfo(FacultyIdentifier id, String name, int iconId, LatLng position) {
            this.id = id;
            this.name = name;
            this.iconId = iconId;
            this.position = position;
            this.markerOptions = null;
        }

        public MarkerOptions getMarkerOptions() {
            if(markerOptions == null) {
                this.markerOptions = new MarkerOptions()
                        .icon(getBitmapFromVectorDrawable(iconId, Color.RED))
                        .position(position)
                        .title(name);
            }
            return markerOptions;
        }
    }
    private List<FacultyInfo> dropDownOrder;

    private enum TransportType {
        WALK,
        BIKE,
        BUS,
        CAR
    }

    // Selection
    private FacultyIdentifier selectedFaculty;
    private TransportType selectedTransport;
    private FloatingActionButton startNavigationButton;
    private FloatingActionButton calculateRouteButton;

    public MapFragment() {}

    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate llamado");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView llamado");
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated llamado");

        initializeFaculties();

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        createLocationCallback();

        // Pedir permisos de ubicación y obtener ubicación inicial
        if (hasLocationPermission()) {
            Log.d(TAG, "Ya tenemos permisos de ubicación, obteniendo ubicación...");
            obtainActualLocation(false);
        } else {
            Log.d(TAG, "Solicitando permisos de ubicación...");
            if (listener != null) {
                listener.askLocationPermission();
            }
        }

        // Obtener el fragmento del mapa e inicializarlo
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.google_map_fragment);

        if (mapFragment != null) {
            Log.d(TAG, "Inicializando mapa...");
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {
                    Log.d(TAG, "Mapa listo");
                    mapGoogle = googleMap;
                    mapReady = true;

                    // Configuración del mapa
                    mapGoogle.getUiSettings().setMapToolbarEnabled(false);
                    mapGoogle.getUiSettings().setZoomControlsEnabled(true);
                    mapGoogle.getUiSettings().setCompassEnabled(true);

                    mapGoogle.setOnMarkerClickListener(marker -> {
                        marker.showInfoWindow();
                        return true;
                    });

                    // Si ya tenemos posición, mostrarla
                    if (userPosition != null) {
                        Log.d(TAG, "Mostrando posición del usuario en el mapa");
                        updateUserPositionOnMap();
                        mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f));
                    } else {
                        // Intentar obtener posición
                        obtainActualLocation(true);

                        // Mientras tanto, centrar en una ubicación por defecto
                        Log.d(TAG, "Centrando mapa en ubicación por defecto");
                        mapGoogle.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 14f));
                    }

                    // Añadir marcadores de facultades
                    Collection<FacultyInfo> faculties = facultyHashMap.values();
                    for (FacultyInfo faculty : faculties) {
                        Marker m = mapGoogle.addMarker(faculty.getMarkerOptions());
                        faculty.marker = m;
                    }

                    // Dibujar ruta de prueba para verificar que se puede dibujar
                    drawTestRoute();
                }
            });
        } else {
            Log.e(TAG, "No se encontró el fragmento del mapa");
        }

        // Configurar dropdowns
        setUpDropDownMenu(view);
        setUpTansportDropdown(view);

        // Inicializar botones
        calculateRouteButton = view.findViewById(R.id.button_calculate_route);
        calculateRouteButton.setOnClickListener(v -> calculateRoute());

        startNavigationButton = view.findViewById(R.id.button_start_navigation);
        startNavigationButton.setOnClickListener(v -> startNavigation());
        startNavigationButton.setVisibility(View.GONE); // Oculto inicialmente
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void drawTestRoute() {
        // Dibujar una ruta de prueba para verificar que se puede dibujar en el mapa
        if (mapGoogle != null) {
            ArrayList<LatLng> testPoints = new ArrayList<>();
            // Usar puntos cercanos al campus
            testPoints.add(new LatLng(42.8395, -2.6724));
            testPoints.add(new LatLng(42.8400, -2.6730));
            testPoints.add(new LatLng(42.8405, -2.6720));

            PolylineOptions options = new PolylineOptions()
                    .addAll(testPoints)
                    .color(Color.RED)
                    .width(8);

            mapGoogle.addPolyline(options);
            Log.d(TAG, "Ruta de prueba dibujada");

            // Si tenemos ubicación, dibujar un marcador
            if (userPosition != null) {
                updateUserPositionOnMap();
            } else {
                // Usar ubicación por defecto para probar
                userPosition = DEFAULT_LOCATION;
                updateUserPositionOnMap();
                Log.d(TAG, "Marcador de prueba añadido: " + userPosition);
            }
        } else {
            Log.e(TAG, "No se pudo dibujar ruta de prueba: mapa no inicializado");
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "locationResult es null");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Nueva ubicación: " + location.getLatitude() + ", " + location.getLongitude());

                    // Actualizar posición del usuario
                    userPosition = new LatLng(location.getLatitude(), location.getLongitude());

                    // Si estamos en modo navegación, actualizar la posición en el mapa
                    if (isInNavigationMode && mapReady) {
                        updateUserPositionOnMap();
                        mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 18f));

                        // Si estamos cerca del destino, finalizar la navegación
                        if (currentRoutePoints != null && !currentRoutePoints.isEmpty()) {
                            LatLng destination = currentRoutePoints.get(currentRoutePoints.size() - 1);
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    userPosition.latitude, userPosition.longitude,
                                    destination.latitude, destination.longitude,
                                    results);

                            if (results[0] < 50) { // 50 metros del destino
                                Toast.makeText(getContext(), "¡Has llegado a tu destino!", Toast.LENGTH_LONG).show();
                                stopNavigation();
                            }
                        }
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Sin permisos para actualizaciones frecuentes");
            if (listener != null) {
                listener.askLocationPermission();
            }
            return;
        }

        Log.d(TAG, "Iniciando actualizaciones frecuentes de ubicación");

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000) // 10 segundos
                .setFastestInterval(5000) // 5 segundos mínimo
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Error al solicitar actualizaciones: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "Deteniendo actualizaciones de ubicación");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void initializeFaculties() {
        facultyHashMap = new HashMap<>();
        FacultyInfo info = null;

        info = new FacultyInfo(
                FacultyIdentifier.Escuela_Ingenieria_Vitoria_Gasteiz,
                getString(R.string.engineer_school),
                R.drawable.engineering_24px,
                new LatLng(42.83921784390094, -2.6744744038759203));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Economia_Empresa,
                getString(R.string.economy_school),
                R.drawable.savings_24px,
                new LatLng(42.83758845850613, -2.668680518146171));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Educacion_Deporte,
                getString(R.string.sports_school),
                R.drawable.sports_tennis_24px,
                new LatLng(42.83933934794871, -2.6744481633876007));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Farmacia,
                getString(R.string.pharmacy_school),
                R.drawable.mixture_med_24px,
                new LatLng(42.84064230072461, -2.6718272245113464));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Letras,
                getString(R.string.letter_school),
                R.drawable.history_edu_24px,
                new LatLng(42.8403414326549, -2.670404675504716));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Relaciones_Laborales_Trabajo_Social,
                getString(R.string.relations_school),
                R.drawable.handshake_24px,
                new LatLng(42.8400093266957, -2.670331999775272));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Unidad_Docente_Medicina,
                getString(R.string.medicine_school),
                R.drawable.medical_services_24px,
                new LatLng(42.8390455018132, -2.670360696479438));
        facultyHashMap.put(info.id, info);
    }

    private void setUpDropDownMenu(View view) {
        Spinner dropDownMenu = view.findViewById(R.id.spinner_destinos);

        List<String> items = new ArrayList<>();
        items.add(getString(R.string.select_faculty));
        dropDownOrder = new ArrayList<>();
        Collection<FacultyInfo> faculties = facultyHashMap.values();
        for (FacultyInfo faculty : faculties) {
            items.add(faculty.name);
            dropDownOrder.add(faculty);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
        ) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0; // Desactiva la opcion : Selecciona un destino
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setTextColor(Color.GRAY);
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropDownMenu.setAdapter(adapter);

        dropDownMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    return;
                }
                position = position-1;

                FacultyInfo info = dropDownOrder.get(position);
                selectFaculty(info.id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void selectFaculty(FacultyIdentifier id) {
        FacultyInfo faculty = facultyHashMap.get(id);
        if(faculty == null) {
            return;
        }

        selectedFaculty = id;

        if (mapReady) {
            Collection<FacultyInfo> faculties = facultyHashMap.values();
            for (FacultyInfo f : faculties) {
                if(f.marker != null) {
                    f.marker.setIcon(getBitmapFromVectorDrawable(f.iconId, Color.GRAY));
                }
            }

            if(faculty.marker != null) {
                faculty.marker.setIcon(getBitmapFromVectorDrawable(faculty.iconId, Color.RED));
            }
            mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(faculty.position, 17f), 500, null);
        }
    }

    private BitmapDescriptor getBitmapFromVectorDrawable(int drawableId, int color) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), drawableId);

        if (drawable == null) {
            throw new IllegalArgumentException("Drawable not found");
        }

        drawable.setTint(color);

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void setUpTansportDropdown(View view) {
        Spinner dropDownTransportes = view.findViewById(R.id.spinner_transportes);

        // Crear la lista de ítems para el spinner (con imagen + texto)
        List<SpinnerImageItem> items = new ArrayList<>();

        for (TransportType type : TransportType.values()) {
            items.add(new SpinnerImageItem(getTransportIcon(type)));
        }

        ImageSpinnerAdapter adapter = new ImageSpinnerAdapter(requireContext(), items);

        dropDownTransportes.setAdapter(adapter);

        // Listener del Spinner
        dropDownTransportes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TransportType type = TransportType.values()[position];
                transportSelected(type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void transportSelected(TransportType type) {
        selectedTransport = type;
    }

    private void calculateRoute() {
        // Todo : Idiomas
        if (!mapReady) {
            Toast.makeText(getContext(), "El mapa aún no está listo, espera un momento", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que se haya seleccionado un destino y un tipo de transporte
        if (selectedFaculty == null || selectedTransport == null) {
            Toast.makeText(getContext(), "Por favor, selecciona un destino y un medio de transporte", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que se conozca la posición del usuario
        if (userPosition == null) {
            // Usar posición por defecto si no hay ubicación real
            Log.d(TAG, "No hay ubicación, usando ubicación por defecto");
            userPosition = DEFAULT_LOCATION;
            Toast.makeText(getContext(), "No se pudo obtener tu ubicación, usando ubicación por defecto", Toast.LENGTH_SHORT).show();
        }

        // Obtener la facultad seleccionada
        FacultyInfo faculty = facultyHashMap.get(selectedFaculty);
        if (faculty == null) {
            return;
        }

        Log.d(TAG, "Calculando ruta desde " + userPosition + " hasta " + faculty.position);

        // Limpiar el mapa para eliminar rutas anteriores
        mapGoogle.clear();
        userMarker = null; // Resetear referencia al marcador del usuario
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }

        // Volver a añadir todos los marcadores
        addAllMarkers();

        // Crear y dibujar la ruta según el tipo de transporte
        currentRoutePoints = createRoutePoints(userPosition, faculty.position, selectedTransport);
        int routeColor = getRouteColor(selectedTransport);

        Log.d(TAG, "Dibujando ruta con " + currentRoutePoints.size() + " puntos");
        currentRoute = drawRoute(currentRoutePoints, routeColor);

        if (currentRoute == null) {
            Log.e(TAG, "Error al dibujar ruta");
            Toast.makeText(getContext(), "Error al dibujar la ruta", Toast.LENGTH_SHORT).show();
            return;
        }

        // Añadir icono del medio de transporte en el origen
        drawVehicle(userPosition, selectedTransport, routeColor);

        // Ajustar la cámara para mostrar toda la ruta
        zoomToShowRoute(currentRoutePoints);

        // Mostrar el botón de iniciar navegación
        startNavigationButton.setVisibility(View.VISIBLE);

        // Mostrar mensaje de confirmación
        Toast.makeText(getContext(), "Ruta calculada a " + faculty.name, Toast.LENGTH_SHORT).show();
    }

    private void startNavigation() {
        if (currentRoutePoints == null || currentRoutePoints.isEmpty()) {
            Toast.makeText(getContext(), "Primero debes calcular una ruta", Toast.LENGTH_SHORT).show();
            return;
        }

        isInNavigationMode = true;

        // Cambiar UI para modo navegación
        startNavigationButton.setImageResource(R.drawable.close_24px);
        startNavigationButton.setOnClickListener(v -> stopNavigation());

        // Iniciar actualizaciones de ubicación frecuentes
        startLocationUpdates();

        // Enfocar la cámara en la posición del usuario
        if (userPosition != null && mapReady) {
            mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 18f));
        }

        Toast.makeText(getContext(), "Navegación iniciada", Toast.LENGTH_SHORT).show();
    }

    private void stopNavigation() {
        isInNavigationMode = false;

        // Restaurar UI normal
        startNavigationButton.setImageResource(R.drawable.navigation_24px);
        startNavigationButton.setOnClickListener(v -> startNavigation());

        // Detener actualizaciones frecuentes de ubicación
        stopLocationUpdates();

        if (mapReady) {
            // Borrar la ruta actual
            if (currentRoute != null) {
                currentRoute.remove();
                currentRoute = null;
            }

            // Recalcular la ruta (opcional)
            if (selectedFaculty != null && selectedTransport != null && userPosition != null) {
                FacultyInfo faculty = facultyHashMap.get(selectedFaculty);
                if (faculty != null) {
                    // Redibujar la ruta con un color más tenue o con menos opacidad
                    ArrayList<LatLng> points = createRoutePoints(userPosition, faculty.position, selectedTransport);
                    int routeColor = adjustColorAlpha(getRouteColor(selectedTransport), 128); // 50% de opacidad
                    currentRoute = drawRoute(points, routeColor);

                    // Mostrar toda la ruta
                    zoomToShowRoute(points);
                }
            }
        }

        Toast.makeText(getContext(), "Navegación finalizada", Toast.LENGTH_SHORT).show();
    }

    // Método para ajustar la opacidad de un color
    private int adjustColorAlpha(int color, int alpha) {
        return Color.argb(alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    private void addAllMarkers() {
        if (!mapReady) return;

        // Añadir marcador de posición del usuario
        if (userPosition != null) {
            updateUserPositionOnMap();
        }

        // Añadir marcadores de todas las facultades
        Collection<FacultyInfo> faculties = facultyHashMap.values();
        for (FacultyInfo faculty : faculties) {
            // Color gris para facultades no seleccionadas, rojo para la seleccionada
            int markerColor = (faculty.id == selectedFaculty) ? Color.RED : Color.GRAY;

            MarkerOptions options = new MarkerOptions()
                    .position(faculty.position)
                    .title(faculty.name)
                    .icon(getBitmapFromVectorDrawable(faculty.iconId, markerColor));

            faculty.marker = mapGoogle.addMarker(options);
        }
    }

    private void updateUserPositionOnMap() {
        if (!mapReady || userPosition == null) return;

        Log.d(TAG, "Actualizando marcador en: " + userPosition.latitude + ", " + userPosition.longitude);

        // Si ya existe un marcador, actualizar su posición
        if (userMarker != null) {
            userMarker.setPosition(userPosition);
        } else {
            // Si no existe, crear uno nuevo
            userMarker = mapGoogle.addMarker(new MarkerOptions()
                    .position(userPosition)
                    .icon(getBitmapFromVectorDrawable(R.drawable.location_on_24px, Color.BLUE))
                    .title("Tu ubicación"));
        }
    }

    private ArrayList<LatLng> createRoutePoints(LatLng start, LatLng end, TransportType transportType) {
        ArrayList<LatLng> routePoints = new ArrayList<>();


        NETEXAdapter adapter = new NETEXAdapter();
        ArrayList<Line> lines = new ArrayList<>();
        adapter.load(requireContext().getFilesDir() + "/data/tuvisa", lines);



        // Para simplificar y asegurar que al menos se dibuja algo, usamos una ruta simple
        routePoints.add(start);

        // Añadir punto intermedio simplificado
        LatLng midPoint = new LatLng(
                (start.latitude + end.latitude) / 2,
                (start.longitude + end.longitude) / 2
        );

        // Añadir variación según tipo de transporte
        double offset = 0;
        switch (transportType) {
            case WALK:
                offset = 0.0005;
                break;
            case BIKE:
                offset = 0.001;
                break;
            case BUS:
                offset = 0.002;
                break;
            case CAR:
                offset = 0.0015;
                break;
        }

        // Modificar punto intermedio ligeramente
        midPoint = new LatLng(
                midPoint.latitude + offset,
                midPoint.longitude - offset
        );

        routePoints.add(midPoint);
        routePoints.add(end);

        return routePoints;
    }

    private int getRouteColor(TransportType type) {
        switch (type) {
            case WALK:
                return Color.BLUE;
            case BIKE:
                return Color.GREEN;
            case BUS:
                return Color.RED;
            case CAR:
                return Color.YELLOW;
            default:
                return Color.GRAY;
        }
    }

    private void zoomToShowRoute(ArrayList<LatLng> routePoints) {
        if (!mapReady || routePoints == null || routePoints.isEmpty()) {
            return;
        }

        try {
            // Construir los límites para incluir todos los puntos de la ruta
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            for (LatLng point : routePoints) {
                boundsBuilder.include(point);
            }

            // Animar la cámara para mostrar toda la ruta
            final int padding = 100; // En píxeles
            LatLngBounds bounds = boundsBuilder.build();
            mapGoogle.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            Log.d(TAG, "Zoom aplicado a la ruta");
        } catch (Exception e) {
            Log.e(TAG, "Error al hacer zoom a la ruta: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    private void obtainActualLocation(boolean updateMap) {
        if (!hasLocationPermission()) {
            Log.d(TAG, "obtainActualLocation: sin permisos");
            if (listener != null) {
                listener.askLocationPermission();
            }
            return;
        }

        // Mostrar un indicador de carga
        Toast.makeText(getContext(), "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();

        try {
            // Configurar solicitud de ubicación con alta precisión
            LocationRequest locationRequest = LocationRequest.create()
                    .setNumUpdates(1)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

            Log.d(TAG, "Solicitando última ubicación conocida...");

            // Primero intentar con la última ubicación conocida (más rápido)
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "Última ubicación conocida: " + location.getLatitude() + ", " + location.getLongitude());
                            userPosition = new LatLng(location.getLatitude(), location.getLongitude());

                            if (updateMap && mapReady) {
                                updateUserPositionOnMap();
                                mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f));
                                Toast.makeText(getContext(), "Ubicación obtenida", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "No hay última ubicación, solicitando actualización...");
                            // No hay última ubicación, intentar obtener una actualización
                            try {
                                fusedLocationClient.requestLocationUpdates(locationRequest,
                                        new LocationCallback() {
                                            @Override
                                            public void onLocationResult(LocationResult locationResult) {
                                                fusedLocationClient.removeLocationUpdates(this);

                                                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                                                    Location location = locationResult.getLocations().get(0);
                                                    Log.d(TAG, "Nueva ubicación obtenida: " + location.getLatitude() + ", " + location.getLongitude());
                                                    userPosition = new LatLng(location.getLatitude(), location.getLongitude());

                                                    if (updateMap && mapReady) {
                                                        updateUserPositionOnMap();
                                                        mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f));
                                                        Toast.makeText(getContext(), "Ubicación obtenida", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Log.d(TAG, "No se pudo obtener ubicación, usando ubicación por defecto");
                                                    // Si no se puede obtener ubicación, usar ubicación por defecto
                                                    userPosition = DEFAULT_LOCATION;
                                                    if (updateMap && mapReady) {
                                                        updateUserPositionOnMap();
                                                        mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f));
                                                        Toast.makeText(getContext(), "Usando ubicación por defecto", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }
                                        },
                                        Looper.getMainLooper());
                            } catch (SecurityException e) {
                                Log.e(TAG, "Error de permisos al obtener ubicación: " + e.getMessage());
                                userPosition = DEFAULT_LOCATION;
                                if (updateMap && mapReady) {
                                    updateUserPositionOnMap();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al obtener última ubicación: " + e.getMessage());
                        userPosition = DEFAULT_LOCATION;
                        if (updateMap && mapReady) {
                            updateUserPositionOnMap();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error general al obtener ubicación: " + e.getMessage());
            userPosition = DEFAULT_LOCATION;
            if (updateMap && mapReady) {
                updateUserPositionOnMap();
            }
        }
    }

    private Polyline drawRoute(ArrayList<LatLng> route, int color) {
        if (!mapReady || route == null || route.isEmpty()) {
            Log.e(TAG, "No se puede dibujar ruta: mapa no listo o ruta vacía");
            return null;
        }

        try {
            PolylineOptions lineOptions = new PolylineOptions()
                    .addAll(route)
                    .color(color)
                    .width(8f);

            Polyline line = mapGoogle.addPolyline(lineOptions);
            Log.d(TAG, "Ruta dibujada correctamente con " + route.size() + " puntos");
            return line;
        } catch (Exception e) {
            Log.e(TAG, "Error al dibujar ruta: " + e.getMessage());
            return null;
        }
    }

    private Marker drawVehicle(LatLng point, TransportType type, int color) {
        if (!mapReady) return null;

        try {
            MarkerOptions markerOptions = new MarkerOptions()
                    .icon(getBitmapFromVectorDrawable(getTransportIcon(type), color))
                    .position(point);

            return mapGoogle.addMarker(markerOptions);
        } catch (Exception e) {
            Log.e(TAG, "Error al dibujar vehículo: " + e.getMessage());
            return null;
        }
    }

    private int getTransportIcon(TransportType type) {
        switch (type) {
            default:
            case WALK:
                return R.drawable.directions_walk_24px;

            case BIKE:
                return R.drawable.pedal_bike_24px;

            case BUS:
                return R.drawable.directions_bus_24px;

            case CAR:
                return R.drawable.directions_car_24px;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume llamado");
        if (isInNavigationMode) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause llamado");
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView llamado");
        stopLocationUpdates();
        super.onDestroyView();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{
            listener = (MapFragment.ListenerFragmentMap) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException("La clase " +context.toString() + " debe implementar el listener");
        }
    }

    public interface ListenerFragmentMap {
        boolean askLocationPermission();
    }
}