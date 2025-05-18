package com.ehunzango.unigo.router.adapters;

import com.ehunzango.unigo.router.entities.Line;
import com.ehunzango.unigo.router.entities.Node;
import com.opencsv.bean.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

// for more info about GTFS: https://gtfs.org/documentation/schedule/reference/
public class GTFSAdapter implements IDataAdapter {

    // TODO: check the validity of this code blob :)
    // TODO: make some visualizing with a map, the points and the path...
    // TODO: check the dates and frequencies, and take into account the calendar bs...
    // TODO: bidirectional routes are a thing? if yes just make a function to clone + invert the Line and check for flags...

    @Override
    public boolean load(String path, List<Line> lines) {
        try {
            // TODO: TIME TEST THIS SHIT
            Map<String, Stop> stopMap = loadStops(path + "/stops.txt");
            Map<String, Route> routeMap = loadRoutes(path + "/routes.txt");
            Map<String, Trip> tripMap = loadTrips(path + "/trips.txt");
            Map<String, List<StopTime>> stopTimeMap = loadStopTimes(path + "/stop_times.txt");

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
            GTFSAdapter.generateEntities(lines, coolRoutes, stopMap);

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

    /* loadX *****************************************************************/

    private Map<String, Stop> loadStops(String filename) throws Exception {
        List<Stop> stops = loadCsv(filename, Stop.class);
        Map<String, Stop> map = new HashMap<>();
        for (Stop s : stops) map.put(s.stop_id, s);
        return map;
    }

    private Map<String, Route> loadRoutes(String filename) throws Exception {
        List<Route> routes = loadCsv(filename, Route.class);
        routes.stream()
                .limit(10)
                .forEach(st -> System.out.println("trip_id: '" + st.route_id + "' " + st.route_short_name));
        Map<String, Route> map = new HashMap<>();
        for (Route r : routes) map.put(r.route_id, r);
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

    private Map<String, List<CalendarDate>> loadCalendarDates(String filename) throws Exception {
        List<CalendarDate> calendarDates = loadCsv(filename, CalendarDate.class);
        Map<String, List<CalendarDate>> map = new HashMap<>();
        for (CalendarDate cd : calendarDates) {
            map.computeIfAbsent(cd.service_id, k -> new ArrayList<>()).add(cd);
        }
        return map;
    }

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

    // NOTE: NO BORRAR Q LA LIBREARIA ES UNA KK Y PARA DEBUGEAR HAY Q INVOCAR A CUTHULHU
    // private <T> List<T> loadCsv(String filename, Class<T> clazz) throws Exception {
    //     List<T> result;
    //     System.out.println("##########################################");
    //     try (Reader reader = new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8)) {
    //         HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
    //         strategy.setType(clazz);
    //
    //         CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
    //                 .withMappingStrategy(strategy)
    //                 .withSeparator(',')
    //                 .withIgnoreLeadingWhiteSpace(true)
    //                 .withThrowExceptions(false)  // Capture exceptions instead
    //                 .build();
    //         result = csvToBean.parse(); // <- parse happens here
    //         System.out.println("✅ Parsed rows: " + result.size());
    //
    //         // Log any exceptions that occurred
    //         List<CsvException> exceptions = csvToBean.getCapturedExceptions();
    //         if (!exceptions.isEmpty()) {
    //             System.err.println("❌ Found " + exceptions.size() + " parsing errors:");
    //             for (CsvException e : exceptions) {
    //                 System.err.println("  🔥 Line " + e.getLineNumber() + ": " + e.getMessage());
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.err.println("💥 Top-level CSV error: " + e.getMessage());
    //         e.printStackTrace();
    //         throw e;
    //     }
    //     System.out.println("##########################################");
    //     return result;
    // }

    /* POJOs *****************************************************************/

    // GTFS types (see more: https://gtfs.org/documentation/schedule/reference/#field-types)
    // Date:        String in format YYYYMMDD
    // Time:        String in format HH:MM:SS (or H:MM:SS)
    // Enum:     int (used for fields like route_type, exception_type, etc.)
    // ID:          String (UTF-8)
    // Latitude:    float (-90 <= lat <= 90)
    // Longitude:   float (-180 <= lat <= 180)

    // NOTE: not all columns need to be defined in the class. Just use the column names
    //       and map them to the correct Java types, e boila.

    // POJOs (Plain Old Java Object)
    public static class Stop { // stops.txt
        public Stop() {
        }

        @CsvBindByName
        public String stop_id;
        @CsvBindByName
        public String stop_name;
        @CsvBindByName
        public double stop_lat;
        @CsvBindByName
        public double stop_lon;

        // ignore
        // @CsvIgnore public String stop_code;
        // @CsvIgnore public String tts_stop_name;
        // @CsvIgnore public String stop_desc;
        // @CsvIgnore public String zone_id;
        // @CsvIgnore public String stop_url;
        // @CsvIgnore public String location_type;
        // @CsvIgnore public String parent_station;
        // @CsvIgnore public String stop_timezone;
        // @CsvIgnore public String wheelchair_boarding;
        // @CsvIgnore public String level_id;
        // @CsvIgnore public String platform_code;
    }

    public static class Route { // routes.txt
        public Route() {
        }

        @CsvBindByName
        public String route_id;
        @CsvBindByName
        public String route_short_name;
        @CsvBindByName
        public int route_type;
    }

    public static class StopTime { // stop_times.txt
        public StopTime() {
        }

        @CsvBindByName
        public String trip_id;
        @CsvBindByName
        public String arrival_time;
        @CsvBindByName
        public String departure_time;
        @CsvBindByName
        public String stop_id;
        @CsvBindByName
        public int stop_sequence;
    }

    public static class Trip { // trips.txt
        public Trip() {
        }

        @CsvBindByName
        public String route_id;
        @CsvBindByName
        public String service_id;
        @CsvBindByName
        public String trip_id;
        // @CsvBindByName public String direction_id; // TODO?? is it necessary??
        @CsvBindByName
        public String shape_id;
    }

    public static class Calendar { // calendar.txt
        public Calendar() {
        }

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
    }

    public static class CalendarDate { // calendar_dates.txt
        public CalendarDate() {
        }

        public String service_id;
        public String date; // YYYYMMDD
        public int exception_type; // 1 = added, 2 = removed
    }

    public static class Shape { // shapes.txt
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
