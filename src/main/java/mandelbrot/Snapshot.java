package mandelbrot;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;

public class Snapshot {
	public ThreadLocal<byte[][]> calculated;
	public final BufferedImage image;
	public final BigDecimal x0;
	public final BigDecimal y0;
	public final BigDecimal x1;
	public final BigDecimal y1;

	public Snapshot(BufferedImage image, BigDecimal x0, BigDecimal y0, BigDecimal x1, BigDecimal y1) {
		this.calculated = ThreadLocal.withInitial(() -> new byte[image.getWidth()][image.getHeight()]);
		this.image = image;
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	public Snapshot(BufferedImage image, Snapshot old) {
		this.calculated = ThreadLocal.withInitial(() -> new byte[image.getWidth()][image.getHeight()]);
		this.image = image;
		this.x0 = old.x0;
		this.y0 = old.y0;
		this.x1 = old.x1;
		this.y1 = old.y1;
	}

	public byte[][] getCalculated() {
		return this.calculated.get();
	}

	public void finishCalculation() {
		this.calculated.remove();
	}
}