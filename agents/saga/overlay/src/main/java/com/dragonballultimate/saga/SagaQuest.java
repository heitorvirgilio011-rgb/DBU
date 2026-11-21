package com.dragonballultimate.saga;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public record SagaQuest(String id, String saga, int revision, String mentor, String prerequisite,
                        List<String> objectives, int seals, String milestone) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    static final int MAX_OBJECTIVES = 32;
    static final int MAX_SEALS = 1_000;

    public SagaQuest {
        requireId(id);
        requireId(saga);
        requireId(mentor);
        if (prerequisite != null) {
            requireId(prerequisite);
        }
        if (milestone != null) {
            requireId(milestone);
        }
        objectives = validatedObjectives(objectives);
        if (revision < 1 || objectives.isEmpty() || objectives.contains(mentor)
                || seals < 1 || seals > MAX_SEALS) {
            throw new IllegalArgumentException("Invalid quest definition: " + id);
        }
    }

    static void requireId(String id) {
        if (id == null || id.length() > 128 || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid saga ID: " + id);
        }
    }

    static List<String> validatedObjectives(List<String> objectives) {
        var copy = List.copyOf(objectives);
        if (copy.size() > MAX_OBJECTIVES || new HashSet<>(copy).size() != copy.size()) {
            throw new IllegalArgumentException("Too many or duplicate saga objectives");
        }
        copy.forEach(SagaQuest::requireId);
        return copy;
    }
}
