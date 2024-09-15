# Distance in Grid Graphs Project

## Objective

This project focuses on optimizing the **shortest path computation** in large grid graphs by reducing the number of precomputations and improving query time complexity. In large-scale grid graphs, finding the shortest path on the fly can be computationally expensive. To tackle this issue, the project aims to strike a balance between precomputing distances and minimizing time complexity for queries.

## Current Progress

The algorithm has made strides in tackling **space complexity** by focusing on key vertices, particularly:
- **Corner vertices**.
- Vertices with the **longest distance from the separators** (separators are lines dividing the graph into subgraphs, which help precompute distances).

However, the current solution still faces challenges, with **query complexity** reaching its worst-case scenario even for smaller graphs.

## Future Improvements

To address these limitations, the following improvements can be made:
- **Adaptive Level Selection**: Implement adaptive level selection based on the distance between the source and destination to avoid unnecessary checks at inappropriate levels.
- **Efficient LCA Query Structure**: Develop a more efficient Lowest Common Ancestor (LCA) query structure during the graph decomposition phase to optimize the process further.

## References

- Charalampopoulos, P., Gawrychowski, P., Mozes, S., & Weimann, O. (2018). *Almost Optimal Distance Oracles for Planar Graphs*. Proceedings of the 2018 Annual ACM-SIAM Symposium on Discrete Algorithms.
  
- Klein, P., Mozes, S., & Sommer, S. (2005). *Multiple-Source Shortest Paths in Planar Graphs*. SIAM Journal on Computing, 33(3), 469-490.
  
- Thorup, M., & Zwick, U. (2001). *Approximate Distance Oracles*. Journal of the ACM, 52(1), 1-24.

