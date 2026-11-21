package com.dragonballultimate.client;

import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Seleção de rótulos para o HUD sem dependências do Minecraft/Forge.
 * Ajusta APENAS a apresentação; não altera Ki, liberação ou suas regras.
 */
public final class HudTextLayout {
    public static final int KI_GAP = 5;

    private HudTextLayout() {}

    public record KiLabels(String ki, String release) {}

    /**
     * Escolhe textos que caibam na mesma linha, medidos pela fonte real do cliente.
     * O Ki tem precedência; a liberação pode ser abreviada ou omitida em espaços extremos.
     * Nunca ultrapassa a largura fornecida, nem deixa os dois textos se sobreporem.
     */
    public static KiLabels fitKiLabels(int ki, int maxKi, int releasePercent, int maxRelease,
                                      int availableWidth, ToIntFunction<String> textWidth) {
        Objects.requireNonNull(textWidth, "textWidth");
        if (availableWidth <= 0) return new KiLabels("", "");

        String fullKi = "KI " + HudMath.number(ki) + "/" + HudMath.number(maxKi);
        String shortKi = "KI " + HudMath.number(ki);
        String fullRelease = "LIB " + releasePercent + "%/" + maxRelease + "%";
        String shortRelease = "L " + releasePercent + "%";
        String[] kiOptions = {fullKi, fullKi, shortKi, shortKi,
                fullKi, shortKi, "KI", "KI"};
        String[] releaseOptions = {fullRelease, shortRelease, fullRelease, shortRelease,
                "", "", shortRelease, ""};

        for (int i = 0; i < kiOptions.length; i++) {
            String left = kiOptions[i];
            String right = releaseOptions[i];
            int leftWidth = Math.max(0, textWidth.applyAsInt(left));
            int rightWidth = right.isEmpty() ? 0 : Math.max(0, textWidth.applyAsInt(right));
            long needed = (long) leftWidth + (right.isEmpty() ? 0 : KI_GAP + (long) rightWidth);
            if (needed <= availableWidth) return new KiLabels(left, right);
        }
        // O próprio texto "KI" não cabe; esconder evita escrita fora do medidor.
        return new KiLabels("", "");
    }
}
