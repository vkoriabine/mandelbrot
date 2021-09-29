package mandelbrot;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.stream.IntStream;

public class Settings {
	public int scaleFactor = 2;
	public int zoomFactor = 8;
	public int width = 800;
	public int height = 800;

	public boolean doublePrecision = true;
	public Color uncalculatedColor = Color.GRAY;
	public MathContext mathContext = new MathContext(20, RoundingMode.HALF_UP);

	public double infThreshDouble = 1000;
	public BigDecimal infThreshArbitrary = BigDecimal.valueOf(infThreshDouble);
	public int maxIter = 10000;
	public int colorThickness = 100;
	public Color[] iterColors;
	public Color[] colors = new Color[] { Util.blend(Color.BLUE, Color.BLACK, 0.75),
			Util.blend(Color.BLUE, Color.WHITE, 0.75) };

	public Settings() {
		regenerateColors();
	}

	public int getScaledWidth() {
		return width * scaleFactor;
	}

	public int getScaledHeight() {
		return height * scaleFactor;
	}

	public void regenerateColors() {
		iterColors = IntStream.range(0, maxIter).mapToObj(x -> {
			int n = x % (colorThickness * colors.length);
			if (n % colorThickness == 0) {
				return colors[n / colorThickness];
			} else {
				Color a = colors[n / colorThickness];
				Color b = colors[((n / colorThickness) + 1) % colors.length];
				return Util.blend(a, b, (n % colorThickness) / (double) colorThickness);
			}
		}).toArray(Color[]::new);
	}
}
