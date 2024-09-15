package org.project;

import java.util.*;
import java.util.stream.Collectors;

public class ShortestPathFinder {
    private int maxLevel;
    private Map<Integer, Set<int[]>> levelSeparators;
    private DistancePrecomputation distancePrecomputation;
    private GridGraph gridGraph;

    public ShortestPathFinder(DistancePrecomputation distancePrecomputation, int maxLevel, GridGraph gridGraph) {
        this.distancePrecomputation = distancePrecomputation;
        this.maxLevel = maxLevel;
        this.levelSeparators = distancePrecomputation.getLevelSeparators();
        this.gridGraph = gridGraph;
    }

    public int findShortestPath(int[] source, int[] target) {
        System.out.println("Finding shortest path from " + Arrays.toString(source) + " to " + Arrays.toString(target));

        int hierarchicalDistance = findShortestPathHierarchical(source, target);
        System.out.println("Hierarchical approach distance: " + hierarchicalDistance);

        return hierarchicalDistance;
    }

    private int findShortestPathHierarchical(int[] source, int[] target) {
        List<int[]> path = new ArrayList<>();
        int distance = findShortestPathRecursive(source, target, maxLevel, path);
        System.out.println("Hierarchical approach path: " + pathToString(path));
        return distance;
    }

    private int findShortestPathRecursive(int[] source, int[] target, int level, List<int[]> path) {
        if (level == 0) {
            return dijkstra(source, target, path);  // Dijkstra can still be used at level 0 if needed
        }

        Set<int[]> separators = levelSeparators.get(level);
        if (separators == null || separators.isEmpty()) {
            return findShortestPathRecursive(source, target, level - 1, path);
        }

        int minDistance = Integer.MAX_VALUE;
        List<int[]> bestPath = new ArrayList<>();  // Store best path found

        for (int[] separator : separators) {
            List<int[]> tempPath1 = new ArrayList<>();
            List<int[]> tempPath2 = new ArrayList<>();

            int distanceViaSeparator = getPrecomputedOrCalculateDistance(source, separator, level, tempPath1)
                    + getPrecomputedOrCalculateDistance(separator, target, level, tempPath2);

            if (distanceViaSeparator < minDistance) {
                minDistance = distanceViaSeparator;
                bestPath.clear();  // Clear bestPath and add the new best path
                bestPath.addAll(tempPath1);
                bestPath.addAll(tempPath2.subList(1, tempPath2.size()));  // Avoid adding separator twice
            }
        }

        // Check if the lower level path is shorter
        List<int[]> lowerLevelPath = new ArrayList<>();
        int lowerLevelDistance = findShortestPathRecursive(source, target, level - 1, lowerLevelPath);

        if (lowerLevelDistance < minDistance) {
            path.addAll(lowerLevelPath);  // Add lower level path if shorter
            return lowerLevelDistance;
        } else {
            path.addAll(bestPath);  // Otherwise, add the best hierarchical path
            return minDistance;
        }
    }



    private int dijkstra(int[] source, int[] target, List<int[]> path) {
        // Minimal Dijkstra's implementation used only if level is 0
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));
        Map<String, Integer> distances = new HashMap<>();
        Map<String, int[]> previousNodes = new HashMap<>();

        pq.offer(new Node(source, 0));
        distances.put(Arrays.toString(source), 0);

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int[] currentPoint = current.point;

            if (Arrays.equals(currentPoint, target)) {
                reconstructPath(source, target, previousNodes, path);
                return current.distance;
            }

            for (int[] neighbor : gridGraph.getNeighbors(currentPoint[0], currentPoint[1])) {
                String neighborKey = Arrays.toString(neighbor);
                int edgeWeight = gridGraph.getEdgeWeight(currentPoint[0], currentPoint[1], neighbor[0], neighbor[1]);
                int newDistance = current.distance + edgeWeight;

                if (newDistance < distances.getOrDefault(neighborKey, Integer.MAX_VALUE)) {
                    distances.put(neighborKey, newDistance);
                    previousNodes.put(neighborKey, currentPoint);
                    pq.offer(new Node(neighbor, newDistance));
                }
            }
        }

        return Integer.MAX_VALUE;
    }

    private void reconstructPath(int[] source, int[] target, Map<String, int[]> previousNodes, List<int[]> path) {
        int[] current = target;
        while (current != null) {
            path.add(current);
            current = previousNodes.get(Arrays.toString(current));
        }
        Collections.reverse(path);
    }

    private int getPrecomputedOrCalculateDistance(int[] vertex1, int[] vertex2, int level, List<int[]> path) {
        Integer precomputedDistance = distancePrecomputation.getPrecomputedDistance(vertex1, vertex2, level);
        if (precomputedDistance != null) {
            path.add(vertex1);
            path.add(vertex2);
            return precomputedDistance;
        }
        return findShortestPathRecursive(vertex1, vertex2, level - 1, path);
    }

    private String pathToString(List<int[]> path) {
        return path.stream()
                .map(Arrays::toString)
                .collect(Collectors.joining(" -> "));
    }

    private static class Node {
        int[] point;
        int distance;

        Node(int[] point, int distance) {
            this.point = point;
            this.distance = distance;
        }
    }
}
