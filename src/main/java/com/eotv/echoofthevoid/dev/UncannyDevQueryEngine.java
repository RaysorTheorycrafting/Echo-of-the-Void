package com.eotv.echoofthevoid.dev;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Pure filtering and ordering rules shared by the dev screen and characterization tests. */
public final class UncannyDevQueryEngine {
    private UncannyDevQueryEngine() {
    }

    public static List<UncannyDevCatalog.Entry> query(
            List<UncannyDevCatalog.Entry> source,
            Query query,
            Set<String> favoriteIds,
            List<String> recentIds,
            Function<String, UncannyDevCatalog.QaStatus> statusResolver) {
        Query effective = query == null ? Query.defaults() : query;
        Set<String> favorites = favoriteIds == null ? Set.of() : favoriteIds;
        List<String> recent = recentIds == null ? List.of() : recentIds;
        Function<String, UncannyDevCatalog.QaStatus> statuses = statusResolver == null
                ? ignored -> UncannyDevCatalog.QaStatus.GRAY
                : statusResolver;
        String needle = normalize(effective.search());

        List<UncannyDevCatalog.Entry> result = new ArrayList<>();
        for (UncannyDevCatalog.Entry entry : source == null ? List.<UncannyDevCatalog.Entry>of() : source) {
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(entry);
            if (effective.category() != UncannyDevCatalog.Category.ALL
                    && entry.category() != effective.category()) {
                continue;
            }
            if (effective.availableAtPhase() > 0 && info.minimumPhase() > effective.availableAtPhase()) {
                continue;
            }
            if (effective.danger() != null && info.danger() != effective.danger()) {
                continue;
            }
            if (effective.qaFilter() != QaFilter.ALL
                    && statuses.apply(entry.id()) != effective.qaFilter().status()) {
                continue;
            }
            if (effective.scope() == Scope.FAVORITES && !favorites.contains(entry.id())) {
                continue;
            }
            if (effective.scope() == Scope.RECENT && !recent.contains(entry.id())) {
                continue;
            }
            if (!needle.isEmpty() && !info.searchText(entry).contains(needle)) {
                continue;
            }
            result.add(entry);
        }

        result.sort(comparator(effective.sortMode(), recent));
        return List.copyOf(result);
    }

    private static Comparator<UncannyDevCatalog.Entry> comparator(SortMode mode, List<String> recentIds) {
        Comparator<UncannyDevCatalog.Entry> byName = Comparator
                .comparing((UncannyDevCatalog.Entry entry) -> entry.groupLabel().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> entry.label().toLowerCase(Locale.ROOT))
                .thenComparing(UncannyDevCatalog.Entry::id);
        return switch (mode == null ? SortMode.NAME : mode) {
            case NAME -> byName;
            case PHASE -> Comparator
                    .comparingInt((UncannyDevCatalog.Entry entry) -> UncannyDevMetadataCatalog.describe(entry).minimumPhase())
                    .thenComparing(byName);
            case RARITY -> Comparator
                    .comparingInt((UncannyDevCatalog.Entry entry) -> UncannyDevMetadataCatalog.describe(entry).rarity().sortRank())
                    .reversed()
                    .thenComparing(byName);
            case DANGER -> Comparator
                    .comparingInt((UncannyDevCatalog.Entry entry) -> UncannyDevMetadataCatalog.describe(entry).danger().sortRank())
                    .reversed()
                    .thenComparing(byName);
            case TYPE -> Comparator
                    .comparingInt((UncannyDevCatalog.Entry entry) -> UncannyDevMetadataCatalog.describe(entry).type().sortRank())
                    .thenComparing(byName);
            case RECENT -> recentComparator(recentIds).thenComparing(byName);
        };
    }

    private static Comparator<UncannyDevCatalog.Entry> recentComparator(List<String> recentIds) {
        Map<String, Integer> positions = new HashMap<>();
        for (int index = 0; index < recentIds.size(); index++) {
            positions.putIfAbsent(recentIds.get(index), index);
        }
        return Comparator.comparingInt(entry -> positions.getOrDefault(entry.id(), Integer.MAX_VALUE));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public enum SortMode {
        NAME("Name"),
        PHASE("Phase"),
        RARITY("Rarity"),
        DANGER("Danger"),
        TYPE("Type"),
        RECENT("Recent");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public enum QaFilter {
        ALL("All QA", null),
        GRAY("Not tested", UncannyDevCatalog.QaStatus.GRAY),
        ORANGE("Review", UncannyDevCatalog.QaStatus.ORANGE),
        GREEN("Validated", UncannyDevCatalog.QaStatus.GREEN);

        private final String label;
        private final UncannyDevCatalog.QaStatus status;

        QaFilter(String label, UncannyDevCatalog.QaStatus status) {
            this.label = label;
            this.status = status;
        }

        public String label() {
            return this.label;
        }

        public UncannyDevCatalog.QaStatus status() {
            return this.status;
        }
    }

    public enum Scope {
        ALL("All entries"),
        FAVORITES("Favorites"),
        RECENT("Recent tests");

        private final String label;

        Scope(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public record Query(
            String search,
            UncannyDevCatalog.Category category,
            int availableAtPhase,
            UncannyDevMetadataCatalog.Danger danger,
            QaFilter qaFilter,
            Scope scope,
            SortMode sortMode) {
        public Query {
            search = search == null ? "" : search;
            category = category == null ? UncannyDevCatalog.Category.ALL : category;
            availableAtPhase = Math.max(0, Math.min(4, availableAtPhase));
            qaFilter = qaFilter == null ? QaFilter.ALL : qaFilter;
            scope = scope == null ? Scope.ALL : scope;
            sortMode = sortMode == null ? SortMode.NAME : sortMode;
        }

        public static Query defaults() {
            return new Query("", UncannyDevCatalog.Category.ALL, 0, null, QaFilter.ALL, Scope.ALL, SortMode.NAME);
        }
    }
}
