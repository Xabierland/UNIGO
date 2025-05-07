package com.ehunzango.unigo.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment
{

    private ListenerFragmentMap listener;

    private GoogleMap mapGoogle;
    private FusedLocationProviderClient locationClient;
    private LatLng userPosition;

    public MapFragment()
    {

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
                }
            });
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