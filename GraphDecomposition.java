package org.project;

import java.util.*;

public class GraphDecomposition {
    private GridGraph gridGraph;
    private GridPanel gridPanel;
    private DistancePrecomputation distancePrecomputation;
    private int maxLevel;

    public GraphDecomposition(GridGraph gridGraph, GridPanel gridPanel) {
        this.gridGraph = gridGraph;
        this.gridPanel = gridPanel;
        this.maxLevel = calculateMaxLevel();
        this.distancePrecomputation = new DistancePrecomputation(gridGraph);
        gridGraph.printGrid();
    }

    private int calculateMaxLevel() {
        int n = Math.max(gridGraph.getM(), gridGraph.getN());
        return (int) (Math.log(n) / Math.log(2));
    }

    public void decomposeEntireGrid() {
        Set<int[]> entireGraph = getSubgraphVertices(0, 0, gridGraph.getM() - 1, gridGraph.getN() - 1);
        System.out.println("Starting decomposition with max level: " + maxLevel);
        decomposeGraph(entireGraph, 0);
        //distancePrecomputation.checkStoredDistances();
        ShortestPathFinder shortestPathFinder = new ShortestPathFinder(distancePrecomputation, maxLevel,gridGraph);

        // Finding shortest path from (0, 0) to (7, 7)
        int[] vertex1 = new int[]{0, 0};
        int[] vertex2 = new int[]{7, 7};

        int result = shortestPathFinder.findShortestPath(vertex1, vertex2);
        System.out.println("Shortest distance from (0, 0) to (7, 7): " + result);

        int dijkstraDistance = distancePrecomputation.dijkstra(vertex1, vertex2);
        System.out.println("Dijkstra's distance from (0,0) to (7,7): " + dijkstraDistance);

    }

    private void decomposeGraph(Set<int[]> subgraph, int level) {
        System.out.println("Decomposing at level " + level + " with subgraph size " + subgraph.size());

        if (subgraph.size() <= 2 || level >= maxLevel) {
            System.out.println("Reached base case at level " + level);
            return;
        }

        // Find the separator for the current subgraph
        List<int[]> separator = findSeparator(subgraph);
        Set<int[]> part1 = new HashSet<>();
        Set<int[]> part2 = new HashSet<>();

        // Divide the subgraph based on the separator
        divideSubgraph(subgraph, separator, part1, part2);

        // Precompute distances only for representative vertices in the subgraph
        distancePrecomputation.precomputeDistances(level, separator, subgraph);

//        for (int[] sep : separator) {
//            gridPanel.markSeparator(sep[0], sep[1]);
//        }

        // Recursively decompose the subgraph further
        decomposeGraph(part1, level + 1);
        decomposeGraph(part2, level + 1);
    }

    // Method to find a separator for the current subgraph (e.g., vertical or horizontal split)
    private List<int[]> findSeparator(Set<int[]> subgraph) {
        int[] bounds = findSubgraphBounds(subgraph);
        int x1 = bounds[0], y1 = bounds[1], x2 = bounds[2], y2 = bounds[3];

        List<int[]> separator = new ArrayList<>();
        if (x2 - x1 > y2 - y1) { // Vertical separator
            int midX = (x1 + x2) / 2;
            for (int y = y1; y <= y2; y++) {
                separator.add(new int[]{midX, y});
            }
        } else { // Horizontal separator
            int midY = (y1 + y2) / 2;
            for (int x = x1; x <= x2; x++) {
                separator.add(new int[]{x, midY});
            }
        }
        return separator;
    }

    // Method to divide the subgraph into two parts based on the separator
    private void divideSubgraph(Set<int[]> subgraph, List<int[]> separator, Set<int[]> part1, Set<int[]> part2) {
        for (int[] vertex : subgraph) {
            if (isLeftOrAboveSeparator(vertex, separator)) {
                part1.add(vertex);
            } else {
                part2.add(vertex);
            }
        }
    }

    // Method to find the bounds of the current subgraph
    private int[] findSubgraphBounds(Set<int[]> subgraph) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] vertex : subgraph) {
            minX = Math.min(minX, vertex[0]);
            minY = Math.min(minY, vertex[1]);
            maxX = Math.max(maxX, vertex[0]);
            maxY = Math.max(maxY, vertex[1]);
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    // Check if a vertex is on the left or above the separator
    private boolean isLeftOrAboveSeparator(int[] vertex, List<int[]> separator) {
        int[] sepVertex = separator.get(0);
        if (separator.get(0)[0] == separator.get(1)[0]) { // Vertical separator
            return vertex[0] < sepVertex[0];
        } else { // Horizontal separator
            return vertex[1] < sepVertex[1];
        }
    }

    // Get all vertices in a subgraph between the given bounds
    private Set<int[]> getSubgraphVertices(int x1, int y1, int x2, int y2) {
        Set<int[]> vertices = new HashSet<>();
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                vertices.add(new int[]{x, y});
            }
        }
        return vertices;
    }

}
