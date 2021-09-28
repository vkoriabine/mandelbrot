package mandelbrot;

import java.lang.reflect.InvocationTargetException;

import mandelbrot.ui.swing.MandelbrotWindow;

public class Main {
	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		Settings settings = new Settings();
		Calculator calculator = new Calculator(settings);
		SnapshotProvider snapshotProvider = new SnapshotProvider(settings, calculator);
		MandelbrotWindow.setUp(settings, snapshotProvider);
	}
}
