package com.ehunzango.unigo.router.adapters;

import com.ehunzango.unigo.router.entities.Line;
import com.ehunzango.unigo.router.entities.Node;
import com.opencsv.bean.*;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Persister;

// for more info about GTFS: https://gtfs.org/documentation/schedule/reference/
public class NETEXAdapter implements IDataAdapter {

    // TODO: check the validity of this code blob :)
    // TODO: make some visualizing with a map, the points and the path...
    // TODO: check the dates and frequencies, and take into account the calendar bs...
    // TODO: bidirectional routes are a thing? if yes just make a function to clone + invert the Line and check for flags...

    @Override
    public boolean load(String path, List<Line> lines) {
        try {
            // TODO: TIME TEST THIS SHIT
            Map<String, Stop> stopMap = loadStops(path + "/stops.xml");
            Map<String, Route> routeMap = loadRoutes(path + "/routes.xml");
            Map<String, Trip> tripMap = loadTrips(path + "/trips.zml");
            Map<String, List<StopTime>> stopTimeMap = loadStopTimes(path + "/stop_times.xml");

            System.out.println("stop count:     " + stopMap.size());
            System.out.println("route count:    " + routeMap.size());
            System.out.println("trip count:     " + tripMap.size());
            System.out.println("stopTime count: " + stopTimeMap.size());
            int i = 0;
            for (Trip trip : tripMap.values()) {
                i++;
                if (trip.route_id == null) {
                    System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFf: " + i);
                    return false;
                }
            }

            Map<String, CoolRoute> coolRoutes = parseRoutes(routeMap, stopTimeMap, tripMap);
            NETEXAdapter.generateEntities(lines, coolRoutes, stopMap);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* gen entities **********************************************************/

    private static void generateEntities(List<Line> lines, Map<String, CoolRoute> coolRoutes, Map<String, Stop> stopMap) {
        for (Map.Entry<String, CoolRoute> entry : coolRoutes.entrySet()) {
            String routeId = entry.getKey();
            CoolRoute coolRoute = entry.getValue();

            if (coolRoute.stop_times == null || coolRoute.stop_times.isEmpty()) continue;

            // Get basic route info
            String lineName = (coolRoute.route_short_name == null) ? "??" : coolRoute.route_short_name;

            // Create new Line with empty start_times for now
            Line line = new Line(lineName, coolRoute.route_type, List.of());

            // Create nodes
            List<Node> nodes = new ArrayList<>();
            for (StopTime stopTime : coolRoute.stop_times) {
                Stop stop = stopMap.get(stopTime.stop_id);
                if (stop == null) {
                    System.err.println("Missing stop for ID: " + stopTime.stop_id);
                    continue;
                }
                Node node = new Node(stop.stop_lat, stop.stop_lon, line);
                nodes.add(node);
            }

            // Skip if no valid nodes
            if (nodes.isEmpty()) continue;

            // Create deltas
            List<Float> deltas = new ArrayList<>();
            Node prev = nodes.get(0);
            for (int i = 1; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                deltas.add((float) prev.dist(node));
                prev = node;
            }
            deltas.add(Float.POSITIVE_INFINITY);

            // Add nodes and deltas to the line
            line.addNode(nodes, deltas);

            // Add line to result list
            lines.add(line);
        }
    }

    class CoolRoute {
        public String route_short_name;
        public Line.Type route_type;
        public List<StopTime> stop_times;
        public List<Date> start_dates; // times the routes start
        public List<Float> deltas; // time between each stop

        public CoolRoute(String route_short_name, int route_type) {
            this.route_short_name = route_short_name;
            this.route_type = Line.Type.fromGtfsCode(route_type);
        }
    }

    private Map<String, CoolRoute> parseRoutes(
            Map<String, Route> routeMap,
            Map<String, List<StopTime>> stopTimeMap,
            Map<String, Trip> tripMap
    ) throws Exception {
        Map<String, CoolRoute> coolRouteMap = new HashMap<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        // Iterate through the Route map to create CoolRoute objects
        for (Route route : routeMap.values()) {
            String routeId = route.route_id;
            String routeShortName = route.route_short_name;
            int routeType = route.route_type;

            // Create a CoolRoute object
            CoolRoute coolRoute = new CoolRoute(routeShortName, routeType);
            coolRoute.stop_times = new ArrayList<>();
            coolRoute.deltas = new ArrayList<>();
            coolRoute.start_dates = new ArrayList<>();

            // Collect StopTimes for trips that belong to this route
            for (Trip trip : tripMap.values()) {
                if (!trip.route_id.equals(routeId)) continue;

                List<StopTime> stopTimes = stopTimeMap.get(trip.trip_id);
                if (stopTimes == null || stopTimes.isEmpty()) continue;

                coolRoute.stop_times.addAll(stopTimes);


                // Step 1: Calculate the start dates (based on your calendar data, if available)
                // For simplicity, let's assume we are extracting start dates directly from StopTime arrival_time
                Date startTime = timeFormat.parse(stopTimes.get(0).arrival_time);
                coolRoute.start_dates.add(startTime);

                // Step 2: Calculate the deltas (time differences between consecutive stops)
                List<Float> deltas = new ArrayList<>();
                deltas.add(0.0f);
                Date previousTime = null;
                for (StopTime st : coolRoute.stop_times) {
                    Date arrivalTime = timeFormat.parse(st.arrival_time);

                    if (previousTime != null) { // Calculate the delta
                        long deltaMillis = arrivalTime.getTime() - previousTime.getTime();
                        deltas.add(deltaMillis / 1000f);  // Convert milliseconds to seconds
                    }
                    previousTime = arrivalTime;
                }
                coolRoute.deltas = deltas;

                // Add the CoolRoute to the result map
                coolRouteMap.put(routeId, coolRoute);
            }
        }

        return coolRouteMap;
    }

    // Utility method to parse time strings (HH:MM:SS or H:MM:SS) into Date objects
    private Date parseTimeToDate(String time) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.parse(time);
    }

    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                                  LOAD                                    |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    private Map<String, Stop> loadStops(String filename) throws Exception
    {
        Persister serializer = new Persister();
        File file = new File(filename);

        PublicationDeliveryXml data = serializer.read(PublicationDeliveryXml.class, file);

        Map<String, Stop> map = new HashMap<>();
        if (data.stopPoints != null && data.stopPoints.stops != null) {
            for (Stop s : data.stopPoints.stops) {
                map.put(s.stop_id, s);
            }
        }
        return map;
    }

    private Map<String, Route> loadRoutes(String filename) throws Exception {
        Persister serializer = new Persister();
        File file = new File(filename);

        PublicationDelivery pubDelivery = serializer.read(PublicationDelivery.class, file);

        Map<String, Route> map = new HashMap<>();
        if (pubDelivery.dataObjects != null &&
                pubDelivery.dataObjects.siteFrame != null &&
                pubDelivery.dataObjects.siteFrame.lines != null &&
                pubDelivery.dataObjects.siteFrame.lines.routes != null) {

            for (Route r : pubDelivery.dataObjects.siteFrame.lines.routes) {
                map.put(r.route_id, r);
            }
        }
        return map;
    }



    public static void printObjectFields(Object obj) {
        Class<?> clazz = obj.getClass();
        System.out.println("Class: " + clazz.getSimpleName());

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true); // Make private fields accessible
            try {
                Object value = field.get(obj);
                System.out.println(field.getName() + " = " + value);
            } catch (IllegalAccessException e) {
                System.out.println(field.getName() + " = [access denied]");
            }
        }
    }

    private Map<String, List<StopTime>> loadStopTimes(String filename) throws Exception {
        List<StopTime> stopTimes = loadCsv(filename, StopTime.class);
        Map<String, List<StopTime>> map = new HashMap<>();
        for (StopTime st : stopTimes) map.computeIfAbsent(st.trip_id, k -> new ArrayList<>()).add(st);
        stopTimes.stream()
                .limit(1)
                .forEach(st -> printObjectFields(st));
        // sort sequence
        map.values().forEach(list -> list.sort(Comparator.comparingInt(stop -> stop.stop_sequence)));
        return map;
    }

    private Map<String, Trip> loadTrips(String filename) throws Exception {
        List<Trip> trips = loadCsv(filename, Trip.class);
        Map<String, Trip> map = new HashMap<>();
        for (Trip trip : trips) map.put(trip.trip_id, trip);
        return map;
    }

    private Map<String, Calendar> loadCalendar(String filename) throws Exception {
        List<Calendar> calendars = loadCsv(filename, Calendar.class);
        Map<String, Calendar> map = new HashMap<>();
        for (Calendar c : calendars) map.put(c.service_id, c);
        return map;
    }

    /*
    private Map<String, List<CalendarDate>> loadCalendarDates(String filename) throws Exception {
        List<CalendarDate> calendarDates = loadCsv(filename, CalendarDate.class);
        Map<String, List<CalendarDate>> map = new HashMap<>();
        for (CalendarDate cd : calendarDates) {
            map.computeIfAbsent(cd.service_id, k -> new ArrayList<>()).add(cd);
        }
        return map;
    }*/

    private Map<String, List<Shape>> loadShapes(String filename) throws Exception {
        List<Shape> shapes = loadCsv(filename, Shape.class);
        Map<String, List<Shape>> map = new HashMap<>();
        for (Shape shape : shapes) {
            map.computeIfAbsent(shape.shape_id, k -> new ArrayList<>()).add(shape);
        }
        // Optional: sort each shape point list by sequence
        for (List<Shape> shapeList : map.values()) {
            shapeList.sort(Comparator.comparingInt(s -> s.shape_pt_sequence));
        }
        return map;
    }

    private <T> List<T> loadCsv(String filename, Class<T> clazz) throws Exception {
        return new CsvToBeanBuilder<T>(new FileReader(filename))
                .withType(clazz)
                .withSeparator(',')
                .build()
                .parse();
    }


    //              +--------------------------------------------------------------------------+
    //              |                                                                          |
    //              |                      DEFINICION DE CLASES PARA XML                       |
    //              |                                                                          |
    //              +--------------------------------------------------------------------------+

    // STOPS -----------------------------------------------------------------------------
    @Root(name = "ScheduledStopPoint", strict = false)
    public class Stop
    {
        @org.simpleframework.xml.Attribute(name = "id")
        public String stop_id;
        @Element(name = "Name")
        public String stop_name;
        @Element(name = "Latitude")
        public double stop_lat;
        @Element(name = "Longitude")
        public double stop_lon;

        @org.simpleframework.xml.Attribute(name = "id")

        public Stop() {}
    }

    @Root(name = "StopPoints", strict = false)
    public class StopPointsXml {
        @ElementList(entry = "ScheduledStopPoint", inline = true, required = false)
        public List<Stop> stops;

        public StopPointsXml() {}
    }

    @Root(name = "PublicationDelivery", strict = false)
    public class PublicationDeliveryXml {
        @Element(name = "StopPoints", required = false)
        public StopPointsXml stopPoints;

        public PublicationDeliveryXml() {}
    }

    // LINES -----------------------------------------------------------------------------
    @Root(name = "Line", strict = false)
    public class Route { // routes.xml
        public Route() {
        }

        @org.simpleframework.xml.Attribute(name = "id")
        public String route_id;
        @Element(name = "PublicCode")
        public String route_short_name;
        @Element(name = "Description", required = false)
        public String route_long_name;
        public int route_type = 3;
    }

    @Root(name = "PublicationDelivery", strict = false)
    public class PublicationDelivery {
        @Element(name = "dataObjects", required = false)
        public DataObjects dataObjects;
    }

    @Root(name = "dataObjects", strict = false)
    public class DataObjects {
        @Element(name = "SiteFrame", required = false)
        public SiteFrame siteFrame;
    }

    @Root(name = "SiteFrame", strict = false)
    public class SiteFrame {
        @Element(name = "lines", required = false)
        public Lines lines;
    }

    @Root(name = "lines", strict = false)
    public class Lines {
        @ElementList(entry = "Line", inline = true, required = false)
        public List<Route> routes;
    }

    // STOPTIME -----------------------------------------------------------------------------
    @Root(name = "Call", strict = false)
    public static class StopTime { // stop_times.txt
        public StopTime() {
        }

        // Campo manualmente inyectado al parsear desde ServiceJourney
        public String trip_id;

        public String arrival_time;
        public String departure_time;
        public String stop_id;
        public int stop_sequence;

        // Campos auxiliares para mapear el XML NetEx
        @Element(name = "Arrival", required = false)
        private Arrival arrival;

        @Element(name = "Departure", required = false)
        private Departure departure;

        @Element(name = "ScheduledStopPointRef", required = false)
        private ScheduledStopPointRef stopPointRef;

        @Attribute(name = "order", required = false)
        private Integer order;

        // Método postprocesamiento para transformar datos XML al formato GTFS
        public void finalizeFromXML(String tripId) {
            this.trip_id = tripId;
            this.arrival_time = (arrival != null) ? arrival.time : null;
            this.departure_time = (departure != null) ? departure.time : null;
            this.stop_id = (stopPointRef != null) ? stopPointRef.ref : null;
            this.stop_sequence = (order != null) ? order : 0;
        }

        // Clases internas para parsear subelementos
        public static class Arrival {
            @Element(name = "Time", required = false)
            public String time;
        }

        public static class Departure {
            @Element(name = "Time", required = false)
            public String time;
        }

        public static class ScheduledStopPointRef {
            @Attribute(name = "ref", required = false)
            public String ref;
        }
    }

    @Root(name = "ServiceJourney", strict = false)
    public class Trip {

        public Trip() {
        }

        @Element(name = "LineRef", required = false)
        public String route_id;

        @Element(name = "OperatingDayRef", required = false)
        public String service_id;

        @Element(name = "id", required = false)
        public String trip_id;

        @Element(name = "JourneyPatternRef", required = false)
        public String shape_id;
    }

    @Root(name = "ServiceCalendarFrame", strict = false)
    public class Calendar {

        public Calendar() {
        }

        // Mismos campos que GTFS
        public String service_id;
        public boolean monday;
        public boolean tuesday;
        public boolean wednesday;
        public boolean thursday;
        public boolean friday;
        public boolean saturday;
        public boolean sunday;
        public String start_date; // YYYYMMDD
        public String end_date;   // YYYYMMDD

        // XML mappings
        @Element(name = "ServiceCalendar", required = false)
        private ServiceCalendar serviceCalendar;

        @ElementList(name = "dayTypes", inline = true, required = false)
        private List<DayType> dayTypes;

        // Método para extraer los campos GTFS
        public void finalizeFromXML() {
            if (serviceCalendar != null) {
                this.service_id = serviceCalendar.id;
                this.start_date = serviceCalendar.fromDate.replace("-", "");
                this.end_date = serviceCalendar.toDate.replace("-", "");
            }

            if (dayTypes != null) {
                for (DayType dt : dayTypes) {
                    if (dt.properties != null) {
                        for (PropertyOfDay prop : dt.properties) {
                            if (prop.daysOfWeek != null) {
                                String[] days = prop.daysOfWeek.trim().split("\\s+");
                                for (String day : days) {
                                    switch (day.toLowerCase()) {
                                        case "monday": this.monday = true; break;
                                        case "tuesday": this.tuesday = true; break;
                                        case "wednesday": this.wednesday = true; break;
                                        case "thursday": this.thursday = true; break;
                                        case "friday": this.friday = true; break;
                                        case "saturday": this.saturday = true; break;
                                        case "sunday": this.sunday = true; break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clases internas para representar el XML

        public class ServiceCalendar {
            @Element(name = "FromDate")
            public String fromDate;

            @Element(name = "ToDate")
            public String toDate;

            @org.simpleframework.xml.Attribute(name = "id", required = false)
            public String id;
        }

        public class DayType {
            @ElementList(name = "properties", inline = false, required = false)
            public List<PropertyOfDay> properties;
        }

        public class PropertyOfDay {
            @Element(name = "DaysOfWeek", required = false)
            public String daysOfWeek;
        }
    }

    /*
    @Root(name = "AvailabilityCondition", strict = false)
    public static class CalendarDate { // calendar_dates.txt
        public CalendarDate() {
        }

        @Attribute(name = "id")
        public String id;

        @ElementList(entry = "AvailabilityCondition", inline = true, required = false)
        public List<AvailabilityConditionXml> availabilityConditions;

        public String service_id;
        public String date; // YYYYMMDD
        public int exception_type; // 1 = added, 2 = removed
    }*/

    public class Shape { // shapes.txt
        public Shape() {
        }

        public String shape_id;
        public double shape_pt_lat;
        public double shape_pt_lon;
        public int shape_pt_sequence;
        public Double shape_dist_traveled; // optional
    }

    // NOTE: frequencies.txt is not provided :(
    // public static class Frequency { // frequencies.txt
    //     public Frequency() {}
    //     public String trip_id;
    //     public String start_time; // HH:MM:SS
    //     public String end_time;   // HH:MM:SS
    //     public int headway_secs;  // Seconds between departures
    // }

    // NOTE: we have more required and optional files, but I don't think we any of them:
    //         - fare_XYZ.txt: no pixco que hacen.
    //         - agency.txt: is bs
    //         - levels.txt: is bs
    //         - attributions.txt: is bs
}
