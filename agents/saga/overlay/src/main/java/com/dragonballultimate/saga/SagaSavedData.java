package com.dragonballultimate.saga;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class SagaSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "dbu_saga";
    private static final int VERSION = 1;
    private final SagaProgress progress;

    private SagaSavedData(SagaProgress progress) {
        this.progress = progress;
    }

    static SagaSavedData forServer(MinecraftServer server) {
        var storage = server.overworld().getDataStorage();
        SagaSavedData existing = storage.get(SagaSavedData::load, DATA_NAME);
        if (existing != null) {
            return existing;
        }

        // DimensionDataStorage returns null for absent files and for read failures.
        var file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(DATA_NAME + ".dat");
        SagaSavedData created;
        if (Files.notExists(file)) {
            created = new SagaSavedData(new SagaProgress());
        } else {
            LOGGER.error("SAGA progress disabled: could not read or check {}. The file is preserved.", file);
            created = new SagaSavedData(null);
        }
        storage.set(DATA_NAME, created);
        return created;
    }

    static SagaSavedData load(CompoundTag root) {
        try {
            if (!root.contains("version", Tag.TAG_INT) || root.getInt("version") != VERSION) {
                throw new IllegalArgumentException("missing or unsupported version");
            }
            Map<UUID, Map<String, SagaProgress.Entry>> players = new HashMap<>();
            for (Tag playerTag : requireList(root, "players", Tag.TAG_COMPOUND)) {
                CompoundTag player = (CompoundTag) playerTag;
                if (!player.hasUUID("uuid")) {
                    throw new IllegalArgumentException("invalid player UUID");
                }
                Map<String, SagaProgress.Entry> quests = new HashMap<>();
                for (Tag questTag : requireList(player, "quests", Tag.TAG_COMPOUND)) {
                    CompoundTag quest = (CompoundTag) questTag;
                    String id = requireId(quest, "id");
                    SagaProgress.Entry prior = quests.putIfAbsent(id, readEntry(quest));
                    if (prior != null) {
                        throw new IllegalArgumentException("duplicate quest ID for player");
                    }
                }
                if (players.putIfAbsent(player.getUUID("uuid"), quests) != null) {
                    throw new IllegalArgumentException("duplicate player UUID");
                }
            }
            return new SagaSavedData(new SagaProgress(players));
        } catch (IllegalArgumentException failure) {
            LOGGER.error("SAGA progress disabled: {}. The file is preserved.", failure.getMessage());
            return new SagaSavedData(null);
        }
    }

    private static SagaProgress.Entry readEntry(CompoundTag quest) {
        int revision = requirePositiveInt(quest, "revision");
        ListTag reached = requireList(quest, "reached", Tag.TAG_STRING);
        List<String> objectives = new ArrayList<>(reached.size());
        for (int index = 0; index < reached.size(); index++) {
            objectives.add(requireId(reached.getString(index), "reached"));
        }

        SagaProgress.Receipt receipt = null;
        if (quest.contains("receipt")) {
            if (!quest.contains("receipt", Tag.TAG_COMPOUND)) {
                throw new IllegalArgumentException("invalid receipt");
            }
            CompoundTag stored = quest.getCompound("receipt");
            String milestone = stored.contains("milestone") ? requireId(stored, "milestone") : null;
            receipt = new SagaProgress.Receipt(requirePositiveInt(stored, "seals"), milestone);
        }
        return new SagaProgress.Entry(revision, objectives, receipt);
    }

    private static ListTag requireList(CompoundTag tag, String key, int elementType) {
        if (!(tag.get(key) instanceof ListTag list) || (!list.isEmpty() && list.getElementType() != elementType)) {
            throw new IllegalArgumentException("invalid " + key + " list");
        }
        return list;
    }

    private static int requirePositiveInt(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_INT) || tag.getInt(key) < 1) {
            throw new IllegalArgumentException("invalid " + key);
        }
        return tag.getInt(key);
    }

    private static String requireId(CompoundTag tag, String key) {
        return requireId(tag.getString(key), key);
    }

    private static String requireId(String value, String key) {
        try {
            SagaQuest.requireId(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("invalid " + key + " ID");
        }
        return value;
    }

    SagaProgress.Result interact(UUID player, String actor, SagaCatalog catalog) {
        if (progress == null) {
            return new SagaProgress.Result(SagaProgress.Outcome.UNAVAILABLE, null, null, 0, 0, null);
        }

        SagaProgress.Result result = progress.interact(player, actor, catalog);
        switch (result.outcome()) {
            case STARTED, ADVANCED, READY, COMPLETED -> setDirty();
            default -> { }
        }
        return result;
    }

    boolean hasMilestone(UUID player, String id) {
        return progress != null && progress.hasMilestone(player, id);
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        if (progress == null) {
            throw new IllegalStateException("SAGA progress is disabled because the save could not be read");
        }

        ListTag players = new ListTag();
        progress.snapshot().forEach((uuid, entries) -> {
            CompoundTag player = new CompoundTag();
            player.putUUID("uuid", uuid);
            ListTag quests = new ListTag();
            entries.forEach((id, state) -> quests.add(writeEntry(id, state)));
            player.put("quests", quests);
            players.add(player);
        });
        root.putInt("version", VERSION);
        root.put("players", players);
        return root;
    }

    private static CompoundTag writeEntry(String id, SagaProgress.Entry state) {
        CompoundTag quest = new CompoundTag();
        quest.putString("id", id);
        quest.putInt("revision", state.revision());
        ListTag reached = new ListTag();
        state.reached().forEach(objective -> reached.add(StringTag.valueOf(objective)));
        quest.put("reached", reached);
        if (state.receipt() != null) {
            CompoundTag receipt = new CompoundTag();
            receipt.putInt("seals", state.receipt().seals());
            if (state.receipt().milestone() != null) {
                receipt.putString("milestone", state.receipt().milestone());
            }
            quest.put("receipt", receipt);
        }
        return quest;
    }
}
