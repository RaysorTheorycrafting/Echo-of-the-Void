package com.eotv.echoofthevoid.lore;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

/** Minecraft item adapter for the pure, validated journal catalog. */
public final class UncannyLoreBookLibrary {
    private UncannyLoreBookLibrary() {
    }

    public static int volumeCount() {
        return UncannyJournalCatalog.count();
    }

    public static int clampVolume(int volume) {
        return Math.max(1, Math.min(volume, volumeCount()));
    }

    public static WrittenBookContent contentForVolume(int volume) {
        UncannyJournalCatalog.Journal journal = UncannyJournalCatalog.journal(clampVolume(volume));
        List<Filterable<Component>> pages = journal.pages().stream()
                .map(page -> Filterable.<Component>passThrough(Component.literal(page)))
                .toList();
        return new WrittenBookContent(
                Filterable.passThrough(journal.title()),
                "Unknown",
                0,
                pages,
                true);
    }

    public static WrittenBookContent defaultContent() {
        return contentForVolume(1);
    }
}
