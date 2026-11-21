package com.dragonballultimate.saga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Mutable aggregate owned by one server thread. Clients can only receive immutable views.
 * Completion and its reward share an entry, so awarding never calls an external side effect.
 */
public final class SagaProgress {
    public record Receipt(int seals, String milestone) {
        public Receipt {
            if (seals < 1 || seals > SagaQuest.MAX_SEALS) {
                throw new IllegalArgumentException("Invalid number of saga seals");
            }
            if (milestone != null) {
                SagaQuest.requireId(milestone);
            }
        }
    }

    public record Entry(int revision, List<String> reached, Receipt receipt) {
        public Entry {
            reached = SagaQuest.validatedObjectives(reached);
            if (revision < 1 || (receipt != null && reached.isEmpty())) {
                throw new IllegalArgumentException("Invalid saga progress entry");
            }
        }
    }

    public enum Outcome { STARTED, ADVANCED, READY, COMPLETED, CURRENT, ALL_DONE, UNAVAILABLE, IGNORED }

    public record Result(Outcome outcome, String questId, String nextActor, int reached, int total,
                         Receipt receipt) {}

    private final Map<UUID, Map<String, Entry>> players = new HashMap<>();

    public SagaProgress() {}

    public SagaProgress(Map<UUID, Map<String, Entry>> state) {
        for (var player : state.entrySet()) {
            var entries = new HashMap<>(Map.copyOf(player.getValue()));
            entries.keySet().forEach(SagaQuest::requireId);
            if (entries.values().stream().filter(entry -> entry.receipt() == null).count() > 1) {
                throw new IllegalArgumentException("Multiple active quests for " + player.getKey());
            }
            players.put(Objects.requireNonNull(player.getKey()), entries);
        }
    }

    public Map<UUID, Map<String, Entry>> snapshot() {
        var copy = new HashMap<UUID, Map<String, Entry>>();
        players.forEach((player, entries) -> copy.put(player, Map.copyOf(entries)));
        return Map.copyOf(copy);
    }

    public Result interact(UUID player, String actor, SagaCatalog catalog) {
        Objects.requireNonNull(player);
        if (!catalog.knowsActor(actor)) {
            return status(Outcome.IGNORED);
        }
        var entries = players.getOrDefault(player, Map.of());
        for (var entry : entries.entrySet()) {
            if (entry.getValue().receipt() == null) {
                return advance(entries, entry.getKey(), entry.getValue(), actor, catalog);
            }
        }

        var offers = catalog.offers(actor);
        if (offers.isEmpty()) {
            return status(Outcome.IGNORED);
        }
        for (var quest : offers) {
            if (entries.containsKey(quest.id())) {
                continue;
            }
            if (quest.prerequisite() == null || entries.containsKey(quest.prerequisite())) {
                players.computeIfAbsent(player, ignored -> new HashMap<>())
                        .put(quest.id(), new Entry(quest.revision(), List.of(), null));
                return view(Outcome.STARTED, quest, quest.objectives().get(0), 0, null);
            }
        }
        boolean allDone = offers.stream().allMatch(quest -> entries.containsKey(quest.id()));
        return status(allDone ? Outcome.ALL_DONE : Outcome.UNAVAILABLE);
    }

    private Result advance(Map<String, Entry> entries, String id, Entry entry, String actor,
                           SagaCatalog catalog) {
        var quest = catalog.quest(id);
        var reached = entry.reached();
        if (quest == null || quest.revision() != entry.revision() || reached.size() > quest.objectives().size()
                || !quest.objectives().subList(0, reached.size()).equals(reached)) {
            return status(catalog.offers(actor).isEmpty() ? Outcome.IGNORED : Outcome.UNAVAILABLE);
        }
        int count = reached.size();
        if (actor.equals(quest.mentor())) {
            if (count < quest.objectives().size()) {
                return view(Outcome.CURRENT, quest, quest.objectives().get(count), count, null);
            }
            var receipt = new Receipt(quest.seals(), quest.milestone());
            entries.put(id, new Entry(entry.revision(), reached, receipt));
            return view(Outcome.COMPLETED, quest, null, count, receipt);
        }

        if (count == quest.objectives().size() || !quest.objectives().get(count).equals(actor)) {
            return status(Outcome.IGNORED);
        }
        var advanced = new ArrayList<>(reached);
        advanced.add(actor);
        entries.put(id, new Entry(entry.revision(), advanced, null));
        if (advanced.size() == quest.objectives().size()) {
            return view(Outcome.READY, quest, quest.mentor(), advanced.size(), null);
        }
        return view(Outcome.ADVANCED, quest, quest.objectives().get(advanced.size()), advanced.size(), null);
    }

    public boolean hasMilestone(UUID player, String id) {
        SagaQuest.requireId(id);
        return players.getOrDefault(player, Map.of()).values().stream()
                .anyMatch(entry -> entry.receipt() != null && id.equals(entry.receipt().milestone()));
    }

    public long seals(UUID player) {
        return players.getOrDefault(player, Map.of()).values().stream()
                .filter(entry -> entry.receipt() != null)
                .mapToLong(entry -> entry.receipt().seals())
                .sum();
    }

    private static Result status(Outcome outcome) {
        return new Result(outcome, null, null, 0, 0, null);
    }

    private static Result view(Outcome outcome, SagaQuest quest, String nextActor, int reached, Receipt receipt) {
        return new Result(outcome, quest.id(), nextActor, reached, quest.objectives().size(), receipt);
    }
}
