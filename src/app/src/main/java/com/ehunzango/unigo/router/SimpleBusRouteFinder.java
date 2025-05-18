package com.ehunzango.unigo.router;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.checkerframework.checker.units.qual.A;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class SimpleBusRouteFinder
{
    private static SimpleBusRouteFinder instance;

    public boolean loaded = false;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

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
        public HashMap<String, Integer> orden;

        public Parada()
        {
            orden = new HashMap<>();
        }
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
        String idFecha;
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

    public class Fecha
    {
        String id;
        String diaSemana;
        ArrayList<DayOfWeek> dias;
        String fechaInicio;
        Date  inicio;
        String fechaFin;
        Date fin;

        public void procesar() {
            try
            {
                inicio = sdf.parse(fechaInicio);
                fin = sdf.parse(fechaFin);
            }
            catch (Exception e)
            {

            }

            ArrayList<String> diasSeparados = new ArrayList<>(List.of(diaSemana.split(" ")));

            dias = new ArrayList<>();

            for(String diaSemana : diasSeparados)
            {
                switch (diaSemana)
                {
                    case "Monday":
                        dias.add(DayOfWeek.MONDAY);
                        break;
                    case "Tuesday":
                        dias.add(DayOfWeek.TUESDAY);
                        break;
                    case "Wednesday":
                        dias.add(DayOfWeek.WEDNESDAY);
                        break;
                    case "Thursday":
                        dias.add(DayOfWeek.THURSDAY);
                        break;
                    case "Friday":
                        dias.add(DayOfWeek.FRIDAY);
                        break;
                    case "Saturday":
                        dias.add(DayOfWeek.SATURDAY);
                        break;
                    case "Sunday":
                        dias.add(DayOfWeek.SUNDAY);
                        break;

                }
            }




        }
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
                    if(parada.orden.get(linea.id) == null)
                    {
                        continue;
                    }

                    float distanciaActual = calcularDistancia(latitudOrigen, longitudOrigen, parada);
                    if(distanciaActual < minimaDistanciaOrigen)
                    {
                        minimaDistanciaOrigen = distanciaActual;
                        paradaOrigen = parada;
                    }
                }

                Log.d("mitag", "\t orden origen : " + paradaOrigen.orden.get(linea.id));
                Log.d("mitag", "\t Buscando llegada...");
                for (Parada parada : linea.paradas)
                {
                    if(parada.orden.get(linea.id) == null || paradaOrigen.orden.get(linea.id) == null || parada.orden.get(linea.id) < paradaOrigen.orden.get(linea.id))
                    {
                        continue;
                    }

                    float distanciaActual = calcularDistancia(latitudDestino, longitudDestino, parada);
                    if(distanciaActual < minimaDistanciaDestino)
                    {
                        minimaDistanciaDestino = distanciaActual;
                        paradaDestino = parada;
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


    // --------------------------------  CARGA DE DATOS  ----------------------------------------------

    public void cargarDatos(String path)
    {
        try
        {

            Log.d("mitag", "Cargando datos ...");

            Map<String, Fecha> mapaFechas = loadFechas(path + "/calendar.xml");
            Log.d("mitag", "Fechas cargados : " + mapaFechas.size());

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

            Date hoy = new Date();

            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();


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

                if(lineasCargadas.contains(idLineaActual))
                {
                    continue;
                }

                String idFecha = horario.idFecha;
                Fecha fechaActual = mapaFechas.get(idFecha);
                if(fechaActual == null)
                {
                    //Log.d("mitag", "\t fechaActual == null : " + idFecha);
                    continue;
                }

                if(fechaActual.dias == null || !fechaActual.dias.contains(dayOfWeek))
                {
                    //Log.d("mitag", "\t dias == null || dia de la semana no valido");
                    continue;
                }


                if (!(    hoy.equals(fechaActual.inicio) ||
                        hoy.equals(fechaActual.fin) ||
                        ((hoy.after(fechaActual.inicio) && hoy.before(fechaActual.fin)))
                    ))
                {
                    Log.d("mitag", "\t Fuera de fecha");
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

                    parada.orden.put(idLineaActual, horaParada.orden);
                    if(lineaActual.paradas == null)
                    {
                        lineaActual.paradas = new ArrayList<>();
                    }
                    lineaActual.paradas.add(parada);

                    //lineaActual.paradas = new ArrayList<Parada>(lineaActual.paradas.stream().sorted(Comparator.comparingInt(st -> st.orden)).collect(Collectors.toList()));
                }

                Dibujito shapeActual = mapaShapes.get(horario.idShape);
                if(shapeActual == null)
                {
                    Log.d("mitag", "\t \t shapeActual == null ");
                }
                else
                {/*
                    if(shapeActual.puntos != null)
                    {
                        Log.d("mitag", "\t \t shapeActual " + shapeActual.puntos.size() + " puntitos");
                    }
                    else
                    {
                        Log.d("mitag", "\t \t shapeActual.puntos == null ");
                    }*/

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
            if(lineas != null)
            {
                Log.d("mitag", "Pos cargado queda. Total " + lineas.size() + " lineas");
            }
            else
            {
                Log.d("mitag", "Pos cargado queda. Pero lineas==null");
            }

        }
        catch (Exception e)
        {
            Log.d("mitag", "\t Excepcion :(");
            if(e.getMessage() != null)
            {
                Log.d("mitag", e.getMessage());
            }
            else {
                Log.d("mitag", "e.getMessage() == null");
            }

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
                        else if (insideServiceJourney && "DayTypeRef".equals(localName))
                        {
                            String id = reader.getAttributeValue(null, "id");
                            id = id.replaceAll("DayType:", "");
                            id = id.trim();
                            horarioActual.idFecha = id;
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

    private Map<String, Fecha> loadFechas(String filename) throws Exception
    {
        Map<String, Fecha> map = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();


        try (InputStream in = new FileInputStream(filename))
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            Fecha fechaActual = null;

            boolean insideServiceCalendarFrame = false;

            String currentElement = "";

            while (reader.hasNext())
            {
                int event = reader.next();

                switch (event)
                {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElement = reader.getLocalName();

                        if ("ServiceCalendarFrame".equals(currentElement))
                        {
                            insideServiceCalendarFrame = true;
                            fechaActual = new Fecha();

                            String idFecha = reader.getAttributeValue(null, "id");

                            idFecha = idFecha.replace("ServiceCalendarFrame:", "");
                            idFecha = idFecha.trim();

                            fechaActual.id = idFecha;
                        }
                        else if (insideServiceCalendarFrame && "DaysOfWeek".contains(currentElement))
                        {
                            fechaActual.diaSemana = (reader.getElementText());
                        }
                        else if (insideServiceCalendarFrame && "FromDate".equals(currentElement))
                        {
                            fechaActual.fechaInicio = (reader.getElementText());
                        }
                        else if (insideServiceCalendarFrame && "ToDate".equals(currentElement))
                        {
                            fechaActual.fechaFin = (reader.getElementText());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endElement = reader.getLocalName();

                        if ("ServiceCalendarFrame".equals(endElement))
                        {
                            insideServiceCalendarFrame = false;
                            if (fechaActual != null)
                            {
                                fechaActual.procesar();
                                map.put(fechaActual.id, fechaActual);
                            }

                            fechaActual = null;
                        }
                        break;
                }
            }
            reader.close();
        }


        return map;
    }


}
