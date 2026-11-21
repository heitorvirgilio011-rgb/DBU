package com.dragonballultimate.saga;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.dragonballultimate.saga.SagaProgress.Outcome.*;

public final class SagaSavedDataSmoke {
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);
    private static final String FIRST = "saga_test:first";
    private static final String SECOND = "saga_test:second";
    private static final String MENTOR = "saga_test:mentor";
    private static final String ROUTE = "saga_test:route";
    private static final String BRIDGE = "saga_test:bridge";
    private static final String POST = "saga_test:post";
    private static final String MILESTONE = "saga_test:certified";
    private static final SagaCatalog CATALOG = new SagaCatalog(List.of(
            new SagaQuest(FIRST, "saga_test:trial", 1, MENTOR, null, List.of(ROUTE), 2, null),
            new SagaQuest(SECOND, "saga_test:trial", 1, MENTOR, FIRST, List.of(BRIDGE, POST), 5, MILESTONE)));
    private static int checks;

    private SagaSavedDataSmoke() {}

    public static void main(String[] args) throws IOException {
        CompoundTag empty = new CompoundTag();
        empty.putInt("version", 1);
        empty.put("players", new ListTag());
        var data = SagaSavedData.load(empty);
        check(!data.isDirty(), "an empty save starts clean");
        interact(data, ALICE, ROUTE, IGNORED);
        check(!data.isDirty(), "an ignored interaction does not dirty a save");
        interact(data, ALICE, MENTOR, STARTED);
        check(data.isDirty(), "starting marks progress dirty");
        interact(data, ALICE, ROUTE, READY);
        check(interact(data, ALICE, MENTOR, COMPLETED).receipt().milestone() == null,
                "first receipt carries seals only");
        interact(data, ALICE, MENTOR, STARTED);
        interact(data, ALICE, BRIDGE, ADVANCED);
        interact(data, BOB, MENTOR, STARTED);

        data = SagaSavedData.load(roundTrip(data.save(new CompoundTag())));
        check(!data.isDirty(), "decoded data starts clean");
        var restored = interact(data, ALICE, MENTOR, CURRENT);
        check(restored.reached() == 1 && POST.equals(restored.nextActor()), "active stage restored");
        check(!data.isDirty(), "reading the active stage does not dirty the save");
        check(interact(data, BOB, MENTOR, CURRENT).reached() == 0, "separate player progress restored");
        interact(data, ALICE, POST, READY);
        check(MILESTONE.equals(interact(data, ALICE, MENTOR, COMPLETED).receipt().milestone()),
                "second receipt includes the milestone");

        CompoundTag persisted = roundTrip(data.save(new CompoundTag()));
        data = SagaSavedData.load(persisted);
        check(data.hasMilestone(ALICE, MILESTONE), "milestone restored for its owner");
        check(!data.hasMilestone(BOB, MILESTONE), "milestone isolated to its owner");
        check(interact(data, ALICE, MENTOR, ALL_DONE).receipt() == null, "repeat has no new receipt");
        check(!data.isDirty(), "claim replay after loading leaves the save clean");
        CompoundTag alice = playerTag(data.save(new CompoundTag()), ALICE);
        check(receiptTag(alice, FIRST).getInt("seals") == 2 && !receiptTag(alice, FIRST).contains("milestone"),
                "receipt without milestone preserved across round trips");
        check(receiptTag(alice, SECOND).getInt("seals") == 5, "receipt with milestone preserved across round trips");

        CompoundTag future = persisted.copy();
        future.putInt("version", 2);
        checkDisabled(future, "a future schema");

        CompoundTag badList = persisted.copy();
        ListTag players = new ListTag();
        players.add(StringTag.valueOf("unexpected"));
        badList.put("players", players);
        checkDisabled(badList, "a list with an invalid element type");

        CompoundTag badMilestone = persisted.copy();
        receiptTag(playerTag(badMilestone, ALICE), SECOND).putInt("milestone", 4);
        checkDisabled(badMilestone, "a receipt with an invalid milestone type");

        CompoundTag duplicate = persisted.copy();
        duplicate.getList("players", Tag.TAG_COMPOUND).add(playerTag(duplicate, ALICE).copy());
        checkDisabled(duplicate, "a duplicate player record");

        System.out.println("SagaSavedDataSmoke: " + checks + " checks passed");
    }

    private static SagaProgress.Result interact(SagaSavedData data, UUID player, String actor,
                                                SagaProgress.Outcome expected) {
        var result = data.interact(player, actor, CATALOG);
        check(result.outcome() == expected, "expected " + expected + " but got " + result.outcome());
        return result;
    }

    private static CompoundTag roundTrip(CompoundTag tag) throws IOException {
        var encoded = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, encoded);
        return NbtIo.readCompressed(new ByteArrayInputStream(encoded.toByteArray()));
    }

    private static CompoundTag playerTag(CompoundTag root, UUID uuid) {
        for (Tag tag : root.getList("players", Tag.TAG_COMPOUND)) {
            var player = (CompoundTag) tag;
            if (uuid.equals(player.getUUID("uuid"))) {
                return player;
            }
        }
        throw new AssertionError("missing player " + uuid);
    }

    private static CompoundTag receiptTag(CompoundTag player, String id) {
        for (Tag tag : player.getList("quests", Tag.TAG_COMPOUND)) {
            var quest = (CompoundTag) tag;
            if (id.equals(quest.getString("id"))) {
                return quest.getCompound("receipt");
            }
        }
        throw new AssertionError("missing quest " + id);
    }

    private static void checkDisabled(CompoundTag malformed, String reason) {
        var data = SagaSavedData.load(malformed);
        interact(data, ALICE, MENTOR, UNAVAILABLE);
        check(!data.hasMilestone(ALICE, MILESTONE), reason + " cannot grant access");
        check(!data.isDirty(), reason + " does not schedule a write");
        try {
            data.save(new CompoundTag());
        } catch (IllegalStateException expected) {
            checks++;
            return;
        }
        throw new AssertionError(reason + " allowed a write");
    }

    private static void check(boolean valid, String description) {
        if (!valid) {
            throw new AssertionError(description);
        }
        checks++;
    }
}
