package com.dragonballultimate.data;

/**
 * Regras puras de autorização de formas (servidor é a autoridade).
 * Não persiste estado, não altera IDs legados, networking, HUD ou controles.
 * Proposta isolada para revisão de EDITH; valores vêm de Transformation.
 */
public final class TransformationRules {
    private static final int MIN_RELEASE = 5;

    private TransformationRules() {}

    /** Unlock explícito contorna SOMENTE requisito de nível, nunca requisito racial. */
    public static boolean hasProgression(Transformation form, int level, boolean unlocked) {
        return form != null && (form == Transformation.BASE
                || level >= form.requiredLevel() || unlocked);
    }

    public static boolean eligible(Transformation form, Race race, int level, boolean unlocked) {
        if (form == null) return false;
        if (form == Transformation.BASE) return true;
        return race != null && (!form.saiyanOnly() || race.isSaiyanLineage())
                && hasProgression(form, level, unlocked);
    }

    /** A ativação não consome Ki; o gasto permanece no tick do servidor. */
    public static boolean canActivate(Transformation form, Race race, int level,
                                      boolean unlocked, int releasePercent, int ki) {
        return eligible(form, race, level, unlocked)
                && (form == Transformation.BASE || (releasePercent >= MIN_RELEASE
                && (long) ki >= 2L * form.kiDrainPerSecond()));
    }

    /** Checagem da manutenção; sem mudar a regra histórica de gasto por segundo. */
    public static boolean canSustain(Transformation form, Race race, int level,
                                     boolean unlocked, int ki) {
        return eligible(form, race, level, unlocked)
                && (form == Transformation.BASE || ki >= form.kiDrainPerSecond());
    }
}
