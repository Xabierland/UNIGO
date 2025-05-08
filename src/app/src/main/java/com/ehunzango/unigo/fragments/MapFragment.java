package com.ehunzango.unigo.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.HashMap;

public class MapFragment extends Fragment
{

    private ListenerFragmentMap listener;

    private GoogleMap mapGoogle;
    private FusedLocationProviderClient locationClient;
    private LatLng userPosition;

    private HashMap<facultyName, LatLng> facultyPositions = new HashMap<>();
    private enum facultyName
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



    public MapFragment()
    {
        facultyPositions = new HashMap<>();
        facultyPositions.put(
                facultyName.Escuela_Ingenieria_Vitoria_Gasteiz,
                new LatLng(42.83921784390094, -2.6744744038759203));

        facultyPositions.put(
                facultyName.Facultad_Economia_Empresa,
                new LatLng(42.83758845850613, -2.668680518146171));

        facultyPositions.put(
                facultyName.Facultad_Educacion_Deporte,
                new LatLng(42.83933934794871, -2.6744481633876007));

        facultyPositions.put(
                facultyName.Facultad_Farmacia,
                new LatLng(42.84064230072461, -2.6718272245113464));

        facultyPositions.put(
                facultyName.Facultad_Letras,
                new LatLng(42.8403414326549, -2.670404675504716));

        facultyPositions.put(
                facultyName.Facultad_Relaciones_Laborales_Trabajo_Social,
                new LatLng(42.8400093266957, -2.670331999775272));

        facultyPositions.put(
                facultyName.Unidad_Docente_Medicina,
                new LatLng(42.8390455018132, -2.670360696479438));
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
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);

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
                        mapGoogle.addMarker(new MarkerOptions().position(userPosition).title("OWO"));
                        mapGoogle.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 12f));
                    }
                    else
                    {
                        obtainActualLocation(true);
                    }


                    // Escuela_Ingenieria_Vitoria_Gasteiz
                    MarkerOptions marker;

                    marker = new MarkerOptions()    .icon(getBitmapFromVectorDrawable(R.drawable.engineering_24px))
                                                    .position(facultyPositions.get(facultyName.Escuela_Ingenieria_Vitoria_Gasteiz))
                                                    .title("Escuela_Ingenieria_Vitoria_Gasteiz");
                    mapGoogle.addMarker(marker);

                    // Facultad_Economia_Empresa
                     marker = new MarkerOptions()   .icon(getBitmapFromVectorDrawable(R.drawable.savings_24px))
                                                    .position(facultyPositions.get(facultyName.Facultad_Economia_Empresa))
                                                    .title("Facultad_Economia_Empresa");
                    mapGoogle.addMarker(marker);

                    // Facultad_Educacion_Deporte
                     marker = new MarkerOptions()   .icon(getBitmapFromVectorDrawable(R.drawable.sports_tennis_24px))
                                                    .position(facultyPositions.get(facultyName.Facultad_Educacion_Deporte))
                                                    .title("Facultad_Educacion_Deporte");
                    mapGoogle.addMarker(marker);

                    // Facultad_Farmacia
                    marker = new MarkerOptions()    .icon(getBitmapFromVectorDrawable(R.drawable.mixture_med_24px))
                                                    .position(facultyPositions.get(facultyName.Facultad_Farmacia))
                                                    .title("Facultad_Farmacia");
                    mapGoogle.addMarker(marker);

                    // Facultad_Letras
                    marker = new MarkerOptions()    .icon(getBitmapFromVectorDrawable(R.drawable.history_edu_24px))
                                                    .position(facultyPositions.get(facultyName.Facultad_Letras))
                                                    .title("Facultad_Letras");
                    mapGoogle.addMarker(marker);

                    // Facultad_Relaciones_Laborales_Trabajo_Social
                    marker = new MarkerOptions()    .icon(getBitmapFromVectorDrawable(R.drawable.handshake_24px))
                                                    .position(facultyPositions.get(facultyName.Facultad_Relaciones_Laborales_Trabajo_Social))
                                                    .title("Facultad_Relaciones_Laborales_Trabajo_Social");
                    mapGoogle.addMarker(marker);

                    // Unidad_Docente_Medicina
                    marker = new MarkerOptions()    .icon(getBitmapFromVectorDrawable(R.drawable.medical_services_24px))
                                                    .position(facultyPositions.get(facultyName.Unidad_Docente_Medicina))
                                                    .title("Unidad_Docente_Medicina");
                    mapGoogle.addMarker(marker);
                }
            });
        }
    }

    private BitmapDescriptor getBitmapFromVectorDrawable(int drawableId)
    {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), drawableId);

        if (drawable == null) {
            throw new IllegalArgumentException("Drawable not found");
        }

        drawable.setTint(Color.RED);

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
                                    mapGoogle.addMarker(new MarkerOptions().position(userPosition).title("OWO"));
                                    mapGoogle.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 12f));
                                }
                            }

                        });
    }


    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
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