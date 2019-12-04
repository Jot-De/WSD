package utils;

public class agentUtils {
    public static int[] getRandomLocation() {
        int max = 10;
        int min = 1;
        int range = max - min + 1;

        int[] location = {(int) (Math.random() * range) + min, (int) (Math.random() * range) + min};
        return location;
    }
}
