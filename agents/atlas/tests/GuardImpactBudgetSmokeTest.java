import com.dragonballultimate.combat.GuardImpactBudget;
import com.dragonballultimate.combat.GuardImpactBudget.Result;

/** Offline deterministic smoke/property tests. No Forge installation required. */
public final class GuardImpactBudgetSmokeTest {
    private static long checks;

    private static void check(boolean passes, String context) {
        checks++;
        if (!passes) throw new AssertionError("Check " + checks + ": " + context);
    }

    private static void near(float actual, float expected, float tolerance, String context) {
        check(Math.abs(actual - expected) <= tolerance, context + " actual=" + actual
                + " expected=" + expected);
    }

    public static void main(String[] args) {
        Result ready = GuardImpactBudget.resolve(10F, 4.8F, 100);
        near(ready.damage(), 4.8F, 0.00001F, "full guard preserves legacy curve");
        check(ready.staminaSpent() == 11, "5.2 damage prevented costs 11 stamina");
        check(ready.fullGuard(), "full budget reports full guard");

        Result shortage = GuardImpactBudget.resolve(10F, 4.8F, 5);
        near(shortage.damage(), (float)(10D - (10D - 4.8F) * 5D / 11D),
                0.00001F, "partial budget prevents only a proportional amount");
        check(shortage.staminaSpent() == 5, "cannot spend more than owned");
        check(!shortage.fullGuard(), "shortage must not be classified as full guard");

        for (float damage : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1F, 0F}) {
            Result r = GuardImpactBudget.resolve(damage, 1F, 100);
            check(r.damage() == 0F && r.staminaSpent() == 0 && !r.fullGuard(),
                    "invalid input must not become immunity or corrupt state");
        }
        for (float guarded : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1F, 10F, 11F}) {
            Result r = GuardImpactBudget.resolve(10F, guarded, 100);
            check(r.damage() == 10F && r.staminaSpent() == 0 && !r.fullGuard(),
                    "malformed guard result must fail open");
        }
        Result depleted = GuardImpactBudget.resolve(10F, 4.8F, 0);
        check(depleted.damage() == 10F && depleted.staminaSpent() == 0,
                "zero stamina grants no reduction");
        Result negative = GuardImpactBudget.resolve(10F, 4.8F, -10);
        check(negative.damage() == 10F && negative.staminaSpent() == 0,
                "negative stamina grants no reduction");

        for (int d = 1; d <= 200; d++) {
            for (int percent = 0; percent <= 100; percent += 5) {
                float guarded = d * percent / 100F;
                float previousDamage = d;
                for (int stamina = 0; stamina <= 200; stamina++) {
                    Result r = GuardImpactBudget.resolve(d, guarded, stamina);
                    check(Float.isFinite(r.damage()) && r.damage() >= guarded
                            && r.damage() <= d, "damage bounds d=" + d);
                    check(r.staminaSpent() >= 0 && r.staminaSpent() <= stamina,
                            "stamina conservation d=" + d);
                    check(r.damage() <= previousDamage + 0.00001F,
                            "additional stamina never makes damage worse");
                    check(r.staminaSpent() == 0 || r.damage() < d,
                            "spent stamina must actually prevent damage");
                    if (r.fullGuard()) {
                        check(r.staminaSpent() > 0 && r.damage() <= guarded + 0.00001F,
                                "full guard preserves established reduction");
                    }
                    previousDamage = r.damage();
                }
            }
        }

        for (float incoming : new float[]{Float.MIN_VALUE, 0.000001F, 0.5F,
                Float.MAX_VALUE}) {
            Result r = GuardImpactBudget.resolve(incoming, 0F, Integer.MAX_VALUE);
            check(Float.isFinite(r.damage()) && r.damage() >= 0F && r.damage() <= incoming,
                    "finite extremes stay bounded");
            check(r.staminaSpent() >= 0, "extreme stamina never overflows");
        }

        Result identicalA = GuardImpactBudget.resolve(75F, 45F, 29);
        Result identicalB = GuardImpactBudget.resolve(75F, 45F, 29);
        check(identicalA.equals(identicalB), "result is deterministic");
        System.out.println("GuardImpactBudgetSmokeTest: " + checks + " verificacoes OK");
    }
}
