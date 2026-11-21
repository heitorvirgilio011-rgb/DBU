import com.dragonballultimate.client.HudTextLayout;
import java.util.Random;
import java.util.function.ToIntFunction;

/** Regressão determinística de Ki/Liberação sem Forge ou inicialização do cliente. */
public final class HudTextLayoutSmokeTest {
    private static int assertions;
    private static final ToIntFunction<String> MONO = s -> s.length() * 6;
    private static final ToIntFunction<String> VARIABLE = s -> {
        int pixels = 0;
        for (int i = 0; i < s.length(); i++)
            pixels += "1il .".indexOf(s.charAt(i)) >= 0 ? 3 : 7;
        return pixels;
    };

    private static void verify(boolean result, String description) {
        if (!result) throw new AssertionError("#" + assertions + ": " + description);
        assertions++;
    }

    private static void checkFits(HudTextLayout.KiLabels labels, int width,
                                  ToIntFunction<String> font) {
        int kiWidth = font.applyAsInt(labels.ki());
        int releaseWidth = font.applyAsInt(labels.release());
        verify(kiWidth + (labels.release().isEmpty() ? 0 :
                HudTextLayout.KI_GAP + releaseWidth) <= width, "overlap or clipping");
        verify(labels.ki().isEmpty() || labels.ki().startsWith("KI"), "Ki anchor");
        verify(labels.release().isEmpty() || labels.release().startsWith("L ")
                || labels.release().startsWith("LIB "), "release anchor");
    }

    public static void main(String[] args) {
        HudTextLayout.KiLabels full = HudTextLayout.fitKiLabels(100, 200, 50, 75, 500, MONO);
        verify(full.ki().equals("KI 100/200"), "Ki full format");
        verify(full.release().equals("LIB 50%/75%"), "release full format");
        HudTextLayout.KiLabels none = HudTextLayout.fitKiLabels(100, 200, 50, 75, 0, MONO);
        verify(none.ki().isEmpty() && none.release().isEmpty(), "zero width");
        verify(HudTextLayout.fitKiLabels(100, 200, 50, 75, 5, MONO).ki().isEmpty(),
                "too narrow for KI");
        for (ToIntFunction<String> font : java.util.List.of(MONO, VARIABLE)) {
            for (int width = 0; width <= 390; width++) {
                HudTextLayout.KiLabels labels = HudTextLayout.fitKiLabels(
                        Integer.MAX_VALUE, Integer.MAX_VALUE, 100, 100, width, font);
                checkFits(labels, width, font);
            }
        }
        Random random = new Random(0xDB0F21L);
        for (int i = 0; i < 12000; i++) {
            int ki = random.nextInt(Integer.MAX_VALUE);
            int maxKi = random.nextInt(Integer.MAX_VALUE);
            int release = random.nextInt(101);
            int maxRelease = random.nextInt(101);
            int width = random.nextInt(391);
            ToIntFunction<String> font = i % 2 == 0 ? MONO : VARIABLE;
            checkFits(HudTextLayout.fitKiLabels(ki, maxKi, release, maxRelease, width, font),
                    width, font);
        }
        // A barra integrada nunca duplica medidores: somente strings para o HUD já existente.
        System.out.println("HudTextLayoutSmokeTest: OK (" + assertions + " verificações)");
    }
}
