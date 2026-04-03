package com.eotv.echoofthevoid.entity;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.entity.custom.UncannyCreeperEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDoubleDormantEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyDrownedEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyEndermanEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyEndermiteEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyEvokerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyGhastEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyHoglinEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyHuskEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyIronGolemEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyKnockerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyMagmaCubeEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyPillagerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyPiglinBruteEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyPhantomEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyPulseEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyRavagerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannySkeletonEntity;
import com.eotv.echoofthevoid.entity.custom.UncannySlimeEntity;
import com.eotv.echoofthevoid.entity.custom.UncannySpiderEntity;
import com.eotv.echoofthevoid.entity.custom.UncannySpiderlingEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStructureVillagerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStalkerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyStrayEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyShadowEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyHurlerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyTerrorEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyUsherEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyKeeperEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyTenantEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyFollowerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyVindicatorEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyWatcherEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyWitherSkeletonEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyZombieEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyZombieVillagerEntity;
import com.eotv.echoofthevoid.entity.custom.UncannyBlazeEntity;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class UncannyEntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, EchoOfTheVoid.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyZombieEntity>> UNCANNY_ZOMBIE = registerMonster(
            "uncanny_zombie", () -> EntityType.Builder.of(UncannyZombieEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_zombie")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyHuskEntity>> UNCANNY_HUSK = registerMonster(
            "uncanny_husk", () -> EntityType.Builder.of(UncannyHuskEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_husk")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyDrownedEntity>> UNCANNY_DROWNED = registerMonster(
            "uncanny_drowned", () -> EntityType.Builder.of(UncannyDrownedEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_drowned")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyZombieVillagerEntity>> UNCANNY_ZOMBIE_VILLAGER = registerMonster(
            "uncanny_zombie_villager", () -> EntityType.Builder.of(UncannyZombieVillagerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_zombie_villager")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannySkeletonEntity>> UNCANNY_SKELETON = registerMonster(
            "uncanny_skeleton", () -> EntityType.Builder.of(UncannySkeletonEntity::new, MobCategory.MONSTER).sized(0.6F, 1.99F).build(id("uncanny_skeleton")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyStrayEntity>> UNCANNY_STRAY = registerMonster(
            "uncanny_stray", () -> EntityType.Builder.of(UncannyStrayEntity::new, MobCategory.MONSTER).sized(0.6F, 1.99F).build(id("uncanny_stray")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyCreeperEntity>> UNCANNY_CREEPER = registerMonster(
            "uncanny_creeper", () -> EntityType.Builder.of(UncannyCreeperEntity::new, MobCategory.MONSTER).sized(0.6F, 1.7F).build(id("uncanny_creeper")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannySpiderEntity>> UNCANNY_SPIDER = registerMonster(
            "uncanny_spider", () -> EntityType.Builder.of(UncannySpiderEntity::new, MobCategory.MONSTER).sized(1.4F, 0.9F).build(id("uncanny_spider")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannySpiderlingEntity>> UNCANNY_SPIDERLING = registerMonster(
            "uncanny_spiderling", () -> EntityType.Builder.of(UncannySpiderlingEntity::new, MobCategory.MONSTER).sized(0.2F, 0.12F).build(id("uncanny_spiderling")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyEndermanEntity>> UNCANNY_ENDERMAN = registerMonster(
            "uncanny_enderman", () -> EntityType.Builder.of(UncannyEndermanEntity::new, MobCategory.MONSTER).sized(0.6F, 2.9F).build(id("uncanny_enderman")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyEndermiteEntity>> UNCANNY_ENDERMITE = registerMonster(
            "uncanny_endermite", () -> EntityType.Builder.of(UncannyEndermiteEntity::new, MobCategory.MONSTER).sized(0.4F, 0.3F).build(id("uncanny_endermite")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyGhastEntity>> UNCANNY_GHAST = registerMonster(
            "uncanny_ghast", () -> EntityType.Builder.of(UncannyGhastEntity::new, MobCategory.MONSTER).sized(4.0F, 4.0F).build(id("uncanny_ghast")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyPhantomEntity>> UNCANNY_PHANTOM = registerMonster(
            "uncanny_phantom", () -> EntityType.Builder.of(UncannyPhantomEntity::new, MobCategory.MONSTER).sized(0.9F, 0.5F).build(id("uncanny_phantom")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyDoubleDormantEntity>> UNCANNY_DOUBLE_DORMANT = registerMonster(
            "uncanny_double_dormant", () -> EntityType.Builder.of(UncannyDoubleDormantEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_double_dormant")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyIronGolemEntity>> UNCANNY_IRON_GOLEM = registerMonster(
            "uncanny_iron_golem", () -> EntityType.Builder.of(UncannyIronGolemEntity::new, MobCategory.MONSTER).sized(1.4F, 2.7F).build(id("uncanny_iron_golem")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyPillagerEntity>> UNCANNY_PILLAGER = registerMonster(
            "uncanny_pillager", () -> EntityType.Builder.of(UncannyPillagerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_pillager")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyVindicatorEntity>> UNCANNY_VINDICATOR = registerMonster(
            "uncanny_vindicator", () -> EntityType.Builder.of(UncannyVindicatorEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_vindicator")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyEvokerEntity>> UNCANNY_EVOKER = registerMonster(
            "uncanny_evoker", () -> EntityType.Builder.of(UncannyEvokerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_evoker")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyRavagerEntity>> UNCANNY_RAVAGER = registerMonster(
            "uncanny_ravager", () -> EntityType.Builder.of(UncannyRavagerEntity::new, MobCategory.MONSTER).sized(1.95F, 2.2F).build(id("uncanny_ravager")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyBlazeEntity>> UNCANNY_BLAZE = registerMonster(
            "uncanny_blaze", () -> EntityType.Builder.of(UncannyBlazeEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build(id("uncanny_blaze")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyWitherSkeletonEntity>> UNCANNY_WITHER_SKELETON = registerMonster(
            "uncanny_wither_skeleton", () -> EntityType.Builder.of(UncannyWitherSkeletonEntity::new, MobCategory.MONSTER).sized(0.7F, 2.4F).build(id("uncanny_wither_skeleton")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyPiglinBruteEntity>> UNCANNY_PIGLIN_BRUTE = registerMonster(
            "uncanny_piglin_brute", () -> EntityType.Builder.of(UncannyPiglinBruteEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_piglin_brute")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyHoglinEntity>> UNCANNY_HOGLIN = registerMonster(
            "uncanny_hoglin", () -> EntityType.Builder.of(UncannyHoglinEntity::new, MobCategory.MONSTER).sized(1.4F, 1.4F).build(id("uncanny_hoglin")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannySlimeEntity>> UNCANNY_SLIME = registerMonster(
            "uncanny_slime", () -> EntityType.Builder.of(UncannySlimeEntity::new, MobCategory.MONSTER).sized(2.04F, 2.04F).build(id("uncanny_slime")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyMagmaCubeEntity>> UNCANNY_MAGMA_CUBE = registerMonster(
            "uncanny_magma_cube", () -> EntityType.Builder.of(UncannyMagmaCubeEntity::new, MobCategory.MONSTER).sized(2.04F, 2.04F).build(id("uncanny_magma_cube")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyWatcherEntity>> UNCANNY_WATCHER = registerMonster(
            "uncanny_watcher", () -> EntityType.Builder.of(UncannyWatcherEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_watcher")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyStalkerEntity>> UNCANNY_STALKER = registerMonster(
            "uncanny_stalker", () -> EntityType.Builder.of(UncannyStalkerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_stalker")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyHurlerEntity>> UNCANNY_HURLER = registerMonster(
            "uncanny_hurler", () -> EntityType.Builder.of(UncannyHurlerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_hurler")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyShadowEntity>> UNCANNY_SHADOW = registerMonster(
            "uncanny_shadow", () -> EntityType.Builder.of(UncannyShadowEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_shadow")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyKnockerEntity>> UNCANNY_KNOCKER = registerMonster(
            "uncanny_knocker", () -> EntityType.Builder.of(UncannyKnockerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_knocker")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyPulseEntity>> UNCANNY_PULSE = registerMonster(
            "uncanny_pulse", () -> EntityType.Builder.of(UncannyPulseEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_pulse")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyTerrorEntity>> UNCANNY_TERROR = registerMonster(
            "uncanny_terror", () -> EntityType.Builder.of(UncannyTerrorEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_terror")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyUsherEntity>> UNCANNY_USHER = registerMonster(
            "uncanny_usher", () -> EntityType.Builder.of(UncannyUsherEntity::new, MobCategory.MONSTER).sized(0.6F, 4.0F).build(id("uncanny_usher")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyKeeperEntity>> UNCANNY_KEEPER = registerMonster(
            "uncanny_keeper", () -> EntityType.Builder.of(UncannyKeeperEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_keeper")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyTenantEntity>> UNCANNY_TENANT = registerMonster(
            "uncanny_tenant", () -> EntityType.Builder.of(UncannyTenantEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_tenant")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyFollowerEntity>> UNCANNY_FOLLOWER = registerMonster(
            "uncanny_follower", () -> EntityType.Builder.of(UncannyFollowerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_follower")));

    public static final DeferredHolder<EntityType<?>, EntityType<UncannyStructureVillagerEntity>> UNCANNY_STRUCTURE_VILLAGER = registerMonster(
            "uncanny_structure_villager", () -> EntityType.Builder.of(UncannyStructureVillagerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build(id("uncanny_structure_villager")));

    private static final Map<EntityType<? extends Mob>, Supplier<? extends EntityType<? extends Mob>>> VANILLA_TO_UNCANNY = new HashMap<>();
    private static final Map<String, Supplier<? extends EntityType<? extends Mob>>> COMMAND_TO_UNCANNY = new HashMap<>();

    static {
        registerMapping(EntityType.ZOMBIE, "uncanny_zombie", UNCANNY_ZOMBIE);
        registerMapping(EntityType.HUSK, "uncanny_husk", UNCANNY_HUSK);
        registerMapping(EntityType.DROWNED, "uncanny_drowned", UNCANNY_DROWNED);
        registerMapping(EntityType.ZOMBIE_VILLAGER, "uncanny_zombie_villager", UNCANNY_ZOMBIE_VILLAGER);
        registerMapping(EntityType.SKELETON, "uncanny_skeleton", UNCANNY_SKELETON);
        registerMapping(EntityType.STRAY, "uncanny_stray", UNCANNY_STRAY);
        registerMapping(EntityType.CREEPER, "uncanny_creeper", UNCANNY_CREEPER);
        registerMapping(EntityType.SPIDER, "uncanny_spider", UNCANNY_SPIDER);
        registerMapping(EntityType.ENDERMAN, "uncanny_enderman", UNCANNY_ENDERMAN);
        registerMapping(EntityType.ENDERMITE, "uncanny_endermite", UNCANNY_ENDERMITE);
        registerMapping(EntityType.GHAST, "uncanny_ghast", UNCANNY_GHAST);
        registerMapping(EntityType.PHANTOM, "uncanny_phantom", UNCANNY_PHANTOM);
        registerMapping(EntityType.IRON_GOLEM, "uncanny_iron_golem", UNCANNY_IRON_GOLEM);
        registerMapping(EntityType.PILLAGER, "uncanny_pillager", UNCANNY_PILLAGER);
        registerMapping(EntityType.VINDICATOR, "uncanny_vindicator", UNCANNY_VINDICATOR);
        registerMapping(EntityType.EVOKER, "uncanny_evoker", UNCANNY_EVOKER);
        registerMapping(EntityType.RAVAGER, "uncanny_ravager", UNCANNY_RAVAGER);
        registerMapping(EntityType.BLAZE, "uncanny_blaze", UNCANNY_BLAZE);
        registerMapping(EntityType.WITHER_SKELETON, "uncanny_wither_skeleton", UNCANNY_WITHER_SKELETON);
        registerMapping(EntityType.PIGLIN_BRUTE, "uncanny_piglin_brute", UNCANNY_PIGLIN_BRUTE);
        registerMapping(EntityType.HOGLIN, "uncanny_hoglin", UNCANNY_HOGLIN);
        registerMapping(EntityType.SLIME, "uncanny_slime", UNCANNY_SLIME);
        registerMapping(EntityType.MAGMA_CUBE, "uncanny_magma_cube", UNCANNY_MAGMA_CUBE);

        COMMAND_TO_UNCANNY.put("uncanny_double_dormant", UNCANNY_DOUBLE_DORMANT);
        COMMAND_TO_UNCANNY.put("double_dormant", UNCANNY_DOUBLE_DORMANT);
        COMMAND_TO_UNCANNY.put("uncanny_mimic", UNCANNY_DOUBLE_DORMANT);
        COMMAND_TO_UNCANNY.put("mimic", UNCANNY_DOUBLE_DORMANT);
        COMMAND_TO_UNCANNY.put("uncanny_watcher", UNCANNY_WATCHER);
        COMMAND_TO_UNCANNY.put("watcher", UNCANNY_WATCHER);
        COMMAND_TO_UNCANNY.put("uncanny_stalker", UNCANNY_STALKER);
        COMMAND_TO_UNCANNY.put("stalker", UNCANNY_STALKER);
        COMMAND_TO_UNCANNY.put("uncanny_attacker", UNCANNY_STALKER);
        COMMAND_TO_UNCANNY.put("attacker", UNCANNY_STALKER);
        COMMAND_TO_UNCANNY.put("uncanny_hurler", UNCANNY_HURLER);
        COMMAND_TO_UNCANNY.put("hurler", UNCANNY_HURLER);
        COMMAND_TO_UNCANNY.put("uncanny_shadow", UNCANNY_SHADOW);
        COMMAND_TO_UNCANNY.put("shadow", UNCANNY_SHADOW);
        COMMAND_TO_UNCANNY.put("uncanny_knocker", UNCANNY_KNOCKER);
        COMMAND_TO_UNCANNY.put("knocker", UNCANNY_KNOCKER);
        COMMAND_TO_UNCANNY.put("uncanny_pulse", UNCANNY_PULSE);
        COMMAND_TO_UNCANNY.put("pulse", UNCANNY_PULSE);
        COMMAND_TO_UNCANNY.put("uncanny_terror", UNCANNY_TERROR);
        COMMAND_TO_UNCANNY.put("terror", UNCANNY_TERROR);
        COMMAND_TO_UNCANNY.put("uncanny_usher", UNCANNY_USHER);
        COMMAND_TO_UNCANNY.put("usher", UNCANNY_USHER);
        COMMAND_TO_UNCANNY.put("uncanny_keeper", UNCANNY_KEEPER);
        COMMAND_TO_UNCANNY.put("keeper", UNCANNY_KEEPER);
        COMMAND_TO_UNCANNY.put("uncanny_tenant", UNCANNY_TENANT);
        COMMAND_TO_UNCANNY.put("tenant", UNCANNY_TENANT);
        COMMAND_TO_UNCANNY.put("uncanny_follower", UNCANNY_FOLLOWER);
        COMMAND_TO_UNCANNY.put("follower", UNCANNY_FOLLOWER);
        COMMAND_TO_UNCANNY.put("uncanny_structure_villager", UNCANNY_STRUCTURE_VILLAGER);
        COMMAND_TO_UNCANNY.put("structure_villager", UNCANNY_STRUCTURE_VILLAGER);
    }

    private UncannyEntityRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(UncannyEntityRegistry::onEntityAttributeCreation);
    }

    public static EntityType<? extends Mob> getReplacement(EntityType<?> vanillaType) {
        Supplier<? extends EntityType<? extends Mob>> supplier = VANILLA_TO_UNCANNY.get(vanillaType);
        return supplier != null ? supplier.get() : null;
    }

    public static EntityType<? extends Mob> byCommandType(String rawName) {
        Supplier<? extends EntityType<? extends Mob>> supplier = COMMAND_TO_UNCANNY.get(rawName.toLowerCase(Locale.ROOT));
        return supplier == null ? null : supplier.get();
    }

    public static boolean isSpecialEntity(EntityType<?> type) {
        return type == UNCANNY_DOUBLE_DORMANT.get()
                || type == UNCANNY_WATCHER.get()
                || type == UNCANNY_STALKER.get()
                || type == UNCANNY_HURLER.get()
                || type == UNCANNY_SHADOW.get()
                || type == UNCANNY_KNOCKER.get()
                || type == UNCANNY_PULSE.get()
                || type == UNCANNY_TERROR.get()
                || type == UNCANNY_USHER.get()
                || type == UNCANNY_KEEPER.get()
                || type == UNCANNY_TENANT.get()
                || type == UNCANNY_FOLLOWER.get();
    }

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(UNCANNY_ZOMBIE.get(), Zombie.createAttributes().build());
        event.put(UNCANNY_HUSK.get(), Husk.createAttributes().build());
        event.put(UNCANNY_DROWNED.get(), Drowned.createAttributes().build());
        event.put(UNCANNY_ZOMBIE_VILLAGER.get(), ZombieVillager.createAttributes().build());

        AttributeSupplier.Builder skeletonAttributes = Skeleton.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
        event.put(UNCANNY_SKELETON.get(), skeletonAttributes.build());

        AttributeSupplier.Builder strayAttributes = Stray.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
        event.put(UNCANNY_STRAY.get(), strayAttributes.build());

        event.put(UNCANNY_CREEPER.get(), Creeper.createAttributes().build());
        event.put(UNCANNY_SPIDER.get(), Spider.createAttributes().build());

        AttributeSupplier.Builder spiderlingAttributes = Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D);
        event.put(UNCANNY_SPIDERLING.get(), spiderlingAttributes.build());

        event.put(UNCANNY_ENDERMAN.get(), EnderMan.createAttributes().build());
        event.put(UNCANNY_ENDERMITE.get(), Endermite.createAttributes().build());
        event.put(UNCANNY_GHAST.get(), Ghast.createAttributes().build());
        event.put(UNCANNY_PILLAGER.get(), Pillager.createAttributes().build());
        event.put(UNCANNY_VINDICATOR.get(), Vindicator.createAttributes().build());
        event.put(UNCANNY_EVOKER.get(), Evoker.createAttributes().build());
        event.put(UNCANNY_RAVAGER.get(), Ravager.createAttributes().build());
        event.put(UNCANNY_BLAZE.get(), Blaze.createAttributes().build());
        event.put(UNCANNY_WITHER_SKELETON.get(), WitherSkeleton.createAttributes().build());
        event.put(UNCANNY_PIGLIN_BRUTE.get(), PiglinBrute.createAttributes().build());
        event.put(UNCANNY_HOGLIN.get(), Hoglin.createAttributes().build());

        AttributeSupplier.Builder slimeAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
        event.put(UNCANNY_SLIME.get(), slimeAttributes.build());

        AttributeSupplier.Builder magmaCubeAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
        event.put(UNCANNY_MAGMA_CUBE.get(), magmaCubeAttributes.build());

        AttributeSupplier.Builder phantomAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.9D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
        event.put(UNCANNY_PHANTOM.get(), phantomAttributes.build());

        event.put(UNCANNY_DOUBLE_DORMANT.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.38D).build());
        event.put(UNCANNY_IRON_GOLEM.get(), IronGolem.createAttributes().build());

        AttributeSupplier.Builder watcherAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 96.0D);
        event.put(UNCANNY_WATCHER.get(), watcherAttributes.build());

        AttributeSupplier.Builder stalkerAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
        event.put(UNCANNY_STALKER.get(), stalkerAttributes.build());

        AttributeSupplier.Builder hurlerAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
        event.put(UNCANNY_HURLER.get(), hurlerAttributes.build());

        AttributeSupplier.Builder shadowAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
        event.put(UNCANNY_SHADOW.get(), shadowAttributes.build());

        AttributeSupplier.Builder knockerAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
        event.put(UNCANNY_KNOCKER.get(), knockerAttributes.build());

        AttributeSupplier.Builder pulseAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.14D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        event.put(UNCANNY_PULSE.get(), pulseAttributes.build());

        AttributeSupplier.Builder terrorAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        event.put(UNCANNY_TERROR.get(), terrorAttributes.build());

        AttributeSupplier.Builder usherAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
        event.put(UNCANNY_USHER.get(), usherAttributes.build());

        AttributeSupplier.Builder keeperAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
        event.put(UNCANNY_KEEPER.get(), keeperAttributes.build());

        AttributeSupplier.Builder tenantAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
        event.put(UNCANNY_TENANT.get(), tenantAttributes.build());

        AttributeSupplier.Builder followerAttributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
        event.put(UNCANNY_FOLLOWER.get(), followerAttributes.build());

        AttributeSupplier.Builder structureVillagerAttributes = Villager.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
        event.put(UNCANNY_STRUCTURE_VILLAGER.get(), structureVillagerAttributes.build());
    }

    private static void registerMapping(
            EntityType<? extends Mob> vanilla,
            String commandKey,
            Supplier<? extends EntityType<? extends Mob>> uncanny) {
        VANILLA_TO_UNCANNY.put(vanilla, uncanny);

        COMMAND_TO_UNCANNY.put(commandKey, uncanny);
        COMMAND_TO_UNCANNY.put(commandKey.replace("uncanny_", ""), uncanny);
    }

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> registerMonster(
            String name,
            Supplier<EntityType<T>> entitySupplier) {
        return ENTITY_TYPES.register(name, entitySupplier);
    }

    private static String id(String path) {
        return EchoOfTheVoid.MODID + ":" + path;
    }
}

