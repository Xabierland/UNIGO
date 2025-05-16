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

import kotlin.text.UStringsKt;

// for more info about GTFS: https://gtfs.org/documentation/schedule/reference/
public class NETEXAdapter implements IDataAdapter
{

    // TODO: check the validity of this code blob :)
    // TODO: make some visualizing with a map, the points and the path...
    // TODO: check the dates and frequencies, and take into account the calendar bs...
    // TODO: bidirectional routes are a thing? if yes just make a function to clone + invert the Line and check for flags...

    @Override
    public boolean load(String path, List<Line> lines) {
        try {
            // Listo
            Map<String, Stop> stopMap = loadStops(path + "/stops.xml");
            Map<String, Route> routeMap = loadRoutes(path + "/routes.xml");
            Map<String, Trip> tripMap = loadTrips(path + "/trips.xml");
            Map<String, List<StopTime>> stopTimeMap = loadStopTimes(path + "/stop_times.xml");

            System.out.println("stop count:     " + stopMap.size());
            System.out.println("route count:    " + routeMap.size());
            System.out.println("trip count:     " + tripMap.size());
            System.out.println("stopTime count: " + stopTimeMap.size());

            int i = 0;
            for (Trip trip : tripMap.values())
            {
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

        PublicationDeliveryStopsXml data = serializer.read(PublicationDeliveryStopsXml.class, file);

        Map<String, Stop> map = new HashMap<>();
        if(data.dataObjects != null && data.dataObjects.composite != null && data.dataObjects.composite.frames != null && data.dataObjects.composite.frames.serviceFrame != null && data.dataObjects.composite.frames.serviceFrame.scheduledStopPoints != null && data.dataObjects.composite.frames.serviceFrame.scheduledStopPoints.stops != null)
        {
            for (Stop s : data.dataObjects.composite.frames.serviceFrame.scheduledStopPoints.stops)
            {
                s.cargar();
                map.put(s.stop_id, s);
            }
        }

        return map;
    }

    private Map<String, Route> loadRoutes(String filename) throws Exception
    {
        Persister serializer = new Persister();
        File file = new File(filename);

        PublicationDeliveryRoutesXML pubDelivery = serializer.read(PublicationDeliveryRoutesXML.class, file);

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

    private Map<String, Trip> loadTrips(String filename) throws Exception
    {
        Persister serializer = new Persister();
        File file = new File(filename);

        PublicationDeliveryTripsXML pubDelivery = serializer.read(PublicationDeliveryTripsXML.class, file);

        Map<String, Trip> map = new HashMap<>();
        if (pubDelivery.dataObjects != null &&
                pubDelivery.dataObjects.timetableFrame != null &&
                pubDelivery.dataObjects.timetableFrame.vehicleJourneys != null &&
                pubDelivery.dataObjects.timetableFrame.vehicleJourneys.serviceJourneys != null)
        {
            for (Trip t : pubDelivery.dataObjects.timetableFrame.vehicleJourneys.serviceJourneys)
            {
                t.cargar();
                map.put(t.trip_id, t);
            }
        }
        return map;
    }

    private Map<String, List<StopTime>> loadStopTimes(String filename) throws Exception
    {

        Persister serializer = new Persister();
        File file = new File(filename);

        PublicationDeliveryStopTimesXML pubDelivery = serializer.read(PublicationDeliveryStopTimesXML.class, file);

        Map<String, List<StopTime>> map = new HashMap<>();
        // trip_id, ''   ''
        if (pubDelivery.dataObjects != null &&
                pubDelivery.dataObjects.timetableFrame != null &&
                pubDelivery.dataObjects.timetableFrame.vehicleJourneys != null &&
                pubDelivery.dataObjects.timetableFrame.vehicleJourneys.serviceJourneys != null)
        {
            for (ServiceJourneyStopTimesXML service : pubDelivery.dataObjects.timetableFrame.vehicleJourneys.serviceJourneys)
            {
                String trip_id = service.trip_id;
                CallsStopTimesXML calls = service.calls;

                if(calls != null)
                {
                    List<StopTime> stopTimes = new ArrayList<>();
                    for (CallStopTimesXML call : calls.calls)
                    {
                        StopTime stopTime = new StopTime();
                        stopTime.cargar(call, trip_id);
                        stopTimes.add(stopTime);
                    }
                    map.put(trip_id, stopTimes);
                }
            }
        }

        map.values().forEach(list -> list.sort(Comparator.comparingInt(stop -> stop.stop_sequence)));

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


    @Root(name = "PublicationDelivery", strict = false)
    public static class PublicationDeliveryStopsXml {
        //@ElementList(entry = "ScheduledStopPoint", inline = true, required = false)
        //public List<Stop> stops;

        @Element(name = "dataObjects")
        public DataObjetcsStopsXML dataObjects;

        public PublicationDeliveryStopsXml() {}
    }

    @Root(name = "dataObjects", strict = false)
    public static class DataObjetcsStopsXML {
        @Element(name = "CompositeFrame")
        public CompositeFrameStopsXML composite;

        public DataObjetcsStopsXML() {}
    }

    @Root(name = "CompositeFrame", strict = false)
    public static class CompositeFrameStopsXML {
        @Element(name = "frames")
        public FramesStopsXML frames;

        public CompositeFrameStopsXML() {}
    }

    @Root(name = "frames", strict = false)
    public static class FramesStopsXML {
        @Element(name = "ServiceFrame")
        public ServiceFrameStopsXML serviceFrame;

        public FramesStopsXML() {}
    }

    @Root(name = "ServiceFrame", strict = false)
    public static class ServiceFrameStopsXML {
        @Element(name = "scheduledStopPoints")
        public ScheduledStopPointsRoutesXML scheduledStopPoints;

        public ServiceFrameStopsXML() {}
    }

    @Root(name = "scheduledStopPoints", strict = false)
    public static class ScheduledStopPointsRoutesXML
    {
        @ElementList(entry = "ScheduledStopPoint", inline = true, required = false)
        public List<Stop> stops;

        public ScheduledStopPointsRoutesXML() {}
    }

    @Root(name = "ScheduledStopPoint", strict = false)
    public static class Stop
    {
        @org.simpleframework.xml.Attribute(name = "id")
        public String stop_id;
        @Element(name = "Name")
        public String stop_name;

        public double stop_lat;
        public double stop_lon;

        @Element(name = "Location")
        public LocationStopXML location;

        public void cargar()
        {
            stop_lat = location.stop_lat;
            stop_lon = location.stop_lon;
        }


        public Stop() {}
    }

    @Root(name = "Location", strict = false)
    public static class LocationStopXML
    {
        @Element(name = "Latitude")
        public double stop_lat;
        @Element(name = "Longitude")
        public double stop_lon;


        public LocationStopXML() {}
    }



    // LINES -----------------------------------------------------------------------------

    @Root(name = "PublicationDelivery", strict = false)
    public static class PublicationDeliveryRoutesXML {
        @Element(name = "dataObjects", required = false)
        public DataObjectsRoutesXML dataObjects;
    }

    @Root(name = "dataObjects", strict = false)
    public static class DataObjectsRoutesXML {
        @Element(name = "SiteFrame", required = false)
        public SiteFrame siteFrame;
    }

    @Root(name = "SiteFrame", strict = false)
    public static class SiteFrame {
        @Element(name = "lines", required = false)
        public Lines lines;
    }

    @Root(name = "lines", strict = false)
    public static class Lines {
        @ElementList(entry = "Line", inline = true, required = false)
        public List<Route> routes;
    }

    @Root(name = "Line", strict = false)
    public static class Route { // routes.xml
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

    // TRIPS --------------------------------------------------------------
    /*
        @Root(name = "NOMBRE", strict = false)
    public class NOMBRE { // NOMBRE.xml
        public NOMBRE() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        //@Element(name = "PublicCode")
        //public String route_short_name;

        //@ElementList(entry = "Line", inline = true, required = false)
        //public List<Route> routes;
    }
     */
    @Root(name = "PublicationDelivery", strict = false)
    public static class PublicationDeliveryTripsXML { // trips.xml
        public PublicationDeliveryTripsXML() {
        }

        @Element(name = "dataObjects")
        public DataObjectsTripsXML dataObjects;
    }

    @Root(name = "dataObjects", strict = false)
    public static class DataObjectsTripsXML { // trips.xml
        public DataObjectsTripsXML() {
        }

        @Element(name = "TimetableFrame")
        public TimetableFrame timetableFrame;
    }

    @Root(name = "TimetableFrame", strict = false)
    public static class TimetableFrame { // trips.xml
        public TimetableFrame() {
        }

        @Element(name = "vehicleJourneys")
        public VehicleJourneysTripsXML vehicleJourneys;
    }

    @Root(name = "vehicleJourneys", strict = false)
    public static class VehicleJourneysTripsXML { // trips.xml
        public VehicleJourneysTripsXML() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        @ElementList(entry = "ServiceJourney", inline = true, required = false)
        public List<Trip> serviceJourneys;

        //@Element(name = "vehicleJourneys")
        //public VehicleJourneysTripsXML vehicleJourneys;
    }

    @Root(name = "vehicleJourneys", strict = false)
    public static class Trip { // trips.xml
        public Trip() {
        }

        @Element(name = "LineRef", required = false)
        public String route_id;

        public String service_id;

        @Element(name = "RouteView", required = false)
        public RouteViewTripsXML routeView;

        @org.simpleframework.xml.Attribute(name = "id")
        public String trip_id;

        @Element(name = "JourneyPatternRef", required = false)
        public String shape_id;


        public void cargar()
        {
            shape_id = routeView.shape_id;
        }
    }

    @Root(name = "RouteView", strict = false)
    public static class RouteViewTripsXML { // trips.xml
        public RouteViewTripsXML() {
        }

        @Element(name = "LinkSequenceProjectionRef", required = false)
        public String shape_id;

    }

    // STOPTIME -----------------------------------------------------------------------------
    /*
        @Root(name = "NOMBRE", strict = false)
    public class NOMBRE { // NOMBRE.xml
        public NOMBRE() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        //@Element(name = "PublicCode")
        //public String route_short_name;

        //@ElementList(entry = "Line", inline = true, required = false)
        //public List<Route> routes;
    }
     */

    @Root(name = "PublicationDelivery", strict = false)
    public static class PublicationDeliveryStopTimesXML { // NOMBRE.xml
        public PublicationDeliveryStopTimesXML() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        @Element(name = "dataObjects")
        public DataObjectsStopTimesXML dataObjects;

        //@ElementList(entry = "Line", inline = true, required = false)
        //public List<Route> routes;
    }

    @Root(name = "dataObjects", strict = false)
    public static class DataObjectsStopTimesXML { // NOMBRE.xml
        public DataObjectsStopTimesXML() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        @Element(name = "TimetableFrame")
        public TimetableFrameStopTimesXML timetableFrame;

        //@ElementList(entry = "Line", inline = true, required = false)
        //public List<Route> routes;
    }

    @Root(name = "TimetableFrame", strict = false)
    public static class TimetableFrameStopTimesXML { // NOMBRE.xml
        public TimetableFrameStopTimesXML() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        @Element(name = "vehicleJourneys")
        public VehicleJourneysStopTimesXML vehicleJourneys;

        //@ElementList(entry = "Line", inline = true, required = false)
        //public List<Route> routes;
    }

    @Root(name = "vehicleJourneys", strict = false)
    public static class VehicleJourneysStopTimesXML { // NOMBRE.xml
        public VehicleJourneysStopTimesXML() {
        }

        //@org.simpleframework.xml.Attribute(name = "id")
        //public String route_id;

        //@Element(name = "vehicleJourneys")
        //public VehicleJourneysstopTimesXML vehicleJourneys;

        @ElementList(entry = "ServiceJourney", inline = true, required = false)
        public List<ServiceJourneyStopTimesXML> serviceJourneys;
    }

    @Root(name = "ServiceJourney", strict = false)
    public static class ServiceJourneyStopTimesXML { // NOMBRE.xml
        public ServiceJourneyStopTimesXML() {
        }

        @org.simpleframework.xml.Attribute(name = "id")
        public String trip_id;

        @Element(name = "calls")
        public CallsStopTimesXML calls;

    }

    @Root(name = "calls", strict = false)
    public static class CallsStopTimesXML { // NOMBRE.xml
        public CallsStopTimesXML() {
        }

        @ElementList(entry = "Call", inline = true, required = false)
        public List<CallStopTimesXML> calls;
    }


    @Root(name = "Call", strict = false)
    public static class CallStopTimesXML { // stop_times.txt
        public CallStopTimesXML() {
        }

        @org.simpleframework.xml.Attribute(name = "order")
        public int order;

        @Element(name = "ScheduledStopPointRef")
        public String stop_id;

        public String arrival_time;
        public String departure_time;

        @Element(name = "Arrival")
        public ArrivalStopTimesXML arrival;

        @Element(name = "Departure")
        public DepartureStopTimesXML departure;

        public void cargar()
        {
            arrival_time = arrival.time;
            departure_time = departure.time;
        }

    }

    @Root(name = "Arrival", strict = false)
    public static class ArrivalStopTimesXML { // stop_times.txt
        public ArrivalStopTimesXML() {
        }
        @Element(name = "Time")
        public String time;

    }

    @Root(name = "Departure", strict = false)
    public static class DepartureStopTimesXML { // stop_times.txt
        public DepartureStopTimesXML() {
        }
        @Element(name = "Time")
        public String time;
    }

    public static class StopTime { // stop_times.txt
        public StopTime() {
        }
        public String trip_id;
        public String arrival_time;
        public String departure_time;
        public String stop_id;
        public int stop_sequence;

        public void cargar(CallStopTimesXML c, String tripId )
        {
            if(c != null)
            {
                c.cargar();

                trip_id = tripId;
                arrival_time = c.arrival_time;
                departure_time = c.departure_time;
                stop_id = c.stop_id;
                stop_sequence = c.order;
            }
        }
    }



    //------------------------------------------------------------------------------------------------------------

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
