package com.dragonballultimate.saga;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Checks the production JSON decoder and catalog swap with the shipped datapack files. */
public final class SagaReloadSmoke {
    private static final String NAMESPACE = "dragonballultimate:";
    private static final String LANG_PREFIX = "saga.dragonballultimate.";
    private static int checks;

    public static void main(String[] args) throws IOException {
        Path module = Path.of(args.length == 0 ? "agents/saga" : args[0]);
        Path quests = module.resolve("overlay/src/main/resources/data/dragonballultimate/saga_quests");
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        for (String path : List.of("wind_map", "fog_signals", "beacon_vigil")) {
            entries.put(new ResourceLocation(NAMESPACE + path), readJson(quests.resolve(path + ".json")));
        }
        assertThat(!SagaReload.catalog().knowsActor(NAMESPACE + "lume"), "initial catalog is empty");

        addInvalidEntries(entries);
        SagaReload reload = new SagaReload();
        reload.apply(entries, null, null);
        SagaCatalog loaded = SagaReload.catalog();
        SagaQuest wind = loaded.quest(NAMESPACE + "wind_map");
        SagaQuest fog = loaded.quest(NAMESPACE + "fog_signals");
        SagaQuest beacon = loaded.quest(NAMESPACE + "beacon_vigil");
        assertThat(wind != null && fog != null && beacon != null, "all bundled quests survived");
        assertThat(wind.prerequisite() == null && fog.prerequisite().equals(wind.id())
                && beacon.prerequisite().equals(fog.id()), "bundled prerequisite chain");
        assertThat(wind.objectives().equals(List.of(NAMESPACE + "wind_east", NAMESPACE + "wind_west")),
                "wind objectives retain their JSON order");
        assertThat(fog.objectives().equals(List.of(NAMESPACE + "fog_signal"))
                && beacon.objectives().equals(List.of(NAMESPACE + "beacon_keeper")),
                "fog and beacon use different targets");
        assertThat(wind.seals() == 1 && fog.seals() == 2 && beacon.seals() == 3, "three seal values");
        assertThat(wind.milestone() == null && fog.milestone() == null
                && (NAMESPACE + "echo_circuit_complete").equals(beacon.milestone()), "final milestone only");
        assertThat(loaded.offers(NAMESPACE + "lume").equals(List.of(beacon, fog, wind)), "deterministic offer order");
        assertThat(loaded.quest("test:independent") != null, "independent datapack entry survives");
        for (String path : List.of("primitive", "fraction", "missing", "depends_on_missing",
                "cycle_a", "cycle_b", "depends_on_cycle")) {
            assertThat(loaded.quest("test:" + path) == null, "invalid entry is isolated: " + path);
        }

        verifyTranslations(module.resolve("lang"), List.of(wind, fog, beacon));
        reload.apply(Map.of(), null, null);
        assertThat(SagaReload.catalog().quest(beacon.id()) == null, "empty reload replaces the catalog");
        assertThat(loaded.quest(beacon.id()) == beacon, "prior catalog snapshot is immutable");
        System.out.printf("SagaReloadSmoke: %d checks passed%n", checks);
    }

    private static void addInvalidEntries(Map<ResourceLocation, JsonElement> entries) {
        JsonObject base = entries.get(new ResourceLocation(NAMESPACE + "wind_map")).getAsJsonObject();
        JsonObject independent = base.deepCopy();
        independent.addProperty("mentor", "test:separate_mentor");
        entries.put(new ResourceLocation("test:independent"), independent);
        entries.put(new ResourceLocation("test:primitive"), new JsonPrimitive("invalid quest"));
        JsonObject fraction = base.deepCopy();
        fraction.addProperty("seals", 1.5);
        entries.put(new ResourceLocation("test:fraction"), fraction);
        addDependent(entries, base, "missing", "test:not_defined");
        addDependent(entries, base, "depends_on_missing", "test:missing");
        addDependent(entries, base, "cycle_a", "test:cycle_b");
        addDependent(entries, base, "cycle_b", "test:cycle_a");
        addDependent(entries, base, "depends_on_cycle", "test:cycle_a");
    }

    private static void addDependent(Map<ResourceLocation, JsonElement> entries, JsonObject original,
                                     String id, String prerequisite) {
        JsonObject json = original.deepCopy();
        json.addProperty("prerequisite", prerequisite);
        entries.put(new ResourceLocation("test:" + id), json);
    }

    private static void verifyTranslations(Path folder, List<SagaQuest> quests) throws IOException {
        JsonObject english = readJson(folder.resolve("en_us.json")).getAsJsonObject();
        JsonObject portuguese = readJson(folder.resolve("pt_br.json")).getAsJsonObject();
        assertThat(english.keySet().equals(portuguese.keySet()), "locale keys match");
        for (JsonObject language : List.of(english, portuguese)) {
            assertThat(language.has(LANG_PREFIX + "echo_circuit.title"), "localized saga title");
            assertThat(language.has(LANG_PREFIX + "actor.lume"), "localized mentor");
            for (SagaQuest quest : quests) {
                String key = LANG_PREFIX + quest.id().substring(NAMESPACE.length());
                assertThat(language.has(key + ".title") && language.has(key + ".description"),
                        "localized title and description: " + quest.id());
                for (String actor : quest.objectives()) {
                    assertThat(language.has(LANG_PREFIX + "actor." + actor.substring(NAMESPACE.length())),
                            "localized actor: " + actor);
                }
            }
            String started = translated(language, "started");
            assertThat(started.contains("%2$s") && started.contains("%6$s"),
                    "start shows the next actor and quest description");
            for (String key : List.of("advanced", "ready", "current")) {
                String value = translated(language, key);
                assertThat(value.contains("%2$s") && value.contains("%3$s") && value.contains("%4$s"),
                        "progress format names next actor and counts: " + key);
            }
            assertThat(translated(language, "completed").contains("%5$s"), "completion includes seal count");
            assertThat(!translated(language, "all_done").isBlank()
                    && !translated(language, "unavailable").isBlank(), "localized terminal statuses");
        }
    }

    private static String translated(JsonObject language, String message) {
        return language.get(LANG_PREFIX + "message." + message).getAsString();
    }

    private static JsonElement readJson(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static void assertThat(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }
}
