package mandelbrot;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Calculator {
	private static final double INV_LOG_2 = 1.0 / Math.log(2);
	private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int CALC_THREADS = MAX_THREADS - 1;
	private static final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

	private final Settings settings;

	public Calculator(Settings settings) {
		this.settings = settings;
	}

	public void submit(Snapshot snapshot) {
		executor.submit(() -> calculate(snapshot));
	}

	void calculate(Snapshot snapshot) {
		BigDecimal widthInterval = snapshot.x1.subtract(snapshot.x0, settings.mathContext)
				.divide(BigDecimal.valueOf(settings.getScaledWidth()), settings.mathContext);
		BigDecimal heightInterval = snapshot.y1.subtract(snapshot.y0, settings.mathContext)
				.divide(BigDecimal.valueOf(settings.getScaledHeight()), settings.mathContext);
		List<BigDecimal> xPoints = new ArrayList<>();
		BigDecimal x = snapshot.x0;
		for (int i = 0; i < settings.getScaledWidth(); i++) {
			xPoints.add(x = x.add(widthInterval, settings.mathContext));
		}

		List<BigDecimal> yPoints = new ArrayList<>();
		BigDecimal y = snapshot.y0;
		for (int i = 0; i < settings.getScaledHeight(); i++) {
			yPoints.add(y = y.add(heightInterval, settings.mathContext));
		}

		int[] threadBounds = new int[CALC_THREADS + 1];
		threadBounds[0] = 0;
		threadBounds[CALC_THREADS] = settings.getScaledHeight();
		for (int t = 1; t < CALC_THREADS; t++) {
			threadBounds[t] = (int) (settings.getScaledHeight() * (t / (double) CALC_THREADS));
		}

		Collection<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < CALC_THREADS; t++) {
			int minY = threadBounds[t];
			int maxY = threadBounds[t + 1];
			futures.add(executor.submit(() -> {
				long t1 = System.currentTimeMillis();
				int[] tracePosition = { 0, minY };
				do {
					tracePosition = calculateArea(snapshot, xPoints, yPoints, minY, maxY, tracePosition);
					traceEdge(snapshot, xPoints, yPoints, minY, maxY, tracePosition);
				} while (tracePosition != null);
				snapshot.finishCalculation();
				long t2 = System.currentTimeMillis();
				// System.out.println((t2-t1) / 1000.0);
			}));
		}
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

	int[] calculateArea(Snapshot snapshot, List<BigDecimal> xPoints, List<BigDecimal> yPoints, int minY, int maxY,
			int[] start) {
		if (start == null) {
			return null;
		}

		for (int x = start[0]; x < settings.getScaledWidth(); x++) {
			for (int y = minY; y < maxY; y++) {
				if (isCalculated(snapshot, x, y)) {
					continue;
				}
				if (evalPoint(snapshot, x, y, xPoints, yPoints)) {
					return new int[] { x, y };
				}
			}
		}

		return null;
	}

	void traceEdge(Snapshot snapshot, List<BigDecimal> xPoints, List<BigDecimal> yPoints, int minY, int maxY,
			int[] start) {
		if (start == null) {
			return;
		}

		int minX = start[0];
		int x = start[0];
		int y = start[1];

		int[] down = { 0, 1 };
		int[] right = { 1, 0 };
		int[] up = { 0, -1 };
		int[] left = { -1, 0 };

		Map<int[], int[][]> decisions = new HashMap<>();
		decisions.put(down, new int[][] { left, down, right });
		decisions.put(right, new int[][] { down, right, up });
		decisions.put(up, new int[][] { right, up, left });
		decisions.put(left, new int[][] { up, left, down });

		Map<int[], int[]> opposites = new HashMap<>();
		opposites.put(down, up);
		opposites.put(up, down);
		opposites.put(right, left);
		opposites.put(left, right);

		Map<int[], int[]> externals = new HashMap<>();
		externals.put(down, left);
		externals.put(up, right);
		externals.put(right, down);
		externals.put(left, up);

		final int set = 1;
		final int ext = 2;
		int[][] mapping = new int[settings.getScaledWidth() + 2][maxY - minY + 2];

		int[] prevDir = null;
		search: do {
			int[][] choices = decisions.get(prevDir == null ? down : prevDir);
			for (int[] choice : choices) {
				if (inBounds(minX, settings.getScaledWidth(), minY, maxY, x, y, choice)) {
					if (evalPoint(snapshot, x + choice[0], y + choice[1], xPoints, yPoints)) {
						prevDir = choice;
						x += choice[0];
						y += choice[1];

						int[] external = externals.get(choice);
						mapping[x + 1][y - minY + 1] = set;
						mapping[x + external[0] + 1][y + external[1] - minY + 1] = ext;
						continue search;
					} else {
						mapping[x + choice[0] + 1][y + choice[1] - minY + 1] = ext;
					}
				}
			}
			if (prevDir != null) {
				x -= prevDir[0];
				y -= prevDir[1];
				prevDir = opposites.get(prevDir);
			}
		} while (x != start[0] || y != start[1]);

		boolean[] sawYExt = new boolean[maxY - minY];
		Arrays.fill(sawYExt, true);
		for (int i = start[0]; i < settings.getScaledWidth(); i++) {
			boolean sawXExt = true;
			for (int j = minY; j < maxY; j++) {
				int value = mapping[i + 1][j - minY + 1];
				if (value == ext) {
					sawXExt = true;
					sawYExt[j - minY] = true;
				} else if (value == set) {
					sawXExt = false;
					sawYExt[j - minY] = false;
				}
				if (!isCalculated(snapshot, i, j) && !sawXExt && !sawYExt[j - minY]) {
					snapshot.getCalculated()[i][j] = 2;
					snapshot.image.setRGB(i, j, Color.BLACK.getRGB());
				}
			}
		}
	}

	boolean inBounds(int minX, int maxX, int minY, int maxY, int x, int y, int[] direction) {
		x += direction[0];
		y += direction[1];
		if (x < minX || x >= maxX || y < minY || y >= maxY) {
			return false;
		}
		return true;
	}

	boolean isCalculated(Snapshot snapshot, int x, int y) {
		return snapshot.getCalculated()[x][y] > 0;
	}

	boolean isInSet(Snapshot snapshot, int x, int y) {
		return snapshot.getCalculated()[x][y] == 2;
	}

	boolean evalPoint(Snapshot snapshot, int xIndex, int yIndex, List<BigDecimal> xPoints, List<BigDecimal> yPoints) {
		if (settings.doublePrecision) {
			return evalPointDouble(snapshot, xIndex, yIndex, xPoints, yPoints);
		} else {
			return evalPointArbitrary(snapshot, xIndex, yIndex, xPoints, yPoints);
		}
	}

	boolean evalPointArbitrary(Snapshot snapshot, int xIndex, int yIndex, List<BigDecimal> xPoints,
			List<BigDecimal> yPoints) {
		if (isCalculated(snapshot, xIndex, yIndex)) {
			return isInSet(snapshot, xIndex, yIndex);
		}

		BigDecimal x0 = xPoints.get(xIndex);
		BigDecimal y0 = yPoints.get(yIndex);
		BigDecimal x = BigDecimal.ZERO;
		BigDecimal y = BigDecimal.ZERO;
		BigDecimal x2 = BigDecimal.ZERO;
		BigDecimal y2 = BigDecimal.ZERO;
		BigDecimal x2PlusY2 = BigDecimal.ZERO;
		int iter = 0;
		Set<BigDecimal> xSet = new HashSet<>();
		Set<BigDecimal> ySet = new HashSet<>();
		while (x2PlusY2.compareTo(settings.infThreshArbitrary) < 0 && iter < settings.maxIter) {
			y = x.add(x, settings.mathContext).multiply(y, settings.mathContext).add(y0, settings.mathContext);
			x = x2.subtract(y2, settings.mathContext).add(x0, settings.mathContext);
			x2 = x.multiply(x, settings.mathContext);
			y2 = y.multiply(y, settings.mathContext);
			x2PlusY2 = x2.add(y2, settings.mathContext);
			iter++;

			if (!xSet.add(x) && !ySet.add(y)) {
				iter = settings.maxIter;
				break;
			}
		}

		if (iter < settings.maxIter) {
			double iterAdjusted = iter + 1 - Math.log((Math.log(x2PlusY2.doubleValue()) * 0.5) * INV_LOG_2) * INV_LOG_2;
			double frac = iterAdjusted % 1;

			Color colorA = settings.iterColors[(int) Math.floor(iterAdjusted) - 1];
			Color colorB = settings.iterColors[(int) Math.floor(iterAdjusted)];
			snapshot.image.setRGB(xIndex, yIndex, Util.blend(colorA, colorB, frac).getRGB());
			snapshot.getCalculated()[xIndex][yIndex] = 1;
			return false;
		} else {
			snapshot.image.setRGB(xIndex, yIndex, Color.BLACK.getRGB());
			snapshot.getCalculated()[xIndex][yIndex] = 2;
			return true;
		}
	}

	boolean evalPointDouble(Snapshot snapshot, int xIndex, int yIndex, List<BigDecimal> xPoints,
			List<BigDecimal> yPoints) {
		if (isCalculated(snapshot, xIndex, yIndex)) {
			return isInSet(snapshot, xIndex, yIndex);
		}

		double x0 = xPoints.get(xIndex).doubleValue();
		double y0 = yPoints.get(yIndex).doubleValue();
		Double x = 0.0;
		Double y = 0.0;
		Double x2 = 0.0;
		Double y2 = 0.0;
		int iter = 0;
		Set<Double> xSet = new HashSet<>();
		Set<Double> ySet = new HashSet<>();
		while (x2 + y2 <= settings.infThreshDouble && iter < settings.maxIter) {
			y = (x + x) * y + y0;
			x = x2 - y2 + x0;
			x2 = x * x;
			y2 = y * y;
			iter++;

			if (!xSet.add(x) && !ySet.add(y)) {
				iter = settings.maxIter;
				break;
			}
		}

		if (iter < settings.maxIter) {
			double iterAdjusted = iter + 1 - Math.log((Math.log(x2 + y2) * 0.5) * INV_LOG_2) * INV_LOG_2;
			double frac = iterAdjusted % 1;

			Color colorA = settings.iterColors[(int) Math.floor(iterAdjusted) - 1];
			Color colorB = settings.iterColors[(int) Math.floor(iterAdjusted)];
			snapshot.image.setRGB(xIndex, yIndex, Util.blend(colorA, colorB, frac).getRGB());
			snapshot.getCalculated()[xIndex][yIndex] = 1;
			return false;
		} else {
			snapshot.image.setRGB(xIndex, yIndex, Color.BLACK.getRGB());
			snapshot.getCalculated()[xIndex][yIndex] = 2;
			return true;
		}
	}
}
