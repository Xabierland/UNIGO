package com.ehunzango.unigo.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ehunzango.unigo.BuildConfig;
import com.ehunzango.unigo.R;
import com.ehunzango.unigo.adapters.ImageSpinnerAdapter;
import com.ehunzango.unigo.adapters.SpinnerImageItem;
import com.ehunzango.unigo.router.RouteFinder;
import com.ehunzango.unigo.router.RouteFinder;
import com.ehunzango.unigo.router.SimpleBusRouteFinder;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
        BUS
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

                    // Actualizar la posición del marcador sin centrar el mapa
                    if (mapReady) {
                        updateUserPositionOnMap();
                        
                        // Solo centrar el mapa si estamos en modo navegación
                        if (isInNavigationMode) {
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
        if(selectedTransport == TransportType.BUS)
        {
            simpleBusRoute();
            return;
        }
        else if(selectedTransport == TransportType.WALK)
        {
            calculateWalkingRoute();
            return;
        }
        else
        {
            currentRoutePoints = createRoutePoints(userPosition, faculty.position, selectedTransport);
        }

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

    private void calculateWalkingRoute() {
        FacultyInfo faculty = facultyHashMap.get(selectedFaculty);
        if (faculty == null) {
            Toast.makeText(getContext(), "Selecciona un destino primero", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Calculando ruta a pie...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Iniciando cálculo de ruta a pie desde " + userPosition.toString() + " hasta " + faculty.position.toString());

        // Realizar la solicitud a la API de Routes en un hilo separado
        new Thread(() -> {
            HttpURLConnection conn = null;
            BufferedReader br = null;
            
            try {
                // Obtener API key desde BuildConfig
                String apiKey = BuildConfig.MAPS_API_KEY;
                Log.d(TAG, "API Key obtenida (primeros 5 caracteres): " + (apiKey.length() > 5 ? apiKey.substring(0, 5) + "..." : "no disponible"));
                
                // URL de la Routes API
                String urlString = "https://routes.googleapis.com/directions/v2:computeRoutes";
                
                // Crear el cuerpo de la petición JSON para la Routes API
                JSONObject requestBody = new JSONObject();
                
                // Origen
                JSONObject origin = new JSONObject();
                JSONObject originLocation = new JSONObject();
                originLocation.put("latLng", new JSONObject()
                        .put("latitude", userPosition.latitude)
                        .put("longitude", userPosition.longitude));
                origin.put("location", originLocation);
                requestBody.put("origin", origin);
                
                // Destino
                JSONObject destination = new JSONObject();
                JSONObject destinationLocation = new JSONObject();
                destinationLocation.put("latLng", new JSONObject()
                        .put("latitude", faculty.position.latitude)
                        .put("longitude", faculty.position.longitude));
                destination.put("location", destinationLocation);
                requestBody.put("destination", destination);
                
                // Modo de transporte
                requestBody.put("travelMode", "WALK");
                
                // Opciones de ruta
                JSONObject routingPreference = new JSONObject();
                routingPreference.put("routingPreference", "ROUTING_PREFERENCE_UNSPECIFIED");
                requestBody.put("routingPreference", "ROUTING_PREFERENCE_UNSPECIFIED");
                
                // Lenguaje
                requestBody.put("languageCode", "es-ES");
                
                // Unidades
                requestBody.put("units", "METRIC");
                
                // Calcular polilínea
                requestBody.put("computeAlternativeRoutes", false);
                requestBody.put("routeModifiers", new JSONObject()
                        .put("avoidTolls", false)
                        .put("avoidHighways", false)
                        .put("avoidFerries", false));
                
                // Opciones de polilínea
                requestBody.put("polylineQuality", "HIGH_QUALITY");
                requestBody.put("polylineEncoding", "ENCODED_POLYLINE");
                
                Log.d(TAG, "Cuerpo de la solicitud: " + requestBody.toString());
                
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-Goog-Api-Key", apiKey);
                conn.setRequestProperty("X-Goog-FieldMask", "routes.legs.steps.polyline.encodedPolyline,routes.polyline.encodedPolyline");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000); // 15 segundos de timeout
                conn.setReadTimeout(15000);
                
                // Escribir el cuerpo de la petición
                conn.getOutputStream().write(requestBody.toString().getBytes("UTF-8"));
                
                Log.d(TAG, "Conectando a la API...");
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Código de respuesta HTTP: " + responseCode);
                
                if (responseCode != 200) {
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        br = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        Log.e(TAG, "Respuesta de error: " + errorResponse.toString());
                    }
                    
                    Log.e(TAG, "Error en la respuesta HTTP: " + responseCode);
                    Log.e(TAG, "Mensaje: " + conn.getResponseMessage());
                    throw new RuntimeException("Error HTTP: " + responseCode + " - " + conn.getResponseMessage());
                }

                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                
                String responseStr = response.toString();
                Log.d(TAG, "Respuesta recibida, longitud: " + responseStr.length());
                
                // Registrar parte de la respuesta para depuración (limitada para no sobrecargar el log)
                if (responseStr.length() > 200) {
                    Log.d(TAG, "Primeros 200 caracteres de la respuesta: " + responseStr.substring(0, 200) + "...");
                } else {
                    Log.d(TAG, "Respuesta completa: " + responseStr);
                }

                // Procesar la respuesta JSON (formato diferente al de Directions API)
                JSONObject json = new JSONObject(responseStr);
                
                if (!json.has("routes") || json.getJSONArray("routes").length() == 0) {
                    Log.e(TAG, "No se encontraron rutas en la respuesta");
                    throw new RuntimeException("No se encontraron rutas en la respuesta");
                }
                
                JSONArray routes = json.getJSONArray("routes");
                Log.d(TAG, "Número de rutas encontradas: " + routes.length());
                
                JSONObject route = routes.getJSONObject(0);
                JSONObject polyline = route.getJSONObject("polyline");
                String points = polyline.getString("encodedPolyline");
                Log.d(TAG, "Polyline obtenida, longitud: " + points.length());
                
                // Decodificar polyline para obtener los puntos de la ruta
                ArrayList<LatLng> routePoints = decodePolyline(points);
                Log.d(TAG, "Polyline decodificada, puntos: " + routePoints.size());
                
                if (routePoints.isEmpty()) {
                    Log.e(TAG, "La polyline se decodificó, pero no se obtuvieron puntos");
                    throw new RuntimeException("No se pudieron obtener puntos de la ruta");
                }
                
                // Actualizar UI en el hilo principal
                requireActivity().runOnUiThread(() -> {
                    try {
                        // Limpiar mapa
                        mapGoogle.clear();
                        userMarker = null;
                        
                        if (currentRoute != null) {
                            currentRoute.remove();
                            currentRoute = null;
                        }
                        
                        // Volver a añadir todos los marcadores
                        addAllMarkers();
                        
                        // Guardar puntos de la ruta
                        currentRoutePoints = routePoints;
                        
                        // Dibujar la ruta
                        int routeColor = getRouteColor(selectedTransport);
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
                    } catch (Exception e) {
                        Log.e(TAG, "Error al actualizar UI: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error al mostrar la ruta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        
                        // Si hay un error, usar el método de fallback
                        fallbackToSimpleRoute(faculty);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error al calcular ruta a pie: " + e.getMessage(), e);
                
                // Registrar el stack trace completo
                e.printStackTrace();
                
                // Mostrar mensaje y usar ruta simple como fallback
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error al calcular ruta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    fallbackToSimpleRoute(faculty);
                });
            } finally {
                // Cerrar conexiones
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error al cerrar BufferedReader: " + e.getMessage());
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    // Método de fallback para usar si la API de Routes falla
    private void fallbackToSimpleRoute(FacultyInfo faculty) {
        Log.d(TAG, "Usando ruta simple como fallback");
        
        try {
            // Limpiar mapa
            mapGoogle.clear();
            userMarker = null;
            
            if (currentRoute != null) {
                currentRoute.remove();
                currentRoute = null;
            }
            
            // Volver a añadir todos los marcadores
            addAllMarkers();
            
            // Crear ruta simple
            ArrayList<LatLng> routePoints = createRoutePointsFallBack(userPosition, faculty.position, TransportType.WALK);
            currentRoutePoints = routePoints;
            
            // Dibujar la ruta
            int routeColor = getRouteColor(TransportType.WALK);
            currentRoute = drawRoute(currentRoutePoints, routeColor);
            
            // Añadir icono del medio de transporte en el origen
            drawVehicle(userPosition, TransportType.WALK, routeColor);
            
            // Ajustar la cámara para mostrar toda la ruta
            zoomToShowRoute(currentRoutePoints);
            
            // Mostrar el botón de iniciar navegación
            startNavigationButton.setVisibility(View.VISIBLE);
            
            Toast.makeText(getContext(), "Usando ruta simple alternativa", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error incluso en fallback: " + e.getMessage(), e);
            Toast.makeText(getContext(), "No se pudo generar ninguna ruta", Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<LatLng> decodePolyline(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            
            poly.add(new LatLng(latitude, longitude));
        }

        return poly;
    }

    private void simpleBusRoute()
    {
        FacultyInfo faculty = facultyHashMap.get(selectedFaculty);

        if(SimpleBusRouteFinder.getInstance().loaded == false)
        {
            Toast.makeText(getContext(), "Los datos se estan cargando", Toast.LENGTH_SHORT).show();
            return;
        }


        SimpleBusRouteFinder.Viaje viaje = SimpleBusRouteFinder.getInstance().obtenerViaje((float) userPosition.latitude, (float) userPosition.longitude, (float) faculty.position.latitude, (float) faculty.position.longitude);

        LatLng paradaEntrada = null;
        LatLng paradaSalida = null;
        if(viaje == null)
        {
            Log.d("mitag", "\t Viaje == null :(");
            Toast.makeText(getContext(), "No se pudo encontrar una ruta", Toast.LENGTH_SHORT).show();
            return;
        }

        try
        {
            paradaEntrada = new LatLng((double)viaje.origen.latitud, (double)viaje.origen.longitud);
            paradaSalida = new LatLng((double)viaje.destino.latitud, (double)viaje.destino.longitud);

            //currentRoutePoints = new ArrayList<>();

            //currentRoutePoints.add(userPosition);
            //currentRoutePoints.add(origen);
            //currentRoutePoints.add(destino);
            //currentRoutePoints.add(faculty.position);
        }
        catch (Exception e)
        {
            Log.d("mitag", "\t Excepcion en calculateRoute :(");
            Log.d("mitag", e.getMessage());
        }


        if(viaje.destino != viaje.origen)
        {


            drawVehicle(userPosition, TransportType.WALK, getRouteColor(TransportType.WALK));
            PolylineOptions lineOptions = new PolylineOptions()
                    .add(userPosition)
                    .add(paradaEntrada)
                    .color(getRouteColor(TransportType.WALK))
                    .width(8f);
            Polyline line = mapGoogle.addPolyline(lineOptions);


            drawParadaBus(paradaEntrada, viaje.origen, viaje.linea, TransportType.BUS);
            lineOptions = new PolylineOptions()
                    .addAll(viaje.linea.obtenerShape())
                    .color(getRouteColor(TransportType.BUS))
                    .width(8f);
        /*
        lineOptions = new PolylineOptions()
                .add(paradaEntrada)
                .add(paradaSalida)
                .color(getRouteColor(TransportType.BUS))
                .width(8f);*/
            line = mapGoogle.addPolyline(lineOptions);


            //drawVehicle(paradaSalida, TransportType.WALK, getRouteColor(TransportType.WALK));
            drawParadaBus(paradaSalida, viaje.destino, viaje.linea, TransportType.WALK);
            lineOptions = new PolylineOptions()
                    .add(paradaSalida)
                    .add(faculty.position)
                    .color(getRouteColor(TransportType.WALK))
                    .width(8f);
            line = mapGoogle.addPolyline(lineOptions);
        }
        else
        {
            drawVehicle(userPosition, TransportType.WALK, getRouteColor(TransportType.WALK));
            PolylineOptions lineOptions = new PolylineOptions()
                    .add(userPosition)
                    .add(faculty.position)
                    .color(getRouteColor(TransportType.WALK))
                    .width(8f);
            Polyline line = mapGoogle.addPolyline(lineOptions);
        }
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

        // Restaurar UI normal y ocultar el botón
        startNavigationButton.setImageResource(R.drawable.navigation_24px);
        startNavigationButton.setOnClickListener(v -> startNavigation());
        startNavigationButton.setVisibility(View.GONE); // Ocultar el botón

        // Detener actualizaciones frecuentes de ubicación
        stopLocationUpdates();

        if (mapReady) {
            // Borrar la ruta actual pero mantener los marcadores
            if (currentRoute != null) {
                currentRoute.remove();
                currentRoute = null;
            }
            
            // No reconstruir ninguna ruta, solo mantener los marcadores existentes
            // y asegurarnos de que la vista del mapa sea adecuada
            if (userPosition != null) {
                // Ajustar zoom para mostrar tanto al usuario como a la facultad destino
                if (selectedFaculty != null) {
                    FacultyInfo faculty = facultyHashMap.get(selectedFaculty);
                    if (faculty != null) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(userPosition);
                        builder.include(faculty.position);
                        
                        try {
                            mapGoogle.animateCamera(CameraUpdateFactory.newLatLngBounds(
                                builder.build(), 150));  // 150px de padding
                        } catch (Exception e) {
                            // En caso de error, solo centrar en el usuario
                            mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                userPosition, 15f));
                        }
                    }
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

    private final RouteFinder rf = new RouteFinder();

    private ArrayList<LatLng> createRoutePoints(LatLng start, LatLng end, TransportType transportType) {
        ArrayList<LatLng> routePoints = new ArrayList<>();
        List<Line> lines = null;

        if (transportType == TransportType.BIKE) {
            Log.d(TAG, "BIKE");
            lines = RouteService.getInstance()
                    .getLines()
                    .stream()
                    .filter(line -> line.type == Line.Type.BIKE) // TODO: change this bs
                    .collect(Collectors.toList());
        } else if (transportType == TransportType.BUS) {
            Log.d(TAG, "BUS");
            lines = RouteService.getInstance()
                    .getBusLines();
            Log.d(TAG, String.format("size: %d", lines != null ? lines.size() : 0));
            if (lines != null) {
                lines = lines.stream()
                        .filter(line -> line.type == Line.Type.BUS) // TODO: change this bs
                        .collect(Collectors.toList());
            }
        }

        // Verificar si lines es null además de comprobar si está vacío
        if (transportType != TransportType.BIKE || lines == null || lines.isEmpty()) {
            Log.d(TAG, "FALL (1) BACK FUCK");
            Log.d(TAG, transportType.toString());
            Log.d(TAG, String.format("lines is null: %b", lines == null));
            if (lines != null) {
                Log.d(TAG, String.format("size: %d", lines.size()));
            }
            Log.d(TAG, String.format("RouteService lines size: %d", RouteService.getInstance().getLines().size()));
            return createRoutePointsFallBack(start, end, transportType);
        }

        Line dummy = new Line("dummy", Line.Type.WALK, new ArrayList<>());
        Node s = new Node(start.latitude, start.longitude, dummy);
        Node e = new Node(end.latitude, end.longitude, dummy);
        dummy.addNode(s, (float)s.dist(e));
        dummy.addNode(e, Float.POSITIVE_INFINITY);
        lines.add(dummy);

        List<Node> path = rf.findShortestPath(s, e, lines);

        if (path == null || path.isEmpty()) {
            Log.d(TAG, "FALL (2) BACK FUCK");
            return createRoutePointsFallBack(start, end, transportType);
        }

        return path.stream()
                .map(node -> new LatLng(node.x, node.y))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<LatLng> createRoutePointsFallBack(LatLng start, LatLng end, TransportType transportType) {
        ArrayList<LatLng> routePoints = new ArrayList<>();

        // NOTE: TE COMENTO ESTO ENEKO ZORRY (perdon en ingles)
        // NETEXAdapter adapter = new NETEXAdapter();
        // ArrayList<Line> lines = new ArrayList<>();
        // adapter.load(requireContext().getFilesDir() + "/data/tuvisa", lines);


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
                /*
            case CAR:
                offset = 0.0015;
                break;*/
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
                return Color.RED;/*
            case CAR:
                return Color.YELLOW;*/
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

    private Marker drawParadaBus(LatLng point, SimpleBusRouteFinder.Parada parada, SimpleBusRouteFinder.Linea linea, TransportType type)
    {
        if (!mapReady) return null;

        int color = getRouteColor(type);
        Marker ret = null;

        try {
            MarkerOptions markerOptions = new MarkerOptions()
                    .icon(getBitmapFromVectorDrawable(getTransportIcon(type), color))
                    .position(point);

            ret = mapGoogle.addMarker(markerOptions);
        } catch (Exception e) {
            Log.e(TAG, "Error al dibujar vehículo: " + e.getMessage());
            return null;
        }

        ret.setTag(parada.nombre + "<<<<>>>>" + linea.nombre);

        mapGoogle.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker clickedMarker) {
                Object tag = clickedMarker.getTag();

                // Lógica personalizada según el marker
                if (tag != null && tag instanceof String)
                {
                    String texto = (String) tag;
                    String[] partes = texto.split("<<<<>>>>");
                    String parada = partes[0];
                    String linea = partes[1];

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getContext().getString(R.string.linea) + ": " + linea);

                    TextView aux = new TextView(getContext());


                    TextView textViewParada = new TextView(getContext());
                    textViewParada.setText("   " + getContext().getString(R.string.parada) + ": " + parada);

                    LinearLayout layoutName = new LinearLayout(getContext());
                    layoutName.setOrientation(LinearLayout.VERTICAL);

                    layoutName.addView(aux);
                    layoutName.addView(textViewParada);

                    builder.setView(layoutName);

                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    builder.show();
                }

                return false; // true si manejas completamente el clic (para evitar el popup por defecto)
            }
        });


        return ret;
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
/*
            case CAR:
                return R.drawable.directions_car_24px;*/
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