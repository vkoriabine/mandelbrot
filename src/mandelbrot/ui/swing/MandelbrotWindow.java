package mandelbrot.ui.swing;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import mandelbrot.Settings;
import mandelbrot.SnapshotProvider;

public class MandelbrotWindow extends JFrame {

	public static void setUp(Settings settings, SnapshotProvider snapshotProvider) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException ex) {
				}

				MandelbrotPane pane = new MandelbrotPane(settings, snapshotProvider);
				MandelbrotWindow frame = new MandelbrotWindow(pane);
				
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
	
	MandelbrotWindow(MandelbrotPane pane) {
		super("Mandelbrot");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		add(pane, BorderLayout.CENTER);
		pack();
	}
}
