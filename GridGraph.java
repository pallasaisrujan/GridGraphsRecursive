package org.project;

import java.util.ArrayList;
import java.util.List;

public class GridGraph {
    private int m, n;
    private int[][] grid;

    public GridGraph(int m, int n) {
        this.m = m;
        this.n = n;
        this.grid = new int[m][n];
        initializeGraph();
    }

    // Initialize the grid with default edge weights (e.g., all weights set to 1)
    private void initializeGraph() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                // Generate random weight between 0 and 2 (exclusive) but greater than 0
                grid[i][j] = 1+((int) (0.1 + (Math.random() * 1.9)));  // Math.random() generates a value between 0.0 and 1.0
            }
        }
    }


    // Accessor for dimensions
    public int getM() {
        return m;
    }

    public int getN() {
        return n;
    }

    // Simulating edges in a grid for testing purposes
    public List<int[]> getNeighbors(int x, int y) {
        List<int[]> neighbors = new ArrayList<>();
        if (x > 0) neighbors.add(new int[]{x - 1, y});
        if (x < m - 1) neighbors.add(new int[]{x + 1, y});
        if (y > 0) neighbors.add(new int[]{x, y - 1});
        if (y < n - 1) neighbors.add(new int[]{x, y + 1});
        return neighbors;
    }

    // Method to get the weight of the edge between two vertices
    public int getEdgeWeight(int x1, int y1, int x2, int y2) {
        if (Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1) {
            return (int) grid[x1][y1];  // Cast to int if grid stores decimal weights
        } else {
            // If the vertices are not adjacent, return a very high number to indicate no direct edge
            return Integer.MAX_VALUE;  // Alternatively, you can throw an exception if needed
        }
    }


    // Printing the grid (optional for debugging)
    public void printGrid() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
    }
}
