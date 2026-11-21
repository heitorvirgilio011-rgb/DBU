package com.dragonballultimate.combat;

/**
 * Optional, pure server-side policy for charging stamina per guarded impact.
 *
 * <p>This proposal does not replace GuardBalance. The caller supplies the
 * already-computed post-guard damage from GuardBalance.reducedDamage, so the
 * established guard reduction curve is preserved when stamina suffices.
 * No Forge, saves, networking, control bindings or resource-pack dependency.
 * EDITH must approve this policy before gameplay integration.
 */
public final class GuardImpactBudget {
    /** Experimental tuning parameter: stamina points per one damage prevented. */
    public static final double STAMINA_PER_PREVENTED_DAMAGE = 2.0D;

    private GuardImpactBudget() {}

    /**
     * @param incoming damage after existing race/class reduction, before guard
     * @param fullyGuarded damage computed by the existing GuardBalance helper
     * @param availableStamina current authoritative server-side stamina
     * @return bounded damage and the stamina to debit atomically in the caller
     */
    public static Result resolve(float incoming, float fullyGuarded, int availableStamina) {
        // An invalid upstream damage amount must never propagate NaN/Infinity.
        if (!Float.isFinite(incoming) || incoming <= 0F) return new Result(0F, 0, false);
        // Fail open if the proposed guarding value is malformed: no free immunity.
        if (!Float.isFinite(fullyGuarded) || fullyGuarded < 0F
                || fullyGuarded >= incoming || availableStamina <= 0) {
            return new Result(incoming, 0, false);
        }

        double prevented = (double) incoming - fullyGuarded;
        double rawCost = Math.ceil(prevented * STAMINA_PER_PREVENTED_DAMAGE);
        int needed = rawCost >= Integer.MAX_VALUE ? Integer.MAX_VALUE
                : Math.max(1, (int) rawCost);
        int spent = Math.min(availableStamina, needed);
        double blocked = prevented * ((double) spent / needed);
        float remaining = (float) Math.max(fullyGuarded,
                Math.min((double) incoming, (double) incoming - blocked));

        // A tiny reduction that rounds away in float arithmetic costs nothing.
        if (remaining >= incoming) return new Result(incoming, 0, false);
        return new Result(remaining, spent, spent == needed);
    }

    /** Immutable, transient decision; it is never written to player NBT. */
    public record Result(float damage, int staminaSpent, boolean fullGuard) {}
}
