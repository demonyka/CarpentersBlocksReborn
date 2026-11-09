package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Carpenter's Stairs, 28 shapes. 1.7.10 numbered them exactly like the first 28 slopes
 * (side, straight, interior corner, exterior corner), and placement, joining and rotation
 * follow the same rules, so a stairs shape is identified by that {@link SlopeType}.
 * Geometry is made of block eighths ("octants").
 */
public final class StairsShape {
    public static final int COUNT = 28;

    @SuppressWarnings("unchecked")
    private static final List<Box>[] BOXES = new List[COUNT];

    static {
        for (int id = 0; id < COUNT; id++) {
            BOXES[id] = Collections.unmodifiableList(merge(octants(SlopeType.byId(id))));
        }
    }

    private StairsShape() {
    }

    public static SlopeType type(int id) {
        return SlopeType.byId(Math.max(0, Math.min(COUNT - 1, id)));
    }

    public static boolean isStairs(SlopeType type) {
        return type.ordinal() < COUNT;
    }

    public static List<Box> boxes(int id) {
        return BOXES[Math.max(0, Math.min(COUNT - 1, id))];
    }

    /** Hammer right-click: the slope cycle, except exterior downward corners wrap to the start (1.7.10). */
    public static int next(int id) {
        SlopeType type = type(id);
        if (type.family == SlopeType.Family.WEDGE_EXT && !type.positive) {
            return 0;
        }
        return type.next().ordinal();
    }

    /** Hammer left-click: a quarter turn clockwise. */
    public static int rotate(int id) {
        return type(id).rotateY().ordinal();
    }

    /** Filled eighths, indexed [x][y][z] with 0 = west / bottom / north half. */
    static boolean[][][] octants(SlopeType t) {
        boolean[][][] o = new boolean[2][2][2];
        int qx = t.facings.contains(Side.EAST) ? 1 : 0;
        int qz = t.facings.contains(Side.SOUTH) ? 1 : 0;
        switch (t.family) {
            case WEDGE_SIDE:
                // Three full-height quarter columns: the one at the facing corner is missing.
                fill(o, true);
                o[qx][0][qz] = o[qx][1][qz] = false;
                return o;
            case WEDGE: {
                // A slab plus the back half (away from the open side) of the other layer.
                Side open = t.facings.stream().filter(s -> s.dy == 0).findFirst().orElse(Side.NORTH);
                int full = t.positive ? 0 : 1, step = 1 - full;
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        o[x][full][z] = true;
                        boolean back = open.dx != 0 ? (open.dx < 0 ? x == 1 : x == 0) : (open.dz < 0 ? z == 1 : z == 0);
                        o[x][step][z] = back;
                    }
                }
                return o;
            }
            case WEDGE_INT: {
                // Everything but one quarter of the step layer.
                fill(o, true);
                o[qx][t.positive ? 1 : 0][qz] = false;
                return o;
            }
            case WEDGE_EXT: {
                // A slab plus one quarter of the step layer, diagonally opposite the facing corner.
                int full = t.positive ? 0 : 1;
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        o[x][full][z] = true;
                    }
                }
                o[1 - qx][1 - full][1 - qz] = true;
                return o;
            }
            default:
                throw new IllegalArgumentException("Not a stairs shape: " + t);
        }
    }

    private static void fill(boolean[][][] o, boolean value) {
        for (boolean[][] a : o) {
            for (boolean[] b : a) {
                java.util.Arrays.fill(b, value);
            }
        }
    }

    /** Greedy merge of eighths into as few boxes as possible (fewer hidden inner faces). */
    static List<Box> merge(boolean[][][] o) {
        boolean[][][] used = new boolean[2][2][2];
        List<Box> boxes = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!o[x][y][z] || used[x][y][z]) {
                        continue;
                    }
                    int x2 = x, z2 = z, y2 = y;
                    while (x2 + 1 < 2 && o[x2 + 1][y][z] && !used[x2 + 1][y][z]) {
                        x2++;
                    }
                    while (z2 + 1 < 2 && rowFree(o, used, x, x2, y, z2 + 1)) {
                        z2++;
                    }
                    while (y2 + 1 < 2 && layerFree(o, used, x, x2, y2 + 1, z, z2)) {
                        y2++;
                    }
                    for (int a = x; a <= x2; a++) {
                        for (int b = y; b <= y2; b++) {
                            for (int c = z; c <= z2; c++) {
                                used[a][b][c] = true;
                            }
                        }
                    }
                    boxes.add(new Box(x * 0.5, y * 0.5, z * 0.5, (x2 + 1) * 0.5, (y2 + 1) * 0.5, (z2 + 1) * 0.5));
                }
            }
        }
        return boxes;
    }

    private static boolean rowFree(boolean[][][] o, boolean[][][] used, int x1, int x2, int y, int z) {
        for (int x = x1; x <= x2; x++) {
            if (!o[x][y][z] || used[x][y][z]) {
                return false;
            }
        }
        return true;
    }

    private static boolean layerFree(boolean[][][] o, boolean[][][] used, int x1, int x2, int y, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
            if (!rowFree(o, used, x1, x2, y, z)) {
                return false;
            }
        }
        return true;
    }
}
