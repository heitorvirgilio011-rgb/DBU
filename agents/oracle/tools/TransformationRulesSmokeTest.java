import com.dragonballultimate.data.Race;
import com.dragonballultimate.data.Transformation;
import com.dragonballultimate.data.TransformationRules;

/** Matriz determinística e verificações de borda; sem bibliotecas de Minecraft/Forge. */
public final class TransformationRulesSmokeTest {
    private static long checks;
    private static void require(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError("#" + checks + " " + why);
    }

    public static void main(String[] args) {
        require(TransformationRules.hasProgression(Transformation.BASE, 0, false), "base livre");
        require(!TransformationRules.hasProgression(null, 100, true), "forma nula");
        require(!TransformationRules.eligible(null, Race.SAIYAN, 100, true), "forma nula elegivel");
        require(TransformationRules.canActivate(Transformation.BASE, null, 0, false, 0, 0), "base reset");
        require(TransformationRules.canSustain(Transformation.BASE, null, 0, false, 0), "base sustain");
        require(!TransformationRules.canActivate(Transformation.KAIOKEN, null, 100, true, 100, 100), "raca nula");
        require(!TransformationRules.canSustain(Transformation.SUPER_SAIYAN, null, 100, true, 100), "raca nula persist");
        require(Transformation.KAIOKEN.kiDrainPerSecond() == 3, "ki legado Kaioken");
        require(Transformation.SUPER_SAIYAN.kiDrainPerSecond() == 2, "ki legado SSJ");
        require(Transformation.SUPER_SAIYAN.requiredLevel() == 8, "nivel legado SSJ");

        for (Race race : Race.values()) {
            for (Transformation form : Transformation.values()) {
                for (int level : new int[]{-1, 0, 1, 2, 3, 7, 8, 30, 100}) {
                    for (boolean unlocked : new boolean[]{false, true}) {
                        boolean progress = form == Transformation.BASE
                                || level >= form.requiredLevel() || unlocked;
                        boolean eligible = progress && (form == Transformation.BASE
                                || !form.saiyanOnly() || race.isSaiyanLineage());
                        require(TransformationRules.hasProgression(form, level, unlocked) == progress,
                                "progress " + race + "/" + form + "/" + level);
                        require(TransformationRules.eligible(form, race, level, unlocked) == eligible,
                                "race " + race + "/" + form + "/" + level);
                        for (int release : new int[]{-100, 0, 4, 5, 6, 100}) {
                            for (int ki : new int[]{-1, 0, 1, 2, 3, 4, 5, 6, 100}) {
                                boolean expected = eligible && (form == Transformation.BASE
                                        || (release >= 5 && ki >= 2 * form.kiDrainPerSecond()));
                                require(TransformationRules.canActivate(form, race, level, unlocked, release, ki)
                                        == expected, "activate " + race + "/" + form + "/" + level
                                        + " release=" + release + " ki=" + ki);
                            }
                        }
                        for (int ki : new int[]{-1, 0, 1, 2, 3, 4, 100}) {
                            boolean expected = eligible && (form == Transformation.BASE
                                    || ki >= form.kiDrainPerSecond());
                            require(TransformationRules.canSustain(form, race, level, unlocked, ki)
                                    == expected, "sustain " + race + "/" + form + " ki=" + ki);
                        }
                    }
                }
            }
        }
        // Unlock não deve permitir SSJ para outras raças.
        for (Race race : Race.values()) {
            if (!race.isSaiyanLineage()) {
                require(!TransformationRules.canActivate(Transformation.SUPER_SAIYAN,
                        race, 100, true, 100, Integer.MAX_VALUE), "unlock racial " + race);
            }
        }
        require(!TransformationRules.canActivate(Transformation.KAIOKEN, Race.HUMAN,
                3, false, 4, 100), "liberação abaixo de 5");
        require(TransformationRules.canActivate(Transformation.KAIOKEN, Race.HUMAN,
                3, false, 5, 6), "limite exato ativacao Kaioken");
        require(!TransformationRules.canActivate(Transformation.KAIOKEN, Race.HUMAN,
                3, false, 5, 5), "abaixo limite de Ki Kaioken");
        require(TransformationRules.canSustain(Transformation.SUPER_SAIYAN, Race.HALF_SAIYAN,
                8, false, 2), "limite manutencao SSJ");
        require(!TransformationRules.canSustain(Transformation.SUPER_SAIYAN, Race.HALF_SAIYAN,
                8, false, 1), "SSJ sem Ki");
        System.out.println("TransformationRulesSmokeTest: OK (" + checks + " verificações)");
    }
}
