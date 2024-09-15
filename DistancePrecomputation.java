package org.project;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DistancePrecomputation {
    private GridGraph gridGraph;
    private static final String URL = "jdbc:postgresql://localhost:5432/gridgraph";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234";
    private Connection connection;
    private Map<Integer, Set<int[]>> levelSeparators;
    private Map<String, Integer> distanceCache;

    public DistancePrecomputation(GridGraph gridGraph) {
        this.gridGraph = gridGraph;
        this.levelSeparators = new HashMap<>();
        this.distanceCache = new ConcurrentHashMap<>();
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to PostgreSQL database.");
            createTableIfNotExists();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    private void createTableIfNotExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS precomputed_distances (" +
                "separator_vertex VARCHAR(50), " +
                "subgraph_vertex VARCHAR(50), " +
                "distance INT, " +
                "level INT, " +
                "PRIMARY KEY (separator_vertex, subgraph_vertex, level))";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }

    public Map<Integer, Set<int[]>> getLevelSeparators() {
        return levelSeparators;
    }

    public void precomputeDistances(int level, List<int[]> separator, Set<int[]> subgraph) {
        levelSeparators.computeIfAbsent(level, k -> new HashSet<>()).addAll(separator);

        int threshold = calculatePrecomputationThreshold(level, subgraph);

        if (level <= threshold && isPrecomputationBeneficial(level, subgraph)) {
            Set<int[]> importantVertices = getImportantVertices(subgraph);
            for (int[] s : separator) {
                for (int[] v : importantVertices) {
                    int distance = dijkstra(s, v);
                    storeDistance(s, v, distance, level);
                }
            }
            System.out.println("Precomputed distances for level " + level + " with " + importantVertices.size() + " important vertices");
        } else {
            System.out.println("Skipped precomputation for level " + level + " (threshold: " + threshold + ")");
        }
    }

    private int calculatePrecomputationThreshold(int level, Set<int[]> subgraph) {
        int graphSize = gridGraph.getM() * gridGraph.getN();
        int subgraphSize = subgraph.size();

        // Base threshold based on graph size
        int baseThreshold = (int) Math.log(graphSize);

        // Adjust threshold based on level and subgraph size
        int adjustedThreshold = baseThreshold - level + (int) Math.log(subgraphSize);

        // Ensure the threshold is within reasonable bounds
        return Math.max(0, Math.min(adjustedThreshold, 5));
    }

    private boolean isPrecomputationBeneficial(int level, Set<int[]> subgraph) {
        int subgraphSize = subgraph.size();
        int separatorSize = levelSeparators.get(level).size();

        // Estimate the cost of precomputation
        int precomputationCost = subgraphSize * separatorSize;

        // Estimate the benefit of precomputation (e.g., number of potential queries saved)
        int potentialQueries = subgraphSize * subgraphSize;

        // Precompute if the benefit outweighs the cost
        return potentialQueries > precomputationCost * 2; // Adjust this factor as needed
    }

    private Set<int[]> getImportantVertices(Set<int[]> subgraph) {
        Set<int[]> importantVertices = new HashSet<>();
        int[] bounds = findSubgraphBounds(subgraph);
        importantVertices.add(new int[]{bounds[0], bounds[1]}); // Bottom-left corner
        importantVertices.add(new int[]{bounds[2], bounds[1]}); // Bottom-right corner
        importantVertices.add(new int[]{bounds[0], bounds[3]}); // Top-left corner
        importantVertices.add(new int[]{bounds[2], bounds[3]}); // Top-right corner

        // Add some random vertices from the subgraph
        List<int[]> subgraphList = new ArrayList<>(subgraph);
        Random random = new Random();
        int additionalVertices = Math.min(5, subgraph.size() / 10); // Add up to 5 additional vertices or 10% of subgraph size
        for (int i = 0; i < additionalVertices; i++) {
            importantVertices.add(subgraphList.get(random.nextInt(subgraphList.size())));
        }

        return importantVertices;
    }

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

    public int dijkstra(int[] source, int[] target) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));
        Map<String, Integer> distances = new HashMap<>();

        pq.offer(new Node(source, 0));
        distances.put(Arrays.toString(source), 0);

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int[] currentPoint = current.point;

            if (Arrays.equals(currentPoint, target)) {
                return current.distance;
            }

            for (int[] neighbor : getNeighbors(currentPoint)) {
                int edgeWeight = gridGraph.getEdgeWeight(currentPoint[0], currentPoint[1], neighbor[0], neighbor[1]);
                int newDistance = current.distance + edgeWeight;
                String neighborKey = Arrays.toString(neighbor);

                if (newDistance < distances.getOrDefault(neighborKey, Integer.MAX_VALUE)) {
                    distances.put(neighborKey, newDistance);
                    pq.offer(new Node(neighbor, newDistance));
                }
            }
        }

        return Integer.MAX_VALUE; // No path found
    }

    private List<int[]> getNeighbors(int[] point) {
        return gridGraph.getNeighbors(point[0], point[1]);
    }

    private void storeDistance(int[] separator, int[] vertex, int distance, int level) {
        String separatorKey = Arrays.toString(separator);
        String vertexKey = Arrays.toString(vertex);
        String cacheKey = separatorKey + "-" + vertexKey + "-" + level;
        distanceCache.put(cacheKey, distance);

        String insertSQL = "INSERT INTO precomputed_distances (separator_vertex, subgraph_vertex, distance, level) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT (separator_vertex, subgraph_vertex, level) DO UPDATE SET distance = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setString(1, separatorKey);
            preparedStatement.setString(2, vertexKey);
            preparedStatement.setInt(3, distance);
            preparedStatement.setInt(4, level);
            preparedStatement.setInt(5, distance);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getPrecomputedDistance(int[] vertex1, int[] vertex2, int level) {
        String key1 = Arrays.toString(vertex1) + "-" + Arrays.toString(vertex2) + "-" + level;
        String key2 = Arrays.toString(vertex2) + "-" + Arrays.toString(vertex1) + "-" + level;

        Integer distance = distanceCache.get(key1);
        if (distance != null) {
            return distance;
        }

        distance = distanceCache.get(key2);
        if (distance != null) {
            return distance;
        }

        // If not in cache, try to fetch from database
        String query = "SELECT distance FROM precomputed_distances " +
                "WHERE (separator_vertex = ? AND subgraph_vertex = ?) OR (separator_vertex = ? AND subgraph_vertex = ?) " +
                "AND level = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            pstmt.setString(1, Arrays.toString(vertex1));
            pstmt.setString(2, Arrays.toString(vertex2));
            pstmt.setString(3, Arrays.toString(vertex2));
            pstmt.setString(4, Arrays.toString(vertex1));
            pstmt.setInt(5, level);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    distance = rs.getInt("distance");
                    // Store in cache for future use
                    distanceCache.put(key1, distance);
                    return distance;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Return null if no precomputed distance is found
    }

    public void checkStoredDistances() {
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM precomputed_distances");
            while (rs.next()) {
                String separator = rs.getString("separator_vertex");
                String subgraph = rs.getString("subgraph_vertex");
                int distance = rs.getInt("distance");
                int level = rs.getInt("level");
                System.out.println("Separator: " + separator + ", Subgraph: " + subgraph + ", Distance: " + distance + ", Level: " + level);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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