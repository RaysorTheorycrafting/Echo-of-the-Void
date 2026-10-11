package com.eotv.echoofthevoid.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Places structure templates drawn with StructureForge ({@code data/echoofthevoid/structure/<name>.nbt}, sources
 * in {@code Ressources/Structures/}). A template is drawn with its front facing south; {@link #place} turns it to
 * face any side and returns its data markers in world coordinates.
 */
public final class StructureForgeTemplates {
    private StructureForgeTemplates() {
    }

    /** A data marker: its name, its world position, and its facing turned like the template (null if none). */
    public record Marker(String name, BlockPos pos, Direction facing) {
    }

    /** The rotation that turns a south-facing template towards {@code front}. */
    public static Rotation rotationFor(Direction front) {
        return switch (front) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static boolean exists(ServerLevel level, String name) {
        return level.getStructureManager().get(id(name)).isPresent();
    }

    /**
     * Places the template so that its local position {@code anchorLocal} (template coordinates of the script's
     * origin, from the .markers.json) lands on {@code anchor}, front towards {@code front}. Data markers are not
     * placed (their cell keeps what was there before; set it yourself from the returned list). Blocks are set
     * without neighbour updates, like the rest of the journal places.
     *
     * @return the markers by name, or {@code null} when the template does not exist
     */
    public static Map<String, List<Marker>> place(ServerLevel level, String name, BlockPos anchor, BlockPos anchorLocal,
            Direction front) {
        Optional<StructureTemplate> found = level.getStructureManager().get(id(name));
        if (found.isEmpty()) {
            return null;
        }
        StructureTemplate template = found.get();
        Rotation rotation = rotationFor(front);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        BlockPos origin = anchor.subtract(StructureTemplate.calculateRelativePosition(settings, anchorLocal));
        Map<String, List<Marker>> markers = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(origin, settings, Blocks.STRUCTURE_BLOCK)) {
            if (info.nbt() == null || !"DATA".equals(info.nbt().getString("mode"))) {
                continue;
            }
            String[] meta = info.nbt().getString("metadata").trim().split("\\s+");
            Direction facing = meta.length > 1 ? Direction.byName(meta[1]) : null;
            if (facing != null && facing.getAxis().isHorizontal()) {
                facing = rotation.rotate(facing);
            }
            markers.computeIfAbsent(meta[0], key -> new ArrayList<>()).add(new Marker(meta[0], info.pos(), facing));
        }
        template.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS);
        return markers;
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("echoofthevoid", name);
    }
}
