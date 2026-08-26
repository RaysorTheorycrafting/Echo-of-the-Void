package com.eotv.echoofthevoid.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DevMenuStabilitySurfaceTest {
    private static final Path DEV_MENU_SOURCE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "client", "UncannyDevMenuScreen.java");
    private static final Path EVENT_SYSTEM_SOURCE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "event", "UncannyParanoiaEventSystem.java");
    private static final Path SOUND_DELIVERY_SOURCE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "sound", "UncannySoundDelivery.java");
    private static final Path CLIENT_AUDIO_SOURCE = Path.of(
            "src", "main", "java", "com", "eotv", "echoofthevoid", "client", "UncannyClientAudioEffects.java");

    @Test
    void listStaysInsideItsPanelAndDetailsRenderAboveWidgets() throws IOException {
        String source = read(DEV_MENU_SOURCE);
        assertTrue(source.contains("new EntryList(this.minecraft, this.listWidth,"));
        assertTrue(source.contains("this.entryList.setX(this.listLeft);"));

        String renderMethod = section(source, "public void render(GuiGraphics", "private void renderDetail(");
        int widgets = renderMethod.indexOf("super.render(graphics, mouseX, mouseY, partialTick);");
        int title = renderMethod.indexOf("graphics.drawString(this.font, TITLE");
        assertTrue(widgets >= 0 && title > widgets, "Details must remain above the list viewport rendering");
    }

    @Test
    void menuExplicitlyPausesIntegratedSingleplayer() throws IOException {
        String source = read(DEV_MENU_SOURCE);
        String pauseMethod = section(source, "public boolean isPauseScreen()", "private void updateFilterLabels()");
        assertTrue(pauseMethod.contains("return true;"));
        assertFalse(pauseMethod.contains("return false;"));
    }

    @Test
    void everyProjectedShadowVariantUsesPrivateNonPositionalAudio() throws IOException {
        String eventSource = read(EVENT_SYSTEM_SOURCE);
        String projectedShadow = section(
                eventSource,
                "private static boolean triggerProjectedShadow(ServerPlayer player, ProjectedShadowVariant variant",
                "public static boolean triggerHunterFog(");
        assertEquals(3, occurrences(projectedShadow, "playMentalSound("));
        assertFalse(projectedShadow.contains("playLocalSound("));
        assertTrue(read(SOUND_DELIVERY_SOURCE).contains("PacketDistributor.sendToPlayer(player"));
        assertTrue(read(CLIENT_AUDIO_SOURCE).contains("SoundInstance.Attenuation.NONE"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "Missing start marker: " + startMarker);
        assertTrue(end > start, "Missing end marker: " + endMarker);
        return source.substring(start, end);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
