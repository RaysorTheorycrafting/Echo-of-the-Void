package com.eotv.echoofthevoid.event.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OldFriendRulesTest {
    private static final UUID HOST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");
    private static final UUID NAMELESS = UUID.fromString("00000000-0000-0000-0000-0000000000c0");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-0000000000d0");

    @Test
    void swordCadenceMatchesAPlayer() {
        assertEquals(13, OldFriendRules.attackCooldownTicks(1.6D), "sword: 1.6 attacks per second");
        assertEquals(5, OldFriendRules.attackCooldownTicks(4.0D), "bare hand");
        assertEquals(20, OldFriendRules.attackCooldownTicks(1.0D), "axe-like");
        assertEquals(20, OldFriendRules.attackCooldownTicks(0.0D));
    }

    @Test
    void miningUsesThePlayerFormula() {
        // Stone (1.5) with a wooden pickaxe (2.0, correct tool): 2 / 1.5 / 30 per tick, i.e. 22.5 ticks.
        assertEquals(2.0F / 1.5F / 30.0F, OldFriendRules.miningProgressPerTick(2.0F, 1.5F, true), 1.0E-6F);
        // Without the right tool the divisor is 100.
        assertEquals(1.0F / 1.5F / 100.0F, OldFriendRules.miningProgressPerTick(1.0F, 1.5F, false), 1.0E-6F);
        assertEquals(1.0F, OldFriendRules.miningProgressPerTick(1.0F, 0.0F, false), "instant blocks");
        assertEquals(0.0F, OldFriendRules.miningProgressPerTick(9.0F, -1.0F, true), "bedrock never breaks");
    }

    @Test
    void quietPeriodLastsTwoToThreeGameDays() {
        assertEquals(48_000L, OldFriendRules.quietTicks(0.0D));
        assertEquals(72_000L, OldFriendRules.quietTicks(1.0D));
    }

    @Test
    void friendsComeOnlyFromOtherSavesWithAKnownName(@TempDir Path root) throws IOException {
        Path current = world(root.resolve("saves/My World"), HOST, MEMBER);
        world(root.resolve("saves/Old LAN World"), HOST, ALEX, NAMELESS, MEMBER);
        world(root.resolve("saves/Another"), BOB);
        Files.writeString(root.resolve("usercache.json"), "[" + entry("Host", HOST) + "," + entry("Alex", ALEX)
                + "," + entry("Bob", BOB) + "," + entry("Member", MEMBER) + "]", StandardCharsets.UTF_8);

        List<OldFriendFiles.Friend> friends = OldFriendFiles.candidates(root, current.toAbsolutePath().normalize(), Set.of(HOST));

        assertEquals(List.of(new OldFriendFiles.Friend(ALEX, "Alex"), new OldFriendFiles.Friend(BOB, "Bob")), friends,
                "never the host, never a member of this world, never a player without a known name");
    }

    @Test
    void anOnlinePlayerWithoutLocalNameIsKeptToBeNamedByMojang(@TempDir Path root) throws IOException {
        // The user's own case: a LAN save whose friend expired from usercache.json long ago.
        UUID online = UUID.fromString("17296b43-977a-45ca-bf05-eb330097d376");
        Path current = world(root.resolve("saves/Current"), HOST);
        world(root.resolve("saves/Old LAN World"), HOST, online, NAMELESS);
        Files.writeString(root.resolve("usercache.json"), "[" + entry("Host", HOST) + "]", StandardCharsets.UTF_8);

        assertEquals(List.of(new OldFriendFiles.Friend(online, null)),
                OldFriendFiles.candidates(root, current.toAbsolutePath().normalize(), Set.of(HOST)),
                "the online id stays, unnamed; the offline nameless id can never be named");
    }

    @Test
    void neoForgeUsernameCacheNamesPlayersAndNamedOnesComeFirst(@TempDir Path root) throws IOException {
        UUID online = UUID.fromString("17296b43-977a-45ca-bf05-eb330097d376");
        Path current = world(root.resolve("saves/Current"), HOST);
        world(root.resolve("saves/Old"), online, BOB);
        Files.writeString(root.resolve("usernamecache.json"), "{\n  \"" + BOB + "\": \"Bob\"\n}", StandardCharsets.UTF_8);

        assertEquals(List.of(new OldFriendFiles.Friend(BOB, "Bob"), new OldFriendFiles.Friend(online, null)),
                OldFriendFiles.candidates(root, current.toAbsolutePath().normalize(), Set.of(HOST)));
    }

    @Test
    void namelessPlayersWhoReallyPlayedAreAskedFirst(@TempDir Path root) throws IOException {
        // Live QA: a test world held hundreds of fake players with online-style ids and tiny files.
        UUID friend = UUID.fromString("17296b43-977a-45ca-bf05-eb330097d376");
        UUID fake = UUID.fromString("0d2e367f-0ca4-4102-a2e5-ec5741ab951b");
        Path current = world(root.resolve("saves/Current"), HOST);
        Path old = world(root.resolve("saves/Old"), fake, friend);
        Files.createDirectories(old.resolve("advancements"));
        Files.writeString(old.resolve("advancements").resolve(fake + ".json"), "{\"DataVersion\":3955}", StandardCharsets.UTF_8);
        Files.writeString(old.resolve("advancements").resolve(friend + ".json"), "x".repeat(40_000), StandardCharsets.UTF_8);

        assertEquals(List.of(new OldFriendFiles.Friend(friend, null), new OldFriendFiles.Friend(fake, null)),
                OldFriendFiles.candidates(root, current.toAbsolutePath().normalize(), Set.of(HOST)));
        assertTrue(OldFriendRules.MAX_NAME_LOOKUPS <= 5, "a few lookups at most per trigger");
    }

    @Test
    void aDedicatedServerReadsSiblingWorldFolders(@TempDir Path root) throws IOException {
        Path current = world(root.resolve("world"), MEMBER);
        world(root.resolve("world_season1"), ALEX);
        Files.writeString(root.resolve("usercache.json"), "[" + entry("Alex", ALEX) + "]", StandardCharsets.UTF_8);
        assertEquals(List.of(new OldFriendFiles.Friend(ALEX, "Alex")),
                OldFriendFiles.candidates(root, current.toAbsolutePath().normalize(), Set.of()));
    }

    @Test
    void missingOrDamagedFilesMeanNobody(@TempDir Path root) throws IOException {
        Path current = world(root.resolve("saves/Only"), HOST);
        assertTrue(OldFriendFiles.candidates(root, current, Set.of()).isEmpty());
        Files.writeString(root.resolve("usercache.json"), "{not json", StandardCharsets.UTF_8);
        world(root.resolve("saves/Other"), ALEX);
        assertTrue(OldFriendFiles.candidates(root, current, Set.of()).isEmpty());
    }

    private static Path world(Path folder, UUID... players) throws IOException {
        Files.createDirectories(folder.resolve("playerdata"));
        Files.writeString(folder.resolve("level.dat"), "", StandardCharsets.UTF_8);
        for (UUID player : players) {
            Files.writeString(folder.resolve("playerdata").resolve(player + ".dat"), "", StandardCharsets.UTF_8);
        }
        return folder;
    }

    private static String entry(String name, UUID id) {
        return "{\"name\":\"" + name + "\",\"uuid\":\"" + id + "\",\"expiresOn\":\"2099-01-01 00:00:00 +0000\"}";
    }
}
