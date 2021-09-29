package mandelbrot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;

public class SnapshotProvider {

	private static final double START_X0 = -2.5;
	private static final double START_Y0 = -2;
	private static final double START_X1 = 1.5;
	private static final double START_Y1 = 2;

	private final Settings settings;

	private final Calculator calculator;

	private Snapshot currentSnapshot;

	private Deque<Snapshot> prevSnapshots = new ArrayDeque<>();

	public SnapshotProvider(Settings settings, Calculator calculator) {
		this.settings = settings;
		this.calculator = calculator;
		zoomIn(-1, -1);
	}

	public void repaint() {
		if (currentSnapshot != null) {
			currentSnapshot = new Snapshot(newImage(), currentSnapshot);
			calculator.submit(currentSnapshot);
		}
	}

	public void zoomIn(int x, int y) {
		BufferedImage image = newImage();
		BigDecimal x0;
		BigDecimal y0;
		BigDecimal x1;
		BigDecimal y1;
		if (currentSnapshot != null) {
			prevSnapshots.push(currentSnapshot);
			double xRatio = x / (double) (settings.width * settings.scaleFactor);
			double yRatio = y / (double) (settings.height * settings.scaleFactor);
			BigDecimal width = currentSnapshot.x1.subtract(currentSnapshot.x0, settings.mathContext);
			BigDecimal height = currentSnapshot.y1.subtract(currentSnapshot.y0, settings.mathContext);
			x0 = currentSnapshot.x0.add(width.multiply(BigDecimal.valueOf(xRatio), settings.mathContext))
					.subtract(width.divide(BigDecimal.valueOf(settings.zoomFactor * 2), settings.mathContext));
			y0 = currentSnapshot.y0.add(height.multiply(BigDecimal.valueOf(yRatio), settings.mathContext))
					.subtract(height.divide(BigDecimal.valueOf(settings.zoomFactor * 2), settings.mathContext));
			x1 = currentSnapshot.x0.add(width.multiply(BigDecimal.valueOf(xRatio), settings.mathContext))
					.add(width.divide(BigDecimal.valueOf(settings.zoomFactor * 2), settings.mathContext));
			y1 = currentSnapshot.y0.add(height.multiply(BigDecimal.valueOf(yRatio), settings.mathContext))
					.add(height.divide(BigDecimal.valueOf(settings.zoomFactor * 2), settings.mathContext));
		} else {
			x0 = BigDecimal.valueOf(START_X0);
			y0 = BigDecimal.valueOf(START_Y0);
			x1 = BigDecimal.valueOf(START_X1);
			y1 = BigDecimal.valueOf(START_Y1);
		}
		
		currentSnapshot = new Snapshot(image, x0, y0, x1, y1);
		calculator.submit(currentSnapshot);
	}

	public void zoomOut() {
		if (!prevSnapshots.isEmpty()) {
			currentSnapshot = prevSnapshots.pop();
		}
	}

	public Snapshot getSnapshot() {
		return currentSnapshot;
	}

	private BufferedImage newImage() {
		BufferedImage image = new BufferedImage(settings.width * settings.scaleFactor,
				settings.height * settings.scaleFactor, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setPaint(settings.uncalculatedColor);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		return image;
	}
}
