package com.ehunzango.unigo.router;

import android.util.Log;

import com.ehunzango.unigo.router.adapters.NETEXAdapter;
import com.ehunzango.unigo.router.entities.Line;
import com.google.android.gms.maps.model.LatLng;

import org.checkerframework.checker.units.qual.A;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class SimpleBusRouteFinder
{
    private static SimpleBusRouteFinder instance;

    public boolean loaded = false;

    public static SimpleBusRouteFinder getInstance()
    {
        if (instance == null)
        {
            instance = new SimpleBusRouteFinder();
        }
        return  instance;
    }

    ArrayList<Linea> lineas;

    public class Linea
    {
        public String nombre;
        public String id;
        public ArrayList<Parada> paradas;
        public Dibujito dibujo;

        public ArrayList<LatLng> obtenerShape()
        {
            ArrayList<LatLng> resultado = new ArrayList<>();

            if(dibujo == null)
            {
                return resultado;
            }

            ArrayList<Posicion> posiciones = dibujo.puntos;

            if(posiciones == null)
            {
                return resultado;
            }
            for (Posicion p : posiciones)
            {
                resultado.add(new LatLng(p.latitud, p.longitud));
            }

            return resultado;
        }
    }

    public class Parada
    {
        public String nombre;
        public String id;
        public float latitud;
        public float longitud;
        public float orden;
    }

    public class Viaje
    {
        public Parada origen;
        public Parada destino;
        public Linea linea;
    }

    public class Horario
    {
        // Id
        // Linea
        // Dias de la semana
        // Shape
        // Direccion
        // Display
        String id;
        String idLinea;
        Linea linea;
        String direccion;
        String display;
        String idShape;
    }

    public class HoraParada
    {
        // id Horario
        // id Parada
        // Orden de parada
        // Hora salida
        // Hora llegada

        public String idHorario;
        String idParada;
        int orden;
        String horaSalida;
        public String horaLlegada;
    }

    public class Dibujito
    {
        public String id;
        public ArrayList<String> puntosString;
        public ArrayList<Posicion> puntos;

        public void procesar()
        {
            //Log.d("mitag", "\t Procesar ");
            puntos = new ArrayList<>();
            for (String punto : puntosString)
            {
                //Log.d("mitag", "\t \t " + punto);
                String[] partes = punto.split(" ");
                if(partes.length != 2)
                {
                    continue;
                }
                Posicion p = new Posicion();
                p.latitud = Double.parseDouble(partes[0]);
                p.longitud = Double.parseDouble(partes[1]);
                puntos.add(p);
            }
        }
    }

    public class Posicion
    {
        double latitud;
        double longitud;
    }



    public Viaje obtenerViaje(float latitudOrigen, float longitudOrigen, float latitudDestino, float longitudDestino)
    {

        Log.d("mitag", "Obteniendo viaje ...");

       if(lineas == null)
       {
           Log.d("mitag", "Lineas == null");
           return null;
       }
        Viaje resultado = null;
        try
        {
            resultado = new Viaje();
            float distanciaMasCorta = Float.MAX_VALUE;

            for (Linea linea : lineas)
            {
                float minimaDistanciaOrigen = Float.MAX_VALUE;
                Parada paradaOrigen = null;
                float minimaDistanciaDestino = Float.MAX_VALUE;
                Parada paradaDestino = null;

                for (Parada parada : linea.paradas)
                {
                    float distanciaActual = calcularDistancia(latitudOrigen, longitudOrigen, parada);
                    if(distanciaActual < minimaDistanciaOrigen)
                    {
                        minimaDistanciaOrigen = distanciaActual;
                        paradaOrigen = parada;
                    }

                    distanciaActual = calcularDistancia(latitudDestino, longitudDestino, parada);
                    if(distanciaActual < minimaDistanciaDestino)
                    {
                        minimaDistanciaDestino = distanciaActual;
                        paradaDestino = parada;
                    }
                }

                if(minimaDistanciaOrigen + minimaDistanciaDestino < distanciaMasCorta)
                {
                    Log.d("mitag", "Nueva distancia minima : " + (minimaDistanciaOrigen + minimaDistanciaDestino));
                    distanciaMasCorta = minimaDistanciaOrigen + minimaDistanciaDestino;
                    resultado.origen = paradaOrigen;
                    resultado.destino = paradaDestino;
                    resultado.linea = linea;
                }
            }

            Log.d("mitag", "Acabado : ");
            Log.d("mitag", "\t Linea : " + resultado.linea.nombre);
            Log.d("mitag", "\t Parada Entrada : " + resultado.origen.id);
            Log.d("mitag", "\t Parada Salida : " + resultado.destino.id);


        }
        catch (Exception e)
        {
            Log.d("mitag", "\t Excepcion en obtenerViaje :(");
            Log.d("mitag", e.getMessage());
        }
        return resultado;
    }

    public float calcularDistancia(float lat, float lon, Parada parada)
    {
        if(parada == null)
        {
            return Float.MAX_VALUE;
        }
        return (float) Math.sqrt(Math.pow(lat - parada.latitud, 2) + Math.pow(lon - parada.longitud, 2));
    }

    public void cargarDatos(String path)
    {
        try
        {
            Log.d("mitag", "Cargando datos ...");
            Map<String, Parada> mapaParadas = loadParadas(path + "/stops.xml");
            Log.d("mitag", "Paradas cargadas : " + mapaParadas.size());

            Map<String, Linea> mapaLineas = loadLineas(path + "/routes.xml");
            Log.d("mitag", "Lineas cargadas : " + mapaLineas.size());

            Map<String, Horario> mapaHorarios = loadHorario(path + "/trips.xml");
            Log.d("mitag", "Horarios cargados : " + mapaHorarios.size());

            Map<String, List<HoraParada>> mapaHorasParadas = loadHorasParadas(path + "/stop_times.xml");
            Log.d("mitag", "Horas de paradas cargadas : " + mapaHorasParadas.size());

            Map<String, Dibujito> mapaShapes = loadShapes(path + "/shapes.xml");
            Log.d("mitag", "Dibujitos cargados : " + mapaShapes.size());


            ArrayList<String> lineasCargadas = new ArrayList<>();

            Log.d("mitag", "Recorriendo horarios...");
            for (Horario horario : mapaHorarios.values())
            {
                if(horario == null)
                {
                    Log.d("mitag", "\t Horario == null");
                    continue;
                }
                List<HoraParada> horas = mapaHorasParadas.get(horario.id);

                if(horas == null)
                {
                    Log.d("mitag", "\t horas == null");
                    continue;
                }

                String idLineaActual = horario.idLinea;

                //Log.d("mitag", "\t Linea : " + idLineaActual);

                if(lineasCargadas.contains(idLineaActual))
                {
                    continue;
                }

                Linea lineaActual = mapaLineas.get(idLineaActual);

                Log.d("mitag", "\t \t Linea : " + lineaActual.nombre);

                if(lineaActual == null)
                {
                    continue;
                }

                for (HoraParada horaParada : horas)
                {
                    Parada parada = mapaParadas.get(horaParada.idParada);

                    if(parada == null)
                    {
                        continue;
                    }

                    parada.orden = horaParada.orden;
                    if(lineaActual.paradas == null)
                    {
                        lineaActual.paradas = new ArrayList<>();
                    }
                    lineaActual.paradas.add(parada);
                }

                Dibujito shapeActual = mapaShapes.get(horario.idShape);
                if(shapeActual == null)
                {
                    Log.d("mitag", "\t \t shapeActual == null ");
                }
                else
                {
                    if(shapeActual.puntos != null)
                    {
                        Log.d("mitag", "\t \t shapeActual " + shapeActual.puntos.size() + " puntitos");
                    }
                    else
                    {
                        Log.d("mitag", "\t \t shapeActual.puntos == null ");
                    }

                }
                lineaActual.dibujo = shapeActual;

                if(lineasCargadas == null)
                {
                    Log.d("mitag", "\t \t No deberia de ser null???? lineasCargadas ");
                }
                lineasCargadas.add(idLineaActual);
                if(lineas == null)
                {
                    Log.d("mitag", "\t Creando lista");
                    lineas = new ArrayList<>();
                }
                lineas.add(lineaActual);
            }

            loaded = true;
        }
        catch (Exception e)
        {
            Log.d("mitag", "\t Excepcion :(");
            Log.d("mitag", e.getMessage());
        }

    }





    private Map<String, Parada> loadParadas(String filename) throws Exception
    {
        Map<String, Parada> map = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (InputStream in = new FileInputStream(filename))
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            Parada paradaActual = null;
            boolean insideScheduledStopPoint = false;
            boolean insideLocation = false;


            while (reader.hasNext())
            {
                int event = reader.next();

                switch (event)
                {
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = reader.getLocalName();

                        if ("ScheduledStopPoint".equals(localName))
                        {
                            insideScheduledStopPoint = true;
                            paradaActual = new Parada();
                            paradaActual.id = reader.getAttributeValue(null, "id");
                        }
                        else if (insideScheduledStopPoint && "Name".equals(localName))
                        {
                            paradaActual.nombre = reader.getElementText();
                        }
                        else if (insideScheduledStopPoint && "Location".equals(localName)) {
                            insideLocation = true;
                        }
                        else if(insideLocation && "Latitude".equals(localName))
                        {
                            paradaActual.latitud = Float.parseFloat(reader.getElementText());
                        }
                        else if(insideLocation && "Longitude".equals(localName))
                        {
                            paradaActual.longitud = Float.parseFloat(reader.getElementText());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endName = reader.getLocalName();

                        if ("ScheduledStopPoint".equals(endName))
                        {
                            insideScheduledStopPoint = false;
                            if(paradaActual != null)
                            {
                                map.put(paradaActual.id, paradaActual);
                            }
                            paradaActual = null;
                        }
                        else if (insideScheduledStopPoint && "Location".equals(endName))
                        {
                            insideLocation = false;
                        }
                        break;
                }
            }

            reader.close();
        }

        return map;
    }

    private Map<String, Linea> loadLineas(String filename) throws Exception
    {
        Map<String, Linea> map = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (InputStream in = new FileInputStream(filename))
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            Linea lineaActual = null;
            boolean insideLine = false;

            while (reader.hasNext())
            {
                int event = reader.next();

                switch (event)
                {
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = reader.getLocalName();

                        if ("Line".equals(localName))
                        {
                            insideLine = true;
                            lineaActual = new Linea();
                            lineaActual.id = reader.getAttributeValue(null, "id");
                        }
                        else if (insideLine && "Name".equals(localName))
                        {
                            lineaActual.nombre = reader.getElementText();
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endName = reader.getLocalName();

                        if ("Line".equals(endName))
                        {
                            insideLine = false;
                            if(lineaActual != null)
                            {
                                map.put(lineaActual.id, lineaActual);
                            }
                            lineaActual = null;
                        }
                        break;
                }
            }

            reader.close();
        }

        return map;
    }

    private Map<String, Horario> loadHorario(String filename) throws Exception
    {
        Map<String, Horario> map = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (InputStream in = new FileInputStream(filename))
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            Horario horarioActual = null;
            boolean insideServiceJourney = false;
            boolean insideRouteView = false;

            while (reader.hasNext())
            {
                int event = reader.next();

                switch (event)
                {
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = reader.getLocalName();

                        if ("ServiceJourney".equals(localName))
                        {
                            insideServiceJourney = true;
                            horarioActual = new Horario();
                            horarioActual.id = reader.getAttributeValue(null, "id");
                        }
                        else if (insideServiceJourney && "LineRef".equals(localName))
                        {
                            String id = reader.getAttributeValue(null, "ref");
                            id = id.trim();
                            if(id != null && id.length() > 1)
                            {
                                String recorte = id.substring(id.length() - 1);
                                if(recorte.equals(":"))
                                {
                                    id = id.substring(0, id.length() -1);
                                }
                            }

                            horarioActual.idLinea = id;
                        }
                        else if (insideServiceJourney && "RouteView".equals(localName))
                        {
                            insideRouteView = true;
                        }
                        else if (insideRouteView && "LinkSequenceProjectionRef".equals(localName))
                        {
                            horarioActual.idShape = reader.getAttributeValue(null, "ref");
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endName = reader.getLocalName();

                        if ("RouteView".equals(endName))
                        {
                            insideRouteView = false;

                        }
                        else if ("ServiceJourney".equals(endName))
                        {
                            map.put(horarioActual.id, horarioActual);
                            insideServiceJourney = false;
                            horarioActual = null;
                        }
                        break;
                }
            }

            reader.close();
        }

        return map;
    }

    private Map<String, List<HoraParada>> loadHorasParadas(String filename) throws Exception
    {
        Map<String, List<HoraParada>> map = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (InputStream in = new FileInputStream(filename))
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            String idHorarioActual = null;
            List<HoraParada> listaHoras = null;
            HoraParada horaParadaActual = null;

            boolean insideServiceJourney = false;
            boolean insideCalls = false;
            boolean insideCall = false;
            boolean insideArrival = false;
            boolean insideDeparture = false;
            String currentElement = "";

            while (reader.hasNext())
            {
                int event = reader.next();

                switch (event)
                {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElement = reader.getLocalName();

                        if ("ServiceJourney".equals(currentElement))
                        {
                            insideServiceJourney = true;
                            idHorarioActual = reader.getAttributeValue(null, "id");
                            idHorarioActual = idHorarioActual.trim();

                            listaHoras = new ArrayList<>();
                        }
                        else if (insideServiceJourney && "calls".equals(currentElement))
                        {
                            insideCalls = true;
                        }
                        else if (insideCalls && "Call".equals(currentElement))
                        {
                            insideCall = true;
                            horaParadaActual = new HoraParada();
                            horaParadaActual.idHorario = idHorarioActual;

                            String order = reader.getAttributeValue(null, "order");
                            if (order != null) {
                                horaParadaActual.orden = Integer.parseInt(order);
                            }
                        }
                        else if (insideCall && "ScheduledStopPointRef".equals(currentElement))
                        {
                            String ref = reader.getAttributeValue(null, "ref");
                            if (ref != null) {
                                horaParadaActual.idParada = ref;
                            }
                        }
                        else if (insideCall && "Time".equals(currentElement))
                        {
                            String time = reader.getElementText();
                            if (insideArrival)
                            {
                                horaParadaActual.horaLlegada = time;
                            } else if (insideDeparture)
                            {
                                horaParadaActual.horaSalida = time;
                            }
                        }
                        else if (insideCall && "Arrival".equals(currentElement))
                        {
                            insideArrival = true;
                        }
                        else if (insideCall && "Departure".equals(currentElement))
                        {
                            insideDeparture = true;
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endElement = reader.getLocalName();

                        if ("Call".equals(endElement) && insideCall)
                        {
                            listaHoras.add(horaParadaActual);
                            insideCall = false;
                            horaParadaActual = null;
                        }
                        else if ("calls".equals(endElement))
                        {
                            insideCalls = false;
                        }
                        else if ("Arrival".equals(currentElement))
                        {
                            insideArrival = false;
                        }
                        else if ("Departure".equals(currentElement))
                        {
                            insideDeparture = false;
                        }
                        else if ("ServiceJourney".equals(endElement))
                        {
                            if (idHorarioActual != null && listaHoras != null)
                            {
                                listaHoras.sort(Comparator.comparingInt(st -> st.orden));
                                map.put(idHorarioActual, listaHoras);
                            }
                            insideServiceJourney = false;
                            idHorarioActual = null;
                            listaHoras = null;
                        }
                        break;
                }
            }
            reader.close();
        }

        return map;
    }

    private Map<String, Dibujito> loadShapes(String filename) throws Exception
    {
        Map<String, Dibujito> map = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (InputStream in = new FileInputStream(filename))
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            String idHorarioActual = null;
            ArrayList<String> posicionesActuales = null;
            Dibujito shapeActual = null;

            boolean insideServiceJourney = false;
            boolean insideLine = false;

            String currentElement = "";

            while (reader.hasNext())
            {
                int event = reader.next();

                switch (event)
                {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElement = reader.getLocalName();

                        if ("ServiceJourney".equals(currentElement))
                        {
                            insideServiceJourney = true;
                            idHorarioActual = reader.getAttributeValue(null, "id");
                            idHorarioActual = idHorarioActual.trim();

                            posicionesActuales = new ArrayList<>();

                            shapeActual = new Dibujito();
                        }
                        else if (insideServiceJourney && "LinkSequenceProjection".equals(currentElement))
                        {
                            shapeActual.id = reader.getAttributeValue(null, "id");
                        }
                        else if (insideServiceJourney && "gml:LineString".contains(currentElement))
                        {
                            insideLine = true;
                        }
                        else if (insideLine && "gml:pos".contains(currentElement))
                        {
                            posicionesActuales.add(reader.getElementText());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endElement = reader.getLocalName();

                        if ("ServiceJourney".equals(endElement))
                        {
                            if (idHorarioActual != null && posicionesActuales != null)
                            {
                                shapeActual.puntosString = posicionesActuales;
                                shapeActual.procesar();
                                map.put(shapeActual.id, shapeActual);
                            }
                            insideServiceJourney = false;
                            idHorarioActual = null;
                            posicionesActuales = null;
                            shapeActual = null;
                        }
                        break;
                }
            }
            reader.close();
        }

        return map;
    }


}
