package utils;

import java.util.ArrayList;

public class agentUtils {
    // Cache of all created locations.
    // FIXME: Change ArrayList to HashMap to improve time performance.
    private static ArrayList<int[]> createdLocations = new ArrayList<>();

    /**
     * Initialize parking location with unique value.
     *
     * @return location of the parking.
     */
    public static int[] initializeParkingLocation() {
        int max = 10;
        int min = 0;
        int range = max - min + 1;

        while (true) {
            int x = (int) (Math.random() * range) + min;
            int y = (int) (Math.random() * range) + min;
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
     * Remove parking location on take down.
     */
    public static void freeParkingLocation(int[] location) {
        for (int i = 0; i < createdLocations.size(); i++) {
            if (location[0] == createdLocations.get(i)[0] && location[1] == createdLocations.get(i)[1]) {
                createdLocations.remove(i);
            }
        }
    }
}
