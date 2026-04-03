package com.eotv.echoofthevoid.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class UncannyAltarShapeData {
    private static final double[][] MODEL_BOXES_WITH_CUBE = {
            {2.8, 20.8, 2.8, 13.2, 31.2, 13.2},
            {0.85, 13.0, 0.85, 15.15, 20.8, 15.15},
            {-2.4, 10.4, -2.4, 18.4, 13.0, 18.4},
            {-5.0, 7.8, -5.0, 21.0, 10.4, 21.0},
            {-7.6, 5.2, -7.6, 23.6, 7.8, 23.6},
            {-10.2, 2.6, -10.2, 26.2, 5.2, 26.2},
            {-10.525, 0.0, -10.525, 26.525, 4.55, 26.525},
            {4.1, 0.0, 26.2, 11.9, 2.6, 28.8},
            {4.425, 2.6, 21.325, 11.575, 10.4, 28.475},
            {4.75, 10.4, 21.65, 11.25, 13.0, 28.15},
            {6.05, 13.0, 22.95, 9.95, 16.9, 26.85},
            {22.95, 13.0, 6.05, 26.85, 16.9, 9.95},
            {26.2, 0.0, 4.1, 28.8, 2.6, 11.9},
            {21.325, 2.6, 4.425, 28.475, 10.4, 11.575},
            {21.65, 10.4, 4.75, 28.15, 13.0, 11.25},
            {4.1, 0.0, -12.8, 11.9, 2.6, -10.2},
            {4.425, 2.6, -12.475, 11.575, 10.4, -5.325},
            {4.75, 10.4, -12.15, 11.25, 13.0, -5.65},
            {6.05, 13.0, -10.85, 9.95, 16.9, -6.95},
            {-12.475, 2.6, 4.425, -5.325, 10.4, 11.575},
            {-12.8, 0.0, 4.1, -10.2, 2.6, 11.9},
            {-10.85, 13.0, 6.05, -6.95, 16.9, 9.95},
            {-12.15, 10.4, 4.75, -5.65, 13.0, 11.25},
            {24.25, 24.7, 8.65, 25.55, 26.0, 8.65},
            {24.25, 24.7, 8.0, 25.55, 26.0, 8.0},
            {23.6, 16.9, 6.7, 26.2, 24.7, 9.3},
            {-9.55, 24.7, 8.65, -8.25, 26.0, 8.65},
            {-9.55, 24.7, 8.0, -8.25, 26.0, 8.0},
            {-10.2, 16.9, 6.7, -7.6, 24.7, 9.3},
            {6.7, 16.9, 23.6, 9.3, 24.7, 26.2},
            {7.35, 24.7, 24.9, 8.65, 26.0, 24.9},
            {7.35, 24.7, 25.55, 8.65, 26.0, 25.55},
            {6.7, 16.9, -10.2, 9.3, 24.7, -7.6},
            {7.35, 24.7, -8.9, 8.65, 26.0, -8.9},
            {7.35, 24.7, -8.25, 8.65, 26.0, -8.25}
    };

    private static final VoxelShape[][] LOWER_WITH_CUBE = new VoxelShape[3][3];
    private static final VoxelShape[][] UPPER_WITH_CUBE = new VoxelShape[3][3];
    private static final VoxelShape[][] LOWER_NO_CUBE = new VoxelShape[3][3];
    private static final VoxelShape[][] UPPER_NO_CUBE = new VoxelShape[3][3];

    static {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                int ix = ox + 1;
                int iz = oz + 1;
                LOWER_WITH_CUBE[ix][iz] = buildCellShape(ox, oz, false, true);
                UPPER_WITH_CUBE[ix][iz] = buildCellShape(ox, oz, true, true);
                LOWER_NO_CUBE[ix][iz] = buildCellShape(ox, oz, false, false);
                UPPER_NO_CUBE[ix][iz] = buildCellShape(ox, oz, true, false);
            }
        }
    }

    private UncannyAltarShapeData() {
    }

    public static VoxelShape getCellShape(int offsetX, int offsetZ, boolean upper, boolean hasCube) {
        int ix = offsetX + 1;
        int iz = offsetZ + 1;
        if (ix < 0 || ix > 2 || iz < 0 || iz > 2) {
            return Shapes.empty();
        }
        if (upper) {
            return hasCube ? UPPER_WITH_CUBE[ix][iz] : UPPER_NO_CUBE[ix][iz];
        }
        return hasCube ? LOWER_WITH_CUBE[ix][iz] : LOWER_NO_CUBE[ix][iz];
    }

    private static VoxelShape buildCellShape(int offsetX, int offsetZ, boolean upper, boolean includeCube) {
        double cellMinX = offsetX * 16.0D;
        double cellMinZ = offsetZ * 16.0D;
        double cellMaxX = cellMinX + 16.0D;
        double cellMaxZ = cellMinZ + 16.0D;
        double cellMinY = upper ? 16.0D : 0.0D;
        double cellMaxY = upper ? 32.0D : 16.0D;

        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < MODEL_BOXES_WITH_CUBE.length; i++) {
            if (!includeCube && i == 0) {
                continue;
            }
            double[] box = MODEL_BOXES_WITH_CUBE[i];
            double x1 = Math.max(box[0], cellMinX);
            double y1 = Math.max(box[1], cellMinY);
            double z1 = Math.max(box[2], cellMinZ);
            double x2 = Math.min(box[3], cellMaxX);
            double y2 = Math.min(box[4], cellMaxY);
            double z2 = Math.min(box[5], cellMaxZ);
            if (x2 <= x1 || y2 <= y1 || z2 <= z1) {
                continue;
            }
            shape = Shapes.or(
                    shape,
                    Block.box(
                            x1 - cellMinX,
                            y1 - cellMinY,
                            z1 - cellMinZ,
                            x2 - cellMinX,
                            y2 - cellMinY,
                            z2 - cellMinZ));
        }
        return shape.optimize();
    }
}
