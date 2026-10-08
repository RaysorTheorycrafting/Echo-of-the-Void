package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.event.paranoia.DebugBoundsRules;
import com.eotv.echoofthevoid.network.UncannyDebugBoundsPayload;
import com.eotv.echoofthevoid.network.UncannyDebugHitboxStatePayload;
import com.eotv.echoofthevoid.sound.UncannySoundRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Private rendering for the hidden F3+B encounter. No fake entity enters the client level. */
public final class UncannyDebugBoundsClientEffects {
    private static final List<Presence> PRESENCES = new ArrayList<>();
    private static final List<SoundInstance> POSITIONAL_SOUNDS = new ArrayList<>();
    private static ClientLevel trackedLevel;
    private static boolean hitboxStateInitialized;
    private static boolean lastHitboxState;
    private static boolean active;
    private static int elapsedTicks;
    private static int durationTicks;
    private static SoundInstance stressBed;
    private static int fakeHitsDealt;
    private static int nextPresenceSoundTick;
    private static int presenceSoundsInBurst;
    private static int fakeHitCooldown;

    private UncannyDebugBoundsClientEffects() {
    }

    public static void apply(UncannyDebugBoundsPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        stopPresentation(minecraft, "payload_replace");
        if (!payload.active() || minecraft.level == null || minecraft.player == null) {
            return;
        }

        int count = Mth.clamp(payload.presenceCount(), 1, DebugBoundsRules.PRESENCE_COUNT);
        durationTicks = Mth.clamp(payload.durationTicks(), 20, DebugBoundsRules.DURATION_TICKS);
        elapsedTicks = 0;
        for (int index = 0; index < count; index++) {
            double radius = DebugBoundsRules.startRadius(payload.seed(), index);
            double angle = DebugBoundsRules.angleRadians(payload.seed(), index, count);
            DebugBoundsRules.Shape shape = DebugBoundsRules.shape(payload.seed(), index);
            boolean aerial = shape == DebugBoundsRules.Shape.FLYING;
            int firstSoundTick = 12 + (index * 19) % 118;
            Vec3 initialPosition = minecraft.player.position().add(
                    Math.cos(angle) * radius,
                    aerial ? 2.0D : 0.0D,
                    Math.sin(angle) * radius);
            Presence presence = new Presence(
                    index,
                    payload.seed(),
                    initialPosition,
                    shape,
                    DebugBoundsRules.huntSpeed(payload.seed(), index, shape),
                    DebugBoundsRules.noticeDelayTicks(payload.seed(), index),
                    firstSoundTick);
            if (!aerial) {
                // Real mobs stand on the ground from the first frame, never hover into place.
                presence.position = new Vec3(
                        initialPosition.x,
                        presence.findGroundY(minecraft.level, initialPosition.x, initialPosition.z, initialPosition.y),
                        initialPosition.z);
            }
            PRESENCES.add(presence);
        }
        // The first striking bodies to notice the player become strikers; everyone else stalks.
        PRESENCES.stream()
                .filter(presence -> presence.strikes)
                .sorted(java.util.Comparator.comparingInt(presence -> presence.noticeTick))
                .limit(DebugBoundsRules.MAXIMUM_STRIKERS)
                .forEach(presence -> presence.striker = true);
        active = true;
        nextPresenceSoundTick = 40;
        presenceSoundsInBurst = 0;
        fakeHitsDealt = 0;
        fakeHitCooldown = 0;
        startStressBed(minecraft);
        UncannyClientDiagnostics.enqueue(
                "INFO", "debug_bounds_started",
                "Hidden hitbox presentation started without creating entities",
                "count=" + count + ";duration_ticks=" + durationTicks);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level != trackedLevel) {
            stopPresentation(minecraft, "level_changed");
            trackedLevel = minecraft.level;
            hitboxStateInitialized = false;
        }
        if (player == null || minecraft.getConnection() == null) {
            stopPresentation(minecraft, "no_player");
            return;
        }

        boolean hitboxes = minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes();
        if (!hitboxStateInitialized || hitboxes != lastHitboxState) {
            PacketDistributor.sendToServer(new UncannyDebugHitboxStatePayload(hitboxes));
            lastHitboxState = hitboxes;
            hitboxStateInitialized = true;
        }
        if (!active) {
            return;
        }
        if (minecraft.isPaused()) {
            // A paused game freezes every presence exactly where it stands.
            return;
        }
        if (!hitboxes || !player.isAlive() || ++elapsedTicks >= durationTicks) {
            stopPresentation(minecraft, !hitboxes ? "hitboxes_disabled" : "completed");
            return;
        }

        if (fakeHitCooldown > 0) {
            fakeHitCooldown--;
        }
        for (Presence presence : PRESENCES) {
            presence.advanceToward(minecraft.level, player);
            if (presence.readyToStrike(player)) {
                fakeStrike(minecraft, player, presence);
            }
        }
        // One breath or whisper at a time, from the nearest hunting presence, with silences between.
        if (elapsedTicks >= nextPresenceSoundTick) {
            Presence nearest = PRESENCES.stream()
                    .filter(Presence::hunting)
                    .min(java.util.Comparator.comparingDouble(presence -> presence.horizontalDistance(player.position())))
                    .orElse(null);
            if (nearest != null) {
                playPresenceSound(minecraft, player, nearest);
            }
            presenceSoundsInBurst++;
            long roll = Double.doubleToLongBits(DebugBoundsRules.unit(elapsedTicks * 131L + presenceSoundsInBurst));
            boolean pause = presenceSoundsInBurst >= 3 + (int) (Math.abs(roll) % 3);
            if (pause) {
                presenceSoundsInBurst = 0;
            }
            nextPresenceSoundTick = elapsedTicks + (pause ? 90 + (int) (Math.abs(roll) % 80) : 30 + (int) (Math.abs(roll) % 35));
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !active || PRESENCES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Presence presence : PRESENCES) {
            Vec3 position = presence.position;
            AABB box = new AABB(
                    position.x - presence.width * 0.5D,
                    position.y,
                    position.z - presence.width * 0.5D,
                    position.x + presence.width * 0.5D,
                    position.y + presence.height,
                    position.z + presence.width * 0.5D).move(
                            -cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            // Same three marks as a real F3+B box: white body, red eye plane, blue view vector.
            LevelRenderer.renderLineBox(poseStack, lines, box, 1.0F, 1.0F, 1.0F, 1.0F);
            double eyeY = position.y + presence.height * 0.85D;
            AABB eyePlane = new AABB(
                    position.x - presence.width * 0.5D,
                    eyeY - 0.01D,
                    position.z - presence.width * 0.5D,
                    position.x + presence.width * 0.5D,
                    eyeY + 0.01D,
                    position.z + presence.width * 0.5D).move(
                            -cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            LevelRenderer.renderLineBox(poseStack, lines, eyePlane, 1.0F, 0.0F, 0.0F, 1.0F);
            double lookLength = 2.0D;
            double endX = position.x + presence.lookX * lookLength;
            double endY = eyeY + presence.lookY * lookLength;
            double endZ = position.z + presence.lookZ * lookLength;
            AABB lookRay = new AABB(
                    Math.min(position.x, endX) - 0.008D,
                    Math.min(eyeY, endY) - 0.008D,
                    Math.min(position.z, endZ) - 0.008D,
                    Math.max(position.x, endX) + 0.008D,
                    Math.max(eyeY, endY) + 0.008D,
                    Math.max(position.z, endZ) + 0.008D).move(
                            -cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            LevelRenderer.renderLineBox(poseStack, lines, lookRay, 0.0F, 0.0F, 1.0F, 1.0F);
        }
        buffers.endBatch(RenderType.lines());
    }

    public static String diagnosticState() {
        return "active=" + active + ",elapsed=" + elapsedTicks + ",presences=" + PRESENCES.size();
    }

    private static void startStressBed(Minecraft minecraft) {
        stressBed = new SimpleSoundInstance(
                UncannySoundRegistry.UNCANNY_TERROR_LOCK.get().getLocation(),
                SoundSource.AMBIENT,
                0.72F,
                0.68F,
                SoundInstance.createUnseededRandom(),
                true,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true);
        minecraft.getSoundManager().play(stressBed);
    }

    private static void playPresenceSound(Minecraft minecraft, LocalPlayer player, Presence presence) {
        Vec3 point = presence.position;
        SoundEvent sound = presence.index % 4 == 0
                ? UncannySoundRegistry.UNCANNY_WHISPER.get()
                : UncannySoundRegistry.UNCANNY_MONSTER_BREATH.get();
        float volume = 0.34F + (float) DebugBoundsRules.unit(presence.index * 97L + elapsedTicks) * 0.24F;
        float pitch = 0.58F + (float) DebugBoundsRules.unit(presence.index * 193L + elapsedTicks) * 0.48F;
        SimpleSoundInstance instance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.HOSTILE,
                volume,
                pitch,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.LINEAR,
                point.x,
                point.y + presence.height * 0.72D,
                point.z,
                false);
        POSITIONAL_SOUNDS.add(instance);
        minecraft.getSoundManager().play(instance);
    }

    /** A felt but harmless blow: hurt shake, hurt sound and a shove away from the presence. */
    private static void fakeStrike(Minecraft minecraft, LocalPlayer player, Presence presence) {
        presence.struck = true;
        fakeHitsDealt++;
        fakeHitCooldown = DebugBoundsRules.FAKE_HIT_COOLDOWN_TICKS;
        Vec3 push = player.position().subtract(presence.position).multiply(1.0D, 0.0D, 1.0D);
        if (push.lengthSqr() < 1.0E-4D) {
            push = new Vec3(presence.lookX, 0.0D, presence.lookZ);
        }
        push = push.normalize();
        float yaw = (float) (Mth.atan2(push.z, push.x) * (180.0D / Math.PI)) - player.getYRot();
        player.animateHurt(yaw);
        player.push(push.x * 0.42D, 0.22D, push.z * 0.42D);
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                net.minecraft.sounds.SoundEvents.PLAYER_HURT.getLocation(),
                SoundSource.PLAYERS,
                1.0F,
                0.9F + (float) DebugBoundsRules.unit(presence.index * 31L + elapsedTicks) * 0.2F,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true));
        UncannyClientDiagnostics.enqueue(
                "INFO", "debug_bounds_fake_strike",
                "A hitbox presence struck harmlessly",
                "index=" + presence.index + ";strike=" + fakeHitsDealt + ";elapsed=" + elapsedTicks);
        // The presence recoils like a mob after a hit, then keeps hunting without striking again.
        presence.velocityX = -push.x * 0.3D;
        presence.velocityZ = -push.z * 0.3D;
    }

    private static void stopPresentation(Minecraft minecraft, String reason) {
        if (stressBed != null) {
            minecraft.getSoundManager().stop(stressBed);
            stressBed = null;
        }
        for (SoundInstance sound : POSITIONAL_SOUNDS) {
            minecraft.getSoundManager().stop(sound);
        }
        POSITIONAL_SOUNDS.clear();
        PRESENCES.clear();
        if (active) {
            UncannyClientDiagnostics.enqueue(
                    "INFO", "debug_bounds_stopped", "Hidden hitbox presentation stopped", "reason=" + reason);
        }
        active = false;
        elapsedTicks = 0;
        durationTicks = 0;
    }

    private static final class Presence {
        private final int index;
        private final long seed;
        private final double width;
        private final double height;
        private final boolean aerial;
        private final boolean strikes;
        private final double huntSpeed;
        private final int noticeTick;
        private final Vec3 home;
        private Vec3 position;
        private Vec3 pursuitTarget;
        private double velocityX;
        private double velocityY;
        private double velocityZ;
        private boolean onGround;
        private boolean struck;
        private boolean striker;
        private int stuckTicks;
        private double bestDistance = Double.MAX_VALUE;
        private double lookX;
        private double lookY;
        private double lookZ;
        private int retargetTicks;
        private int targetGeneration;
        private int nextSoundTick;

        private Presence(
                int index,
                long seed,
                Vec3 position,
                DebugBoundsRules.Shape shape,
                double huntSpeed,
                int noticeTick,
                int nextSoundTick) {
            this.index = index;
            this.seed = seed;
            this.position = position;
            this.home = position;
            this.width = shape.width();
            this.height = shape.height();
            this.aerial = shape == DebugBoundsRules.Shape.FLYING;
            this.strikes = shape.strikes();
            this.huntSpeed = huntSpeed;
            this.noticeTick = noticeTick;
            this.pursuitTarget = position;
            this.lookX = Math.cos(DebugBoundsRules.angleRadians(seed, index, DebugBoundsRules.PRESENCE_COUNT) + Math.PI);
            this.lookZ = Math.sin(DebugBoundsRules.angleRadians(seed, index, DebugBoundsRules.PRESENCE_COUNT) + Math.PI);
            this.retargetTicks = 0;
            this.nextSoundTick = nextSoundTick;
        }

        private boolean hunting() {
            return elapsedTicks >= noticeTick;
        }

        private boolean readyToStrike(LocalPlayer player) {
            return striker
                    && hunting()
                    && !struck
                    && onGround
                    && fakeHitCooldown <= 0
                    && fakeHitsDealt < DebugBoundsRules.MAXIMUM_FAKE_HITS
                    && horizontalDistance(player.position()) <= DebugBoundsRules.STRIKE_DISTANCE
                    && Math.abs(player.getY() - position.y) <= 1.5D;
        }

        private double horizontalDistance(Vec3 point) {
            return Math.hypot(point.x - position.x, point.z - position.z);
        }

        private void advanceToward(ClientLevel level, LocalPlayer player) {
            if (aerial) {
                flutterAround(level, player);
                return;
            }
            double desiredSpeed;
            if (hunting()) {
                // Each one closes in on its own side of the player so they do not stack in a line.
                double side = DebugBoundsRules.angleRadians(seed, index, DebugBoundsRules.PRESENCE_COUNT);
                double hold = !striker
                        ? DebugBoundsRules.stalkRadius(seed, index)
                        : struck ? DebugBoundsRules.AFTER_STRIKE_RADIUS : DebugBoundsRules.CONTACT_DISTANCE;
                this.pursuitTarget = player.position().add(Math.cos(side) * hold, 0.0D, Math.sin(side) * hold);
                double toPlayer = horizontalDistance(player.position());
                // Stalkers close in fast, then creep and hold their ring; nobody stands inside the player.
                desiredSpeed = toPlayer <= DebugBoundsRules.CONTACT_DISTANCE ? 0.0D
                        : !striker && toPlayer <= hold + 1.5D ? Math.min(huntSpeed, 0.04D)
                        : huntSpeed;
            } else {
                if (--this.retargetTicks <= 0) {
                    DebugBoundsRules.Step offset = DebugBoundsRules.targetOffset(seed, index, targetGeneration);
                    this.pursuitTarget = home.add(offset.x(), 0.0D, offset.z());
                    this.retargetTicks = DebugBoundsRules.retargetInterval(seed, index, targetGeneration++);
                }
                desiredSpeed = DebugBoundsRules.WANDER_SPEED;
            }

            double dx = pursuitTarget.x - position.x;
            double dz = pursuitTarget.z - position.z;
            double distance = Math.hypot(dx, dz);
            double wantX = distance > 0.05D ? dx / distance * Math.min(desiredSpeed, distance) : 0.0D;
            double wantZ = distance > 0.05D ? dz / distance * Math.min(desiredSpeed, distance) : 0.0D;
            // Accelerate like a walking mob instead of sliding at a constant rate.
            velocityX += (wantX - velocityX) * (onGround ? 0.35D : 0.08D);
            velocityZ += (wantZ - velocityZ) * (onGround ? 0.35D : 0.08D);

            if (hunting()) {
                Vec3 eyes = player.getEyePosition();
                Vec3 toward = eyes.subtract(position.add(0.0D, height * 0.85D, 0.0D));
                if (toward.lengthSqr() > 1.0E-6D) {
                    toward = toward.normalize();
                    lookX = toward.x;
                    lookY = toward.y;
                    lookZ = toward.z;
                }
            } else if (velocityX * velocityX + velocityZ * velocityZ > 1.0E-6D) {
                double length = Math.hypot(velocityX, velocityZ);
                lookX = velocityX / length;
                lookY = 0.0D;
                lookZ = velocityZ / length;
            }

            // Horizontal move with a Vanilla-like step: a one-block ledge is jumped, a wall stops.
            Vec3 horizontal = new Vec3(position.x + velocityX, position.y, position.z + velocityZ);
            if (level.noCollision(boundsAt(horizontal))) {
                position = horizontal;
            } else if (onGround && level.noCollision(boundsAt(horizontal.add(0.0D, 1.05D, 0.0D)))) {
                velocityY = DebugBoundsRules.JUMP_VELOCITY;
                onGround = false;
            } else if (!onGround && level.noCollision(boundsAt(horizontal.add(0.0D, 0.6D, 0.0D)))
                    && level.noCollision(boundsAt(position.add(0.0D, 0.6D, 0.0D)))) {
                // Mid-jump, a last nudge over the ledge edge, like Vanilla step-up.
                position = horizontal.add(0.0D, 0.6D, 0.0D);
            } else {
                Vec3 alongX = new Vec3(position.x + velocityX, position.y, position.z);
                Vec3 alongZ = new Vec3(position.x, position.y, position.z + velocityZ);
                if (level.noCollision(boundsAt(alongX))) {
                    position = alongX;
                } else if (level.noCollision(boundsAt(alongZ))) {
                    position = alongZ;
                }
            }

            // Gravity in Vanilla order (move, then accelerate), so a jump clears a full block.
            Vec3 vertical = new Vec3(position.x, position.y + velocityY, position.z);
            if (level.noCollision(boundsAt(vertical))) {
                position = vertical;
                onGround = false;
            } else {
                if (velocityY <= 0.0D) {
                    position = new Vec3(position.x, findGroundY(level, position.x, position.z, position.y), position.z);
                    onGround = true;
                }
                velocityY = 0.0D;
            }
            velocityY = onGround ? 0.0D : (velocityY - DebugBoundsRules.GRAVITY) * 0.98D;
            if (onGround && level.noCollision(boundsAt(position.add(0.0D, -0.05D, 0.0D)))) {
                // Walked off a ledge: start falling next tick.
                onGround = false;
            }
            trackProgress(level, player);
        }

        private void trackProgress(ClientLevel level, LocalPlayer player) {
            if (!striker || struck || !hunting()) {
                stuckTicks = 0;
                return;
            }
            double distance = horizontalDistance(player.position());
            if (distance < bestDistance - 0.15D) {
                bestDistance = distance;
                stuckTicks = 0;
                return;
            }
            if (++stuckTicks < DebugBoundsRules.STUCK_REPOSITION_TICKS) {
                return;
            }
            // Blocked by a wall or a cliff: reappear right behind the player, where they are not looking.
            Vec3 view = player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
            if (view.lengthSqr() < 1.0E-4D) {
                return;
            }
            view = view.normalize();
            for (int attempt = 0; attempt < 8; attempt++) {
                double spread = (attempt - 3.5D) * 0.22D;
                double cos = Math.cos(spread);
                double sin = Math.sin(spread);
                Vec3 back = new Vec3(-(view.x * cos - view.z * sin), 0.0D, -(view.x * sin + view.z * cos));
                Vec3 spot = player.position().add(back.scale(2.6D));
                double groundY = findGroundY(level, spot.x, spot.z, player.getY());
                Vec3 candidate = new Vec3(spot.x, groundY, spot.z);
                if (Math.abs(groundY - player.getY()) <= 1.0D && level.noCollision(boundsAt(candidate))) {
                    position = candidate;
                    velocityX = 0.0D;
                    velocityZ = 0.0D;
                    stuckTicks = 0;
                    bestDistance = 2.6D;
                    return;
                }
            }
            stuckTicks = 0;
        }

        /** Bat-sized boxes flit erratically around the player's head, as bats do. */
        private void flutterAround(ClientLevel level, LocalPlayer player) {
            if (--this.retargetTicks <= 0) {
                DebugBoundsRules.Step offset = DebugBoundsRules.targetOffset(seed, index, targetGeneration);
                double rise = 1.2D + DebugBoundsRules.unit(seed ^ targetGeneration * 31L + index) * 1.8D;
                Vec3 anchor = hunting() ? player.position() : home;
                this.pursuitTarget = anchor.add(offset.x(), rise, offset.z());
                this.retargetTicks = 8 + (int) (DebugBoundsRules.unit(seed + index * 17L + targetGeneration) * 18.0D);
                targetGeneration++;
            }
            Vec3 toward = pursuitTarget.subtract(position);
            double length = toward.length();
            if (length > 1.0E-4D) {
                Vec3 step = toward.scale(Math.min(0.16D, length) / length);
                velocityX += (step.x - velocityX) * 0.25D;
                velocityY += (step.y - velocityY) * 0.25D;
                velocityZ += (step.z - velocityZ) * 0.25D;
            }
            Vec3 next = position.add(velocityX, velocityY, velocityZ);
            if (level.noCollision(boundsAt(next))) {
                position = next;
            } else {
                retargetTicks = 0;
            }
            double horizontal = Math.hypot(velocityX, velocityZ);
            if (horizontal > 1.0E-4D) {
                lookX = velocityX / horizontal;
                lookZ = velocityZ / horizontal;
            }
            lookY = 0.0D;
        }

        private AABB boundsAt(Vec3 point) {
            return new AABB(
                    point.x - width * 0.5D,
                    point.y,
                    point.z - width * 0.5D,
                    point.x + width * 0.5D,
                    point.y + height,
                    point.z + width * 0.5D);
        }

        private double findGroundY(ClientLevel level, double x, double z, double aroundY) {
            BlockPos origin = BlockPos.containing(x, aroundY, z);
            for (int dy = 2; dy >= -24; dy--) {
                BlockPos feet = origin.offset(0, dy, 0);
                if (!level.hasChunkAt(feet)) {
                    continue;
                }
                BlockPos support = feet.below();
                Vec3 point = new Vec3(x, feet.getY(), z);
                if (!level.getBlockState(support).getCollisionShape(level, support).isEmpty()
                        && level.noCollision(boundsAt(point))) {
                    double top = support.getY() + level.getBlockState(support).getCollisionShape(level, support).max(
                            net.minecraft.core.Direction.Axis.Y);
                    return Math.max(feet.getY() - 1.0D, top);
                }
            }
            return aroundY;
        }
    }
}
