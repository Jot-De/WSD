package utils;

import java.util.*;

import static utils.agentUtils.possibleParkingLocations;

public class MapState {
    public static int[][] grid = new int[21][41];

    public static void fillMapWithRoads() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (i % 5 == 0 || j % 5 == 0) {
                    grid[i][j] = 1;
                } else {
                    grid[i][j] = 0;
                }
            }
        }

        possibleParkingLocations.forEach((key, value) -> {
            System.out.println("Parking location" + Arrays.toString(key));
            grid[key[0]][key[1]] = 1;
        });
    }

    public static ArrayList<int[]> calculatePathBFS(int[] source, int[] target) {

        fillMapWithRoads();

        ArrayList<int[]> visitedTiles = new ArrayList<>();
        Tile sourceTile = new Tile(source[0], source[1], visitedTiles);
        Queue<Tile> queue = new LinkedList<Tile>();

        // Helper matrix to determine whether we visited a tile.
        int [][] visited = new int[grid.length][];
        for(int i = 0; i < grid.length; i++)
            visited[i] = grid[i].clone();

        queue.add(sourceTile);

        while(!queue.isEmpty()) {
            Tile currentTile = queue.poll();

            int i = currentTile.i;
            int j = currentTile.j;

            if (grid[i][j] == 0) {
                continue;
            }
            if (visited[i][j] == 0) {
                continue;
            } else {
                visited[i][j] = 0;
            }

            if(i == target[0] && j == target[1]) {
                return currentTile.visitedTiles;
            }
            else {
                visited[i][j] = 0;

                List<Tile> unvisitedNeighbours = getUnvisitedNeighbours(currentTile, grid, visited);
                queue.addAll(unvisitedNeighbours);
            }
        }
        ArrayList<int[]> emptyPath = new ArrayList<>();
        return emptyPath;
    }

    private static List<Tile> getUnvisitedNeighbours(Tile current, int[][] matrix, int[][] visited) {

        List<Tile> unvisitedNeighbours = new LinkedList<Tile>();

        if(current.i-1 >= 0 && visited[current.i-1][current.j] != 0) {
            int[] pos = {current.i-1, current.j};
            ArrayList<int[]> currentArrayList = new ArrayList<int[]>(current.visitedTiles);
            currentArrayList.add(pos);
            unvisitedNeighbours.add(new Tile(current.i-1, current.j, currentArrayList));
        }
        if(current.i+1 < matrix.length && visited[current.i+1][current.j] != 0) {
            int[] pos = {current.i+1, current.j};
            ArrayList<int[]> currentArrayList = new ArrayList<int[]>(current.visitedTiles);
            currentArrayList.add(pos);
            unvisitedNeighbours.add(new Tile(current.i+1, current.j, currentArrayList));
        }
        if(current.j-1 >= 0 && visited[current.i][current.j-1] != 0) {
            int[] pos = {current.i, current.j-1};
            ArrayList<int[]> currentArrayList = new ArrayList<int[]>(current.visitedTiles);
            currentArrayList.add(pos);
            unvisitedNeighbours.add(new Tile(current.i, current.j-1, currentArrayList));
        }
        if(current.j+1 < matrix[0].length && visited[current.i][current.j+1] != 0) {
            int[] pos = {current.i, current.j+1};
            ArrayList<int[]> currentArrayList = new ArrayList<int[]>(current.visitedTiles);
            currentArrayList.add(pos);
            unvisitedNeighbours.add(new Tile(current.i, current.j+1, currentArrayList));
        }
        return unvisitedNeighbours;
    }

    private static class Tile {
        int i;
        int j;
        ArrayList<int[]> visitedTiles;

        Tile(int i, int j, ArrayList<int[]> visitedTiles) {
            this.i = i;
            this.j = j;
            this.visitedTiles = visitedTiles;
        }
    }
}