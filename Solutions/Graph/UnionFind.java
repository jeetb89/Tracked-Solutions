/**
 * Disjoint Set Union (Union Find) with path compression and union by size.
 * Vertices are numbered from 0 to n - 1.
 */
public class UnionFind {
    private final int[] parent;
    private final int[] size;
    private int componentCount;

    public UnionFind(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Number of vertices cannot be negative");
        }

        parent = new int[n];
        size = new int[n];
        componentCount = n;

        for (int vertex = 0; vertex < n; vertex++) {
            parent[vertex] = vertex;
            size[vertex] = 1;
        }
    }

    /** Returns the representative of the set containing {@code vertex}. */
    public int find(int vertex) {
        checkVertex(vertex);
        if (parent[vertex] != vertex) {
            parent[vertex] = find(parent[vertex]);
        }
        return parent[vertex];
    }

    /**
     * Merges the sets containing {@code first} and {@code second}.
     * @return true if the vertices were in different sets and a merge occurred.
     */
    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);

        if (firstRoot == secondRoot) {
            return false;
        }

        if (size[firstRoot] < size[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }

        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        componentCount--;
        return true;
    }

    /** Returns whether both vertices belong to the same set. */
    public boolean connected(int first, int second) {
        return find(first) == find(second);
    }

    /** Returns the number of disjoint sets. */
    public int componentCount() {
        return componentCount;
    }

    /** Returns the size of the set containing {@code vertex}. */
    public int componentSize(int vertex) {
        return size[find(vertex)];
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IndexOutOfBoundsException("Vertex out of range: " + vertex);
        }
    }

    public static void main(String[] args) {
        UnionFind unionFind = new UnionFind(6);

        unionFind.union(0, 1);
        unionFind.union(1, 2);
        unionFind.union(3, 4);

        System.out.println("0 connected to 2: " + unionFind.connected(0, 2)); // true
        System.out.println("0 connected to 4: " + unionFind.connected(0, 4)); // false
        System.out.println("Components: " + unionFind.componentCount()); // 3
        System.out.println("Size of component containing 1: " + unionFind.componentSize(1)); // 3
    }
}
