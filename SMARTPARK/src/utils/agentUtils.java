package utils;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class agentUtils {
    // Cache of all created locations.
    // FIXME: Change ArrayList to HashMap to improve time performance.
    private static ArrayList<int[]> createdLocations = new ArrayList<>();
    public static HashMap<int[],Boolean> possibleParkingLocations = new HashMap<>() {{
       put(new int[] {1, 1}, true);
       put(new int[] {19, 39}, true);
        put(new int[] {1, 39}, true);
        put(new int[] {6, 1}, true);
        put(new int[] {11, 1}, true);
        put(new int[] {16, 1}, true);
        put(new int[] {6, 6}, true);
        put(new int[] {11, 6}, true);
        put(new int[] {16, 6}, true);
        put(new int[] {6, 21}, true);
        put(new int[] {11, 21}, true);
        put(new int[] {16, 21}, true);
    }};

    /**
     * Initialize parking location with unique value.
     *
     * @return location of the parking.
     */
    public static int[] initializeParkingLocation() {
        final int[] location = new int[] {0, 0};

        for(Map.Entry<int[], Boolean> entry : possibleParkingLocations.entrySet()) {
            int[] key = entry.getKey();
            boolean value = entry.getValue();

            if (value) {
                possibleParkingLocations.put(key, false);
                return key;
            }
        }
        return location;
    }

    public static int[] initializeCarLocation() {
        int max_x = 20;
        int max_y = 40;
        int min = 0;
        int range_x = max_x - min + 1;
        int range_y = max_y - min + 1;
        int x = -1;
        int y = -1;

        while (true) {
            while (x % 5 != 0 && y % 5 != 0) {
                x = (int) (Math.random() * range_x) + min;
                y = (int) (Math.random() * range_y) + min;
            }

            int[] location = {x, y};

            if (createdLocations.size() == 0) {
                createdLocations.add(location);
                return location;
            }

            for (int i = 0; i < createdLocations.size(); i++) {
                int createdLocation_x = createdLocations.get(i)[0];
                int createdLocation_y = createdLocations.get(i)[1];

                if (createdLocation_x != x && createdLocation_y != y) {
                    createdLocations.add(location);
                    return location;
                }
            }
            x = -1;
            y = -1;
        }
    }

    /**
     * Parse string to 2D array.
     */
    public static int[] parseLocation(String locationString) {
        String[] coordinates = locationString.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
        int[] location = new int[coordinates.length];
        //Create an Array.
        for (int i = 0; i < coordinates.length; i++) {
            try {
                location[i] = Integer.parseInt(coordinates[i]);
            } catch (NumberFormatException nfe) {
                System.out.println("Error occurred");
            }
        }
        return location;
    }

    /**
     * Send data to the middleware server that holds information about agents.
     * @param name
     * @param type
     * @param location
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void sendData(String name, String type, String location) throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:3000/agent");

        String json = "{\"name\":\"" + name + "\",";
        json += "\"type\":\"" + type + "\",";
        json += "\"location\":\"" + location + "\"}";

        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        CloseableHttpResponse response = client.execute(httpPost);
        client.close();
    }

    public static void removeAgentFromDatabase(String name) throws ClientProtocolException, IOException {
        System.out.println("trying to delete agent");
        CloseableHttpClient client = HttpClients.createDefault();
        String escapedName = name.replaceAll("/", "%2F");
        String url = "http://localhost:3000/agent/" + escapedName;
        HttpDelete httpDelete = new HttpDelete(url);

        CloseableHttpResponse response = client.execute(httpDelete);
        client.close();
    }

    /*
     * Remove parking location on take down.
     */
    public static void freeParkingLocation(int[] location) {
        possibleParkingLocations.put(location, true);
    }

    /*
     * Remove car location on take down.
     */
    public static void freeCarLocation(int[] location) {
        for (int i = 0; i < createdLocations.size(); i++) {
            if (location[0] == createdLocations.get(i)[0] && location[1] == createdLocations.get(i)[1]) {
                createdLocations.remove(i);
            }
        }
    }

    /**
     * Calculate distance from source to target
     * @param sourceLocation
     * @param targetLocation
     * @return
     */
    public static double calculateDistance(int[] sourceLocation, int[] targetLocation) {
        //Calculate the distance between car and parking on 2D plane.
        int x1 = sourceLocation[0];
        int y1 = sourceLocation[1];
        int x2 = targetLocation[0];
        int y2 = targetLocation[1];
        double distance = Math.hypot(x1 - x2, y1 - y2);

        return distance;
    }
}
