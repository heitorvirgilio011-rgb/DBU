package com.dragonballultimate.saga;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class SagaCatalog {
    private static final SagaCatalog EMPTY = new SagaCatalog(List.of());
    private final Map<String, SagaQuest> quests;
    private final Map<String, List<SagaQuest>> offers;
    private final Set<String> actors;

    public SagaCatalog(Collection<SagaQuest> definitions) {
        var sorted = new TreeMap<String, SagaQuest>();
        for (var quest : definitions) {
            if (sorted.putIfAbsent(quest.id(), quest) != null) {
                throw new IllegalArgumentException("Duplicate quest: " + quest.id());
            }
        }
        if (reachableQuestIds(sorted).size() != sorted.size()) {
            throw new IllegalArgumentException("Missing or cyclic saga prerequisites");
        }
        var byMentor = new HashMap<String, List<SagaQuest>>();
        var knownActors = new HashSet<String>();
        for (var quest : sorted.values()) {
            byMentor.computeIfAbsent(quest.mentor(), ignored -> new ArrayList<>()).add(quest);
            knownActors.add(quest.mentor());
            knownActors.addAll(quest.objectives());
        }
        byMentor.replaceAll((mentor, list) -> List.copyOf(list));
        quests = Map.copyOf(sorted);
        offers = Map.copyOf(byMentor);
        actors = Set.copyOf(knownActors);
    }

    public static SagaCatalog empty() {
        return EMPTY;
    }

    public boolean knowsActor(String actor) {
        return actors.contains(actor);
    }

    SagaQuest quest(String id) {
        return quests.get(id);
    }

    List<SagaQuest> offers(String mentor) {
        return offers.getOrDefault(mentor, List.of());
    }

    static Set<String> reachableQuestIds(Map<String, SagaQuest> definitions) {
        var roots = new ArrayDeque<String>();
        var dependents = new HashMap<String, List<String>>();
        for (var quest : definitions.values()) {
            String prerequisite = quest.prerequisite();
            if (prerequisite == null) {
                roots.add(quest.id());
                continue;
            }
            dependents.computeIfAbsent(prerequisite, ignored -> new ArrayList<>()).add(quest.id());
        }
        var visited = new HashSet<String>();
        while (!roots.isEmpty()) {
            String id = roots.remove();
            visited.add(id);
            roots.addAll(dependents.getOrDefault(id, List.of()));
        }
        return visited;
    }
}
