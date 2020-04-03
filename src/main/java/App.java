import com.google.gson.Gson;
import com.google.ortools.constraintsolver.*;
import example.Element;
import example.Example;
import example.Row;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class App {
    static {
        System.loadLibrary("jniortools");
    }

    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws IOException {
        DataModel data = new DataModel();
        Arrays.fill(data.demands, 1);
        Arrays.fill(data.vehicleCapacities, 6);
//        String[] originAddresses = {data.addresses[0]};
//        String[] destinationAddresses = {data.addresses[1]};
//        try {
//            Example example = sendRequest(data.API_KEY, originAddresses, destinationAddresses);
//            System.out.println(example.getRows().get(0).getElements().get(0).getDistance().getValue());
//        } catch (IOException io) {
//            System.out.println(io.fillInStackTrace());
//        }
        List<List<Long>> distanceMatrix = createDistanceMatrix(data);
        for(List<Long> rowList: distanceMatrix) {
            for(Long e: rowList) {
                System.out.print(e + " ");
            }
            System.out.println();
        }

        int vehicleNumber = data.vehicleCapacities.length;
        int depot = 0;

        RoutingIndexManager manager = new RoutingIndexManager(distanceMatrix.size(), vehicleNumber, depot);
        RoutingModel routing = new RoutingModel(manager);

        final int transitCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return distanceMatrix.get(fromNode).get(toNode);
        });
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        routing.addDimension(transitCallbackIndex, 0, 3000000, true, "Distance");
        RoutingDimension distanceDimension = routing.getMutableDimension("Distance");
        distanceDimension.setGlobalSpanCostCoefficient(100);

        final int demandCallbackIndex = routing.registerUnaryTransitCallback((long fromIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            return data.demands[fromNode];
        });
        routing.addDimensionWithVehicleCapacity(demandCallbackIndex, 0,
                data.vehicleCapacities,
                true,
                "Capacity");

        RoutingSearchParameters searchParameters = main
                .defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .build();

        Assignment solution = routing.solveWithParameters(searchParameters);

        printSolution(vehicleNumber, routing, manager, solution);
    }

    static class DataModel {
        public final String[] addresses = {
                "Bayerstraße 81, 80335 München, Germany",
                "Theresienhöhe 16, 80339 München, Germany",
                "Aberlestraße 52, 81371 München, Germany",
                "Friedenheimer Str. 104, 80686 München, Germany",
                "Bergmannstraße 24, 80339 München, Germany",
                "Sonnenstraße 5, 80331 München, Germany",
                "Augustenstraße 107, 80798 München, Germany",
                "Lothringer Str. 10, 81667 München, Germany",
                "Vogelweidepl. 1, 81677 München, Germany",
                "Klenzestraße 17, 80469 München, Germany",
                "Sheraton Munich Arabellapark Hotel, Arabellastraße 5, 81925 München, Germany",
                "Salesforce, Erika-Mann-Straße 31, 80636 München, Germany",
                "Heideckstraße 31, 80637 München, Germany",
                "Balanstraße 73, 81541 München, Germany",
                "Anni-Albers-Straße 11, 80807 München, Germany",
                "Timehouse, Leopoldstraße 204a, 80804 München, Germany",
                "Prinzregentenstraße 56, 80538 München, Germany",
                "Löwenbräukeller - Das Original, Nymphenburger Straße 2 Stiglmaierplatz, 80335 München, Germany",
                "Theresienstraße 27, 80333 München, Germany",
                "Schwindstraße 7, 80798 München, Germany",
                "Platzl Hotel (Superior), Sparkassenstraße 10, 80331 München, Germany",
                "Nockherstraße 30, 81541 München, Germany",
                "Allplan GmbH, Konrad-Zuse-Platz 1, 81829 München, Germany",
                "Vienna House Easy München, Nymphenburger Str. 136, 80636 München, Germany",
                "Gräfstraße 19, 81241 München, Germany",
                "Gneisenaustraße 22, 80992 München, Germany",
                "Schackstraße 3, 80539 München, Germany",
                "NYX Hotel Munich, Hofmannstraße 2, 81379 München, Germany",
                "Sophienstraße 16, 80333 München, Germany",
                "Schinkelstraße 26, 80805 München, Germany",
                "Sixt Car rental, Munich International Airport, Terminalstraße Mitte, 85356 München-Flughafen, Germany",
                "Josephsburgstraße 83, 81673 München, Germany",
                "Waldeslust 4, 81377 München, Germany",
                "Giesecke+Devrient, Prinzregentenstraße 159, 81677 München, Germany",
                "Adlzreiterstraße 23b, 80337 München, Germany",
                "Leopoldstraße 160, 80804 München, Germany",
                "Die Frischeinsel - @H2 Hotel München, Olof-Palme-Straße 12, 81829 München, Germany",
                "Leopoldstraße 170, 80805 München, Germany",
                "Mauerkircherstraße 105, 81925 München, Germany",
                "Käfer-Schänke, Prinzregentenstraße 73/1. Etage, 81675 München, Germany"
        };
        public long[] demands = new long[addresses.length];
        public long[] vehicleCapacities = new long[addresses.length/5];
        public final String API_KEY = "";
    }

    static void printSolution(int vehicleNumber, RoutingModel routing, RoutingIndexManager manager, Assignment solution) {
        long maxRouteDistance = 0;
        for (int i = 0; i < vehicleNumber; ++i) {
            long index = routing.start(i);
            logger.info("Route for Vehicle " + i + ":");
            long routeDistance = 0;
            StringBuilder route = new StringBuilder();
            while (!routing.isEnd(index)) {
                route.append(manager.indexToNode(index)).append(" -> ");
                long previousIndex = index;
                index = solution.value(routing.nextVar(index));
                routeDistance += routing.getArcCostForVehicle(previousIndex, index, i);
            }
            logger.info(route.toString() + manager.indexToNode(index));
            logger.info("Distance of the route: " + routeDistance + "m");
            maxRouteDistance = Math.max(routeDistance, maxRouteDistance);
        }
        logger.info("Maximum of the route distances: " + maxRouteDistance + "m");
    }

    public static List<List<Long>> createDistanceMatrix(DataModel data) throws IOException {
        int maxElements = 100;
        int numAddresses = data.addresses.length;
        int maxRows = maxElements/numAddresses;
        int q = numAddresses/maxRows;
        int r = numAddresses % maxRows;

        String[] destinationAddresses = data.addresses;
        List<List<Long>> distanceMatrix = new ArrayList<>();
        for(int i = 0; i < q; i++) {
            String[] originAddresses = Arrays.copyOfRange(data.addresses, i*maxRows, (i+1)*maxRows);
            Example example = sendRequest(data.API_KEY, originAddresses, destinationAddresses);
            distanceMatrix.addAll(buildDistanceMatrix(example));
        }

        if (r > 0) {
            String[] originAddresses = Arrays.copyOfRange(data.addresses, q*maxRows, q*maxRows+r);
            Example example = sendRequest(data.API_KEY, originAddresses, destinationAddresses);
            distanceMatrix.addAll(buildDistanceMatrix(example));
        }
        return distanceMatrix;
    }

    public static List<List<Long>> createDistanceMatrixH(DataModel data) throws IOException {
        List<List<Long>> distanceMatrix = new ArrayList<>();
        return distanceMatrix;
    }

    public static Example sendRequest(String apiKey, String[] originAddresses, String[] destinationAddresses) throws IOException {
        String request = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial";
        String originAddressString = String.join("|", originAddresses).replace(" ", "+");
        String destinationAddressString = String.join("|", destinationAddresses).replace(" ", "+");
        request = request + "&origins=" + originAddressString + "&destinations=" + destinationAddressString + "&key=" + apiKey;

        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        InputStream in = connection.getInputStream();
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        return new Gson().fromJson(reader, Example.class);
    }

    public static List<List<Long>> buildDistanceMatrix(Example example) {
        List<List<Long>> distanceMatrix = new ArrayList<>();
        for(Row row: example.getRows()) {
            List<Long> rowList = new ArrayList<>();
            for(Element element: row.getElements()) {
//                if (element.getStatus().equals("OK")) {
                    rowList.add(Long.valueOf(element.getDistance().getValue()));
//                }
            }
            distanceMatrix.add(rowList);
        }
        return distanceMatrix;
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c * 1000;
    }
}
