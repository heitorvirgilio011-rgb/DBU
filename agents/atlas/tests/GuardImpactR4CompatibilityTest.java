import com.dragonballultimate.combat.GuardBalance;
import com.dragonballultimate.combat.GuardImpactBudget;

/**
 * Optional cross-check against the unmodified experimental r4 GuardBalance.
 * This test intentionally does not copy r4's private source into the PR.
 */
public final class GuardImpactR4CompatibilityTest {
    private static long checks;

    private static void require(boolean ok, String caseId) {
        checks++;
        if (!ok) throw new AssertionError(caseId + " (#" + checks + ")");
    }

    public static void main(String[] args) {
        for (int maximum : new int[]{1, 20, 100, 250}) {
            for (int halfDamage = 1; halfDamage <= 120; halfDamage++) {
                float incoming = halfDamage / 2F;
                float previous = incoming;
                for (int stamina = 0; stamina <= maximum; stamina++) {
                    float legacy = GuardBalance.reducedDamage(incoming, stamina, maximum);
                    GuardImpactBudget.Result result = GuardImpactBudget.resolve(incoming,
                            legacy, stamina);
                    require(result.damage() >= legacy - 0.00001F
                                    && result.damage() <= incoming,
                            "never stronger than r4 guard, never worse than no guard");
                    require(result.staminaSpent() >= 0
                                    && result.staminaSpent() <= stamina,
                            "never spends more than server stamina");
                    require(result.damage() <= previous + 0.00001F,
                            "stamina increase cannot increase damage");
                    if (result.fullGuard()) {
                        require(Math.abs(result.damage() - legacy) <= 0.00001F,
                                "fully funded guard preserves r4 formula");
                    }
                    previous = result.damage();
                }
            }
        }
        System.out.println("GuardImpactR4CompatibilityTest: " + checks + " verificacoes OK");
    }
}
