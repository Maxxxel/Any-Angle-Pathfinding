package main.graphgeneration;

import java.util.Random;

import grid.GridAndGoals;
import grid.GridGraph;

public class AutomataGenerator {

    public static GridAndGoals generateUnseeded(int sizeX, int sizeY, int unblockedRatio, int iterations, int cutoffOffset, int sx, int sy, int ex, int ey) {
        GridGraph gridGraph = generate(false, 0, sizeX, sizeY, unblockedRatio, iterations, cutoffOffset);
        return new GridAndGoals(gridGraph, sx, sy, ex, ey);
    }

    public static GridAndGoals generateSeeded(long seed, int sizeX, int sizeY, int unblockedRatio, int iterations, int cutoffOffset, int sx, int sy, int ex, int ey) {
        GridGraph gridGraph = generate(true, seed, sizeX, sizeY, unblockedRatio, iterations, cutoffOffset);
        return new GridAndGoals(gridGraph, sx, sy, ex, ey);
    }
    
    public static GridGraph generateSeededGraphOnly(long seed, int sizeX, int sizeY, int unblockedRatio, int iterations, int cutoffOffset) {
        GridGraph gridGraph = generate(true, seed, sizeX, sizeY, unblockedRatio, iterations, cutoffOffset);
        return gridGraph;
    }

    private static GridGraph generate(boolean seededRandom, long seed, int sizeX, int sizeY, int unblockedRatio, int iterations, int cutoffOffset) {
        GridGraph gridGraph = new GridGraph(sizeX, sizeY);

        Random rand = new Random();
        if (!seededRandom) {
            seed = rand.nextInt();
            System.out.println("Starting random with random seed = " + seed);
        } else {
            System.out.println("Starting random with predefined seed = " + seed);
        }
        rand = new Random(seed);
        
        generateRandomMap(rand, gridGraph, unblockedRatio, iterations, cutoffOffset);
        
        return gridGraph;
    }
    

    /**
     * Generates a truly random map for the gridGraph.
     * No longer used as this does not generate very good or realistic grids.
     */
    private static void generateRandomMap(Random rand, GridGraph gridGraph, int frequency, int iterations, int cutoffOffset) {
        int sizeX = gridGraph.sizeX;
        int sizeY = gridGraph.sizeY;
        int resolution = Math.max((sizeX+sizeY)/150, 1);
        int cutoff = (int)(0.8f*resolution*resolution + 1.75f*resolution + 0.8f) + cutoffOffset;

        System.out.println("Resolution " + resolution + ", Cutoff " + cutoff);
        
        boolean[][] grid = new boolean[sizeY][];
        // Count: used for DP computation of number of blocked neighbours.
        //  Note: count includes the current tile as well. We subtract it when we compare with cutoff.
        int[][] count = new int[sizeY][];
        for (int y=0;y<sizeY;++y) {
            grid[y] = new boolean[sizeX];
            count[y] = new int[sizeX];
            for (int x=0;x<sizeX;++x) {
                grid[y][x] = rand.nextInt()%frequency == 0;
            }
        }
        
        for (int itr=0;itr<iterations;++itr) {
            /*
             * Note: for brevity, the following code:
             * nBlocked += (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
             * 
             * Is a shortened version of:
             * if (px < 0 || py < 0 || px >= sizeX || py >= sizeY) {
             *     nBlocked++;
             * } else {
             *     nBlocked += grid[py][px] ? 1 : 0;
             * }
             */
            
            { // Base case: y = 0
                int y = 0;
                { // Base case: x = 0
                    int x = 0;
                    int nBlocked = 0;
                    for (int i=-resolution;i<=resolution;++i) {
                        for (int j=-resolution;j<=resolution;++j) {
                            int px = x + i;
                            int py = y + j;
                            nBlocked += (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                        }
                    }
                    
                    count[y][x] = nBlocked;
                }

                // y = 0, x > 0
                for (int x=1;x<sizeX;++x) {
                    int nBlocked = count[y][x-1];

                    { // subtract for (x-1-r,?)
                        int px = x - resolution - 1;
                        for (int j=-resolution;j<=resolution;++j) {
                            int py = y + j;
                            nBlocked -= (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                        }
                    }
                    
                    { // add for (x+r,?)
                        int px = x + resolution;
                        for (int j=-resolution;j<=resolution;++j) {
                            int py = y + j;
                            nBlocked += (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                        }
                    }
                    
                    count[y][x] = nBlocked;
                }
            }

            // y > 0
            for (int y=1;y<sizeY;++y) {
                // y > 0, x = 0
                {
                    int x = 0;
                    int nBlocked = count[y-1][x];

                    { // subtract for (?,y-1-r)
                        int py = y - resolution - 1;
                        for (int i=-resolution;i<=resolution;++i) {
                            int px = x + i;
                            nBlocked -= (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                        }
                    }
                    
                    { // add for (?,y+r)
                        int py = y + resolution;
                        for (int i=-resolution;i<=resolution;++i) {
                            int px = x + i;
                            nBlocked += (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                        }
                    }

                    count[y][x] = nBlocked;
                }
                
                // y > 0, x > 0
                for (int x=1;x<sizeX;++x) {
                    int nBlocked = count[y-1][x] + count[y][x-1] - count[y-1][x-1];

                    { // add (x-1-r,y-1-r)
                        int px = x - resolution - 1;
                        int py = y - resolution - 1;
                        nBlocked += (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                    }
                    { // add (x+r,y+r)
                        int px = x + resolution;
                        int py = y + resolution;
                        nBlocked += (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                    }
                    { // subtract (x-1-r,y+r)
                        int px = x - resolution - 1;
                        int py = y + resolution;
                        nBlocked -= (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                    }
                    { // subtract (x+r,y-1-r)
                        int px = x + resolution;
                        int py = y - resolution - 1;
                        nBlocked -= (px < 0 || py < 0 || px >= sizeX || py >= sizeY || grid[py][px]) ? 1 : 0;
                    }
                    
                    count[y][x] = nBlocked;
                }
            }
            
            for (int y=0;y<sizeY;++y) {
                for (int x=0;x<sizeX;++x) {
                    grid[y][x] = (count[y][x] - (grid[y][x] ? 1 : 0) >= cutoff);
                }
            }
        }
        
        for (int y=0;y<sizeY;++y) {
            for (int x=0;x<sizeX;++x) {
                gridGraph.setBlocked(x, y, grid[y][x]);
            }
        }
    }

    /**
     * Generates a truly random map for the gridGraph.
     * No longer used as this does not generate very good or realistic grids.
     */
    private static void generateRandomMap_slow(Random rand, GridGraph gridGraph, int frequency, int iterations, int cutoffOffset) {
        int sizeX = gridGraph.sizeX;
        int sizeY = gridGraph.sizeY;
        int resolution = Math.max((sizeX+sizeY)/150, 1);
        int cutoff = (int)(0.8f*resolution*resolution + 1.75f*resolution + 0.8f) + cutoffOffset;

        System.out.println("Resolution " + resolution + ", Cutoff " + cutoff);
        
        boolean[][] grid = new boolean[sizeY][];
        boolean[][] grid2 = new boolean[sizeY][];
        for (int y=0;y<sizeY;++y) {
            grid[y] = new boolean[sizeX];
            grid2[y] = new boolean[sizeX];
            for (int x=0;x<sizeX;++x) {
                grid[y][x] = rand.nextInt()%frequency == 0;
            }
        }
        
        for (int itr=0;itr<iterations;++itr) {
            
            for (int y=0;y<sizeY;++y) {
                for (int x=0;x<sizeX;++x) {
                    int nBlocked = 0;
                    for (int i=-resolution;i<=resolution;++i) {
                        for (int j=-resolution;j<=resolution;++j) {
                            if (i == 0 && j == 0) continue;
                            int px = x + i;
                            int py = y + j;
                            if (px < 0 || py < 0 || px >= sizeX || py >= sizeY) {
                                nBlocked++;
                            } else {
                                nBlocked += grid[py][px] ? 1 : 0;
                            }
                        }
                    }
                    grid2[y][x] = nBlocked >= cutoff;
                }
            }
            
            boolean[][] temp = grid;
            grid = grid2;
            grid2 = temp;
        }
        
        for (int y=0;y<sizeY;++y) {
            for (int x=0;x<sizeX;++x) {
                gridGraph.setBlocked(x, y, grid[y][x]);
            }
        }
    }
    
    
}
