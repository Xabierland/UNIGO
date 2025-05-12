package com.ehunzango.unigo.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ehunzango.unigo.R;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.PrimitiveIterator;

public class MapFragment extends Fragment
{

    private ListenerFragmentMap listener;

    private GoogleMap mapGoogle;
    private FusedLocationProviderClient locationClient;
    private LatLng userPosition;

    //private List<String> facultyNames;
    //private HashMap<FacultyIdentifier, LatLng> facultyPositions = new HashMap<>();
    private HashMap<FacultyIdentifier, FacultyInfo> facultyHashMap = new HashMap<>();
    private enum FacultyIdentifier
    {
        Escuela_Ingenieria_Vitoria_Gasteiz,
        Facultad_Economia_Empresa,
        Facultad_Educacion_Deporte,
        Facultad_Farmacia,
        Facultad_Letras,
        Facultad_Relaciones_Laborales_Trabajo_Social,
        Unidad_Docente_Medicina,
        Aulas_Experiencia_Alava
    }
    private class FacultyInfo
    {
        public FacultyIdentifier id;
        public String name;
        private MarkerOptions markerOptions;
        public int iconId;
        public LatLng position;
        public Marker marker;
        public FacultyInfo(FacultyIdentifier id, String name, int iconId, LatLng position)
        {
            this.id = id;
            this.name = name;
            this.iconId = iconId;
            this.position = position;
            this.markerOptions = null;

        }

        public MarkerOptions getMarkerOptions()
        {
            if(markerOptions == null)
            {
                this.markerOptions = new MarkerOptions()
                        .icon(getBitmapFromVectorDrawable(iconId, Color.RED))
                        .position(position)
                        .title(name);
            }

            return markerOptions;
        }


    }
    private List<FacultyInfo> dropDownOrder;

    private enum TransportType
    {
        WALK,
        BIKE,
        BUS
    }
    private HashMap<TransportType, ImageView> transportImageHashMap = new HashMap<>();

    private FacultyIdentifier selectedFaculty;
    private TransportType selectedTransport;


    public MapFragment()
    {
        // TODO: Cambiar los nombres por identificadores de idioma
        facultyHashMap = new HashMap<>();
        FacultyInfo info = null;

        info = new FacultyInfo(
                FacultyIdentifier.Escuela_Ingenieria_Vitoria_Gasteiz,
                "Escuela de Ingeniería de Vitoria-Gasteiz",
                R.drawable.engineering_24px,
                new LatLng(42.83921784390094, -2.6744744038759203));
        facultyHashMap.put(info.id, info);


        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Economia_Empresa,
                "Facultad de Economía y Empresa",
                R.drawable.savings_24px,
                new LatLng(42.83758845850613, -2.668680518146171));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Educacion_Deporte,
                "Facultad de Educación y Deporte",
                R.drawable.sports_tennis_24px,
                new LatLng(42.83933934794871, -2.6744481633876007));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Farmacia,
                "Facultad de Farmacia",
                R.drawable.mixture_med_24px,
                new LatLng(42.84064230072461, -2.6718272245113464));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Letras,
                "Facultad de Letras",
                R.drawable.history_edu_24px,
                new LatLng(42.8403414326549, -2.670404675504716));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Facultad_Relaciones_Laborales_Trabajo_Social,
                "Facultad de Relaciones Laborales y Trabajo Social",
                R.drawable.handshake_24px,
                new LatLng(42.8400093266957, -2.670331999775272));
        facultyHashMap.put(info.id, info);

        info = new FacultyInfo(
                FacultyIdentifier.Unidad_Docente_Medicina,
                "Unidad Docente de Medicina",
                R.drawable.medical_services_24px,
                new LatLng(42.8390455018132, -2.670360696479438));
        facultyHashMap.put(info.id, info);
    }

    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);


        // Pedir permisos de ubicación
        if (listener != null)
        {
            listener.askLocationPermission();
        }

        // Inicializar cliente de ubicación
        locationClient = LocationServices.getFusedLocationProviderClient(getContext());

        obtainActualLocation(false);

        // Obtener el fragmento del mapa e inicializarlo
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map_fragment);

        if (mapFragment != null)
        {
            mapFragment.getMapAsync(new OnMapReadyCallback()
            {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap)
                {
                    mapGoogle = googleMap;

                    // Ubicación de ejemplo: Bogotá
                    //LatLng bogota = new LatLng(4.7110, -74.0721);
                    //mapGoogle.addMarker(new MarkerOptions().position(bogota).title("Marker en Bogotá"));
                    if(userPosition != null)
                    {
                        mapGoogle.addMarker(new MarkerOptions().position(userPosition).icon(getBitmapFromVectorDrawable(R.drawable.location_on_24px, Color.RED)));
                        mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 12f), 1000, null);
                    }
                    else
                    {
                        obtainActualLocation(true);
                    }

                    Collection<FacultyInfo> faculties = facultyHashMap.values();
                    for (FacultyInfo faculty : faculties)
                    {
                        Marker m = mapGoogle.addMarker(faculty.getMarkerOptions());
                        faculty.marker = m;

                    }
                }
            });
        }

        // DROPDOWN MENU
        setUpDropDownMenu(view);

        // TRANSPORT TYPE IMAGE BUTTONS
        setUpTansportButtons(view);
    }


    private void setUpDropDownMenu(View view)
    {
        Spinner dropDownMenu = view.findViewById(R.id.spinner_destinos);

        List<String> items = new ArrayList<>();
        items.add("Selecciona un destino"); //TODO: Poner idioma
        dropDownOrder = new ArrayList<>();
        Collection<FacultyInfo> faculties = facultyHashMap.values();
        for (FacultyInfo faculty : faculties)
        {
            items.add(faculty.name);
            dropDownOrder.add(faculty);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                                            requireContext(),
                                                            android.R.layout.simple_spinner_item,
                                                            items
                                                         )
        {
            @Override
            public boolean isEnabled(int position)
            {
                return position != 0; // Desactiva la opcion : Selecciona un destino
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent)
            {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0)
                {
                    textView.setTextColor(Color.GRAY);
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropDownMenu.setAdapter(adapter);

        dropDownMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 0)
                {
                    return;
                }
                position = position-1;

                //Toast.makeText(getContext(), facultyNames.get(position), Toast.LENGTH_SHORT).show();
                FacultyInfo info = dropDownOrder.get(position);
                selectFaculty(info.id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
    }

    private void selectFaculty(FacultyIdentifier id)
    {
        FacultyInfo faculty = facultyHashMap.get(id);
        if(faculty == null)
        {
            return;
        }

        selectedFaculty = id;

        Collection<FacultyInfo> faculties = facultyHashMap.values();
        for (FacultyInfo f : faculties)
        {
            if(f.marker != null)
            {
                f.marker.setIcon(getBitmapFromVectorDrawable(f.iconId, Color.GRAY));
            }
        }

        //Toast.makeText(getContext(), faculty.name, Toast.LENGTH_SHORT).show();
        if(faculty.marker != null)
        {
            faculty.marker.setIcon(getBitmapFromVectorDrawable(faculty.iconId, Color.RED));
        }
        //mapGoogle.moveCamera(CameraUpdateFactory.newLatLngZoom(faculty.position, 17f));
        mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(faculty.position, 17f), 500, null);
    }

    private BitmapDescriptor getBitmapFromVectorDrawable(int drawableId, int color)
    {
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


    private void setUpTansportButtons(View view)
    {
        Button buttonWalk = view.findViewById(R.id.button_background_walk);
        Button buttonBike = view.findViewById(R.id.button_background_bike);
        Button buttonBus = view.findViewById(R.id.button_background_bus);

        buttonWalk.setOnClickListener(v -> transportSelected(TransportType.WALK));
        buttonBike.setOnClickListener(v -> transportSelected(TransportType.BIKE));
        buttonBus.setOnClickListener(v -> transportSelected(TransportType.BUS));

        transportImageHashMap = new HashMap<>();

        ImageView imageViewWalk = view.findViewById(R.id.image_walking_symbol);
        ImageView imageViewBike = view.findViewById(R.id.image_bike_symbol);
        ImageView imageViewBus = view.findViewById(R.id.image_bus_symbol);

        transportImageHashMap.put(TransportType.WALK, imageViewWalk);
        transportImageHashMap.put(TransportType.BIKE, imageViewBike);
        transportImageHashMap.put(TransportType.BUS, imageViewBus);

        Collection<ImageView> images = transportImageHashMap.values();
        for (ImageView image : images)
        {
            image.setColorFilter(Color.GRAY);
        }
    }

    private void transportSelected(TransportType type)
    {
        Collection<ImageView> images = transportImageHashMap.values();
        for (ImageView image : images)
        {
            image.setColorFilter(Color.GRAY);
        }

        transportImageHashMap.get(type).setColorFilter(Color.RED);
        selectedTransport = type;

        switch (type)
        {
            default:
            case WALK: //Caminar
                //Toast.makeText(getContext(), "Caminar", Toast.LENGTH_SHORT).show();
                break;

            case BIKE: // Bici
                //Toast.makeText(getContext(), "Bici", Toast.LENGTH_SHORT).show();
                break;

            case BUS: // Bus
                //Toast.makeText(getContext(), "Bus", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void obtainActualLocation(boolean updateMap)
    {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //listener.salirDelMapa(); //TODO
            Toast.makeText(getContext(), "[obtainActualLocation] NO PEWMISSION", Toast.LENGTH_SHORT).show();
            return;
        }

        locationClient  .getLastLocation()
                        .addOnSuccessListener(getActivity(), location ->
                        {
                            userPosition = null;
                            if (location != null) {
                                double lat = location.getLatitude();
                                double lon = location.getLongitude();

                                userPosition = new LatLng(lat, lon);

                                if(updateMap)
                                {
                                    mapGoogle.addMarker(new MarkerOptions().position(userPosition).icon(getBitmapFromVectorDrawable(R.drawable.location_on_24px, Color.RED)));
                                    mapGoogle.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 12f), 1000, null);
                                }
                            }

                        });
    }


    //              +--------------------------------------------------------------------------+
    //              |                                                                             |
    //              |                        CONEXION CON MAIN ACTIVITY                        |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

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

    public interface ListenerFragmentMap
    {
        boolean askLocationPermission();
    }
}