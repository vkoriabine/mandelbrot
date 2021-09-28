package mandelbrot;

import java.awt.Color;
import java.math.MathContext;
import java.math.RoundingMode;

public class Settings {
	public int scaleFactor = 1;
	public int zoomFactor = 8;
	public int width = 400;
	public int height = 400;
	
	public Color uncalculatedColor = Color.GRAY;
	public MathContext mathContext = new MathContext(16, RoundingMode.HALF_UP);
	
	public int getScaledWidth() {
		return width * scaleFactor;
	}
	
	public int getScaledHeight() {
		return height * scaleFactor;
	}
}
