package com.dragonballultimate.saga;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

public final class SagaReload extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> FIELDS = Set.of(
            "saga", "revision", "mentor", "prerequisite", "objectives", "seals", "milestone");
    private static volatile SagaCatalog catalog = SagaCatalog.empty();

    SagaReload() {
        super(new Gson(), "saga_quests");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new SagaReload());
    }

    public static SagaCatalog catalog() {
        return catalog;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<String, JsonElement> ordered = new TreeMap<>();
        resources.forEach((id, json) -> ordered.put(id.toString(), json));
        Map<String, SagaQuest> parsed = new TreeMap<>();
        ordered.forEach((id, json) -> {
            try {
                parsed.put(id, parse(id, json));
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Ignoring invalid SAGA quest {}: {}", id, exception.getMessage());
            }
        });
        List<SagaQuest> valid = withValidPrerequisites(parsed);
        catalog = new SagaCatalog(valid);
        LOGGER.info("Loaded {} SAGA quests ({} skipped after JSON loading)",
                valid.size(), ordered.size() - valid.size());
    }

    private static SagaQuest parse(String id, JsonElement input) {
        if (input == null || !input.isJsonObject()) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        JsonObject json = input.getAsJsonObject();
        for (String field : json.keySet()) {
            if (!FIELDS.contains(field)) {
                throw new IllegalArgumentException("unknown field " + field);
            }
        }
        JsonElement objectiveList = json.get("objectives");
        if (objectiveList == null || !objectiveList.isJsonArray()) {
            throw new IllegalArgumentException("objectives must be an array of IDs");
        }
        List<String> objectives = new ArrayList<>();
        for (JsonElement objective : objectiveList.getAsJsonArray()) {
            objectives.add(resourceId(objective, "objectives"));
        }
        return new SagaQuest(id, resourceId(json.get("saga"), "saga"), integer(json, "revision"),
                resourceId(json.get("mentor"), "mentor"), optionalId(json, "prerequisite"),
                objectives, integer(json, "seals"), optionalId(json, "milestone"));
    }

    private static String optionalId(JsonObject json, String field) {
        return json.has(field) ? resourceId(json.get(field), field) : null;
    }

    private static String resourceId(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(field + " must be an ID string");
        }
        String id = value.getAsString();
        SagaQuest.requireId(id);
        return id;
    }

    private static int integer(JsonObject json, String field) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a 32-bit integer", exception);
        }
    }

    private static List<SagaQuest> withValidPrerequisites(Map<String, SagaQuest> quests) {
        Set<String> reachable = SagaCatalog.reachableQuestIds(quests);
        List<SagaQuest> valid = new ArrayList<>();
        for (SagaQuest quest : quests.values()) {
            if (reachable.contains(quest.id())) {
                valid.add(quest);
            } else if (!quests.containsKey(quest.prerequisite())) {
                LOGGER.warn("Ignoring SAGA quest {}: prerequisite {} is missing or invalid",
                        quest.id(), quest.prerequisite());
            } else {
                LOGGER.warn("Ignoring SAGA quest {}: prerequisite {} has a broken chain or cycle",
                        quest.id(), quest.prerequisite());
            }
        }
        return valid;
    }
}
