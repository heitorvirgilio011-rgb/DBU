package com.dragonballultimate.saga;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SagaProgressSmoke {
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);
    private static final String SAGA = "saga:ridge";
    private static final String MENTOR = "saga:mentor";
    private static final String GUIDE = "saga:guide";
    private static final String SCOUT = "saga:scout";
    private static final String OBSERVER = "saga:observer";
    private static final String FIRST = "saga:ridge/survey";
    private static final String SECOND = "saga:ridge/cache";
    private static final String THIRD = "saga:ridge/return";
    private static final String MILESTONE = "saga:ridge/surveyed";
    private static int checks;

    private SagaProgressSmoke() {}

    public static void main(String[] args) {
        chainAndPlayerIsolation();
        deterministicOffers();
        reloadAndUnavailableDefinitions();
        invalidCatalogAndSave();
        System.out.printf("SagaProgressSmoke: %d checks passed%n", checks);
    }

    private static void deterministicOffers() {
        SagaQuest earliest = quest("saga:a_offer", null, List.of(OBSERVER));
        SagaQuest latest = quest("saga:z_offer", null, List.of(GUIDE));
        SagaCatalog catalog = new SagaCatalog(List.of(latest, earliest));
        SagaProgress progress = new SagaProgress();

        outcome(progress.interact(ALICE, "saga:unregistered", catalog), SagaProgress.Outcome.IGNORED);
        SagaProgress.Result result = progress.interact(ALICE, MENTOR, catalog);
        outcome(result, SagaProgress.Outcome.STARTED);
        same(earliest.id(), result.questId(), "quest IDs order multiple eligible offers");
        same(OBSERVER, result.nextActor(), "progress belongs to the selected offer");
    }

    private static void chainAndPlayerIsolation() {
        SagaCatalog catalog = new SagaCatalog(List.of(third(), second(), first(1)));
        SagaProgress progress = new SagaProgress();

        outcome(progress.interact(ALICE, GUIDE, catalog), SagaProgress.Outcome.IGNORED);
        same(0L, progress.seals(ALICE), "a stranger cannot start or pay a quest");
        check(progress.snapshot().isEmpty(), "ignored events do not create records");

        SagaProgress.Result started = progress.interact(ALICE, MENTOR, catalog);
        outcome(started, SagaProgress.Outcome.STARTED);
        same(FIRST, started.questId(), "only the quest with satisfied prerequisites starts");
        same(GUIDE, started.nextActor(), "first objective");
        same(0, started.reached(), "initial count");
        same(2, started.total(), "initial total");

        SagaProgress.Result current = progress.interact(ALICE, MENTOR, catalog);
        outcome(current, SagaProgress.Outcome.CURRENT);
        same(GUIDE, current.nextActor(), "mentor repeats the expected objective");
        outcome(progress.interact(ALICE, SCOUT, catalog), SagaProgress.Outcome.IGNORED);
        SagaProgress.Result advanced = progress.interact(ALICE, GUIDE, catalog);
        outcome(advanced, SagaProgress.Outcome.ADVANCED);
        same(SCOUT, advanced.nextActor(), "next objective");
        same(1, advanced.reached(), "progress after first objective");
        outcome(progress.interact(ALICE, GUIDE, catalog), SagaProgress.Outcome.IGNORED);

        SagaProgress.Result bobStarted = progress.interact(BOB, MENTOR, catalog);
        outcome(bobStarted, SagaProgress.Outcome.STARTED);
        same(GUIDE, bobStarted.nextActor(), "Bob starts with his own progress");
        outcome(progress.interact(BOB, SCOUT, catalog), SagaProgress.Outcome.IGNORED);
        same(0L, progress.seals(BOB), "Bob has no reward");

        SagaProgress.Result ready = progress.interact(ALICE, SCOUT, catalog);
        outcome(ready, SagaProgress.Outcome.READY);
        same(MENTOR, ready.nextActor(), "return to mentor after all objectives");
        same(2, ready.reached(), "all objectives reached");
        outcome(progress.interact(ALICE, SCOUT, catalog), SagaProgress.Outcome.IGNORED);
        same(0L, progress.seals(ALICE), "the last objective grants no receipt");
        check(!progress.hasMilestone(ALICE, MILESTONE), "the milestone waits for the mentor");

        SagaProgress.Result completed = progress.interact(ALICE, MENTOR, catalog);
        outcome(completed, SagaProgress.Outcome.COMPLETED);
        same(FIRST, completed.questId(), "return completes the active quest");
        same(new SagaProgress.Receipt(2, MILESTONE), completed.receipt(), "recorded reward");
        same(2L, progress.seals(ALICE), "the first reward is credited once");
        check(progress.hasMilestone(ALICE, MILESTONE), "milestone granted to Alice");
        check(!progress.hasMilestone(BOB, MILESTONE), "milestone is private to Alice");
        same(0L, progress.seals(BOB), "no reward leaked to Bob");
        check(!progress.snapshot().get(ALICE).containsKey(SECOND),
                "completion does not start the next quest");

        SagaProgress.Result next = progress.interact(ALICE, MENTOR, catalog);
        outcome(next, SagaProgress.Outcome.STARTED);
        same(SECOND, next.questId(), "the next quest starts on a separate interaction");
        outcome(progress.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.CURRENT);
        same(2L, progress.seals(ALICE), "repeated mentor interaction cannot duplicate reward");
        outcome(progress.interact(ALICE, OBSERVER, catalog), SagaProgress.Outcome.READY);
        outcome(progress.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.COMPLETED);
        next = progress.interact(ALICE, MENTOR, catalog);
        outcome(next, SagaProgress.Outcome.STARTED);
        same(THIRD, next.questId(), "third quest requires the second receipt");
        outcome(progress.interact(ALICE, GUIDE, catalog), SagaProgress.Outcome.READY);
        outcome(progress.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.COMPLETED);
        outcome(progress.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.ALL_DONE);
        outcome(progress.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.ALL_DONE);
        same(10L, progress.seals(ALICE), "replaying the final return preserves the total");
        same(GUIDE, progress.interact(BOB, MENTOR, catalog).nextActor(),
                "Bob remains at his first objective");
    }

    private static void reloadAndUnavailableDefinitions() {
        SagaCatalog catalog = new SagaCatalog(List.of(first(1), second(), third()));
        SagaProgress progress = new SagaProgress();
        outcome(progress.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.STARTED);
        outcome(progress.interact(ALICE, GUIDE, catalog), SagaProgress.Outcome.ADVANCED);
        Map<UUID, Map<String, SagaProgress.Entry>> saved = progress.snapshot();
        expect(UnsupportedOperationException.class, () -> saved.clear(), "snapshot is immutable");
        expect(UnsupportedOperationException.class, () -> saved.get(ALICE).clear(),
                "player records are immutable");
        expect(UnsupportedOperationException.class, () -> saved.get(ALICE).get(FIRST).reached().clear(),
                "objective history is immutable");

        SagaProgress resumed = new SagaProgress(saved);
        same(saved, resumed.snapshot(), "mid-quest state survives reconstruction");
        SagaCatalog revised = new SagaCatalog(List.of(first(2), second(), third()));
        outcome(resumed.interact(ALICE, MENTOR, revised), SagaProgress.Outcome.UNAVAILABLE);
        same(saved, resumed.snapshot(), "revision mismatch leaves the original state intact");

        SagaQuest unrelated = new SagaQuest("saga:other", SAGA, 1, MENTOR, null, List.of(GUIDE), 1, null);
        SagaCatalog missing = new SagaCatalog(List.of(unrelated));
        outcome(resumed.interact(ALICE, MENTOR, missing), SagaProgress.Outcome.UNAVAILABLE);
        outcome(resumed.interact(ALICE, MENTOR, SagaCatalog.empty()), SagaProgress.Outcome.IGNORED);
        same(saved, resumed.snapshot(), "missing definitions leave the original state intact");

        SagaProgress reordered = new SagaProgress(Map.of(ALICE, Map.of(
                FIRST, new SagaProgress.Entry(1, List.of(SCOUT), null))));
        var invalidOrder = reordered.snapshot();
        outcome(reordered.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.UNAVAILABLE);
        same(invalidOrder, reordered.snapshot(), "invalid saved objective order cannot reward or erase state");
        same(0L, reordered.seals(ALICE), "invalid saved progress cannot produce seals");

        outcome(resumed.interact(ALICE, GUIDE, catalog), SagaProgress.Outcome.IGNORED);
        outcome(resumed.interact(ALICE, SCOUT, catalog), SagaProgress.Outcome.READY);
        same(List.of(GUIDE), saved.get(ALICE).get(FIRST).reached(),
                "advancing the restored state cannot modify the old snapshot");
        SagaProgress ready = new SagaProgress(resumed.snapshot());
        outcome(ready.interact(ALICE, SCOUT, catalog), SagaProgress.Outcome.IGNORED);
        outcome(ready.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.COMPLETED);

        Map<UUID, Map<String, SagaProgress.Entry>> receipt = ready.snapshot();
        SagaProgress paid = new SagaProgress(receipt);
        same(2L, paid.seals(ALICE), "the restored receipt contributes to the total");
        check(paid.hasMilestone(ALICE, MILESTONE), "the restored receipt remembers the milestone");
        outcome(paid.interact(ALICE, MENTOR, SagaCatalog.empty()), SagaProgress.Outcome.IGNORED);
        same(receipt, paid.snapshot(), "an empty catalog cannot erase a receipt");
        check(paid.hasMilestone(ALICE, MILESTONE), "a missing quest cannot revoke a paid milestone");
        outcome(paid.interact(ALICE, MENTOR, catalog), SagaProgress.Outcome.STARTED);
        same(2L, paid.seals(ALICE), "restoring the catalog cannot replay an old reward");
    }

    private static void invalidCatalogAndSave() {
        expect(IllegalArgumentException.class, () -> quest("QUEST", null, List.of(GUIDE)),
                "a quest must have a valid namespaced ID");
        expect(IllegalArgumentException.class, () -> quest(FIRST, null, List.of("saga:bad actor")),
                "the objective actor must have a valid ID");
        expect(IllegalArgumentException.class, () -> quest(FIRST, null, List.of(GUIDE, GUIDE)),
                "an objective cannot be repeated");
        expect(IllegalArgumentException.class, () -> quest(FIRST, null, List.of(MENTOR)),
                "a mentor cannot be their own objective");
        expect(IllegalArgumentException.class, () -> new SagaCatalog(List.of(first(1), first(2))),
                "quest IDs are unique across revisions");
        expect(IllegalArgumentException.class, () -> new SagaCatalog(List.of(second())),
                "prerequisites must exist");
        expect(IllegalArgumentException.class, () -> new SagaCatalog(List.of(
                quest(FIRST, SECOND, List.of(GUIDE)), quest(SECOND, FIRST, List.of(SCOUT)))),
                "cyclic prerequisites are invalid");
        expect(IllegalArgumentException.class, () -> new SagaProgress(Map.of(ALICE, Map.of(
                FIRST, new SagaProgress.Entry(1, List.of(), null),
                SECOND, new SagaProgress.Entry(1, List.of(), null)))),
                "inconsistent save with two active quests for one player");
    }

    private static SagaQuest first(int revision) {
        return new SagaQuest(FIRST, SAGA, revision, MENTOR, null, List.of(GUIDE, SCOUT), 2, MILESTONE);
    }

    private static SagaQuest second() {
        return new SagaQuest(SECOND, SAGA, 1, MENTOR, FIRST, List.of(OBSERVER), 3, null);
    }

    private static SagaQuest third() {
        return new SagaQuest(THIRD, SAGA, 1, MENTOR, SECOND, List.of(GUIDE), 5, null);
    }

    private static SagaQuest quest(String id, String prerequisite, List<String> objectives) {
        return new SagaQuest(id, SAGA, 1, MENTOR, prerequisite, objectives, 1, null);
    }

    private static void outcome(SagaProgress.Result result, SagaProgress.Outcome expected) {
        same(expected, result.outcome(), "outcome for " + result.questId());
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action, String label) {
        try {
            action.run();
        } catch (RuntimeException error) {
            check(type.isInstance(error), label + " (got " + error + ")");
            return;
        }
        throw new AssertionError(label + " (no exception)");
    }

    private static void same(Object expected, Object actual, String label) {
        check(Objects.equals(expected, actual), label + " (expected " + expected + ", got " + actual + ")");
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
