package mandelbrot.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.Timer;

import mandelbrot.Settings;
import mandelbrot.Snapshot;
import mandelbrot.SnapshotProvider;

public class MandelbrotPane extends JPanel {

	private final Settings settings;

	private final SnapshotProvider snapshotProvider;

	private int mouseX;

	private int mouseY;

	private boolean showMouse;

	MandelbrotPane(Settings settings, SnapshotProvider snapshotProvider) {
		this.settings = settings;
		this.snapshotProvider = snapshotProvider;
		addMouseMotionListener(new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				showMouse = true;
				mouseX = e.getX();
				mouseY = e.getY();
			}
		});

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				showMouse = false;
				repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					snapshotProvider.zoomIn(mouseX * settings.scaleFactor, mouseY * settings.scaleFactor);
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					snapshotProvider.zoomOut();
				}
				repaint();
			}
		});

		new Timer(50, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		}).start();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(settings.width, settings.height);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Snapshot currentSnapshot = snapshotProvider.getSnapshot();
		if (currentSnapshot != null) {
			g.drawImage(currentSnapshot.image.getScaledInstance(settings.width, settings.height, Image.SCALE_SMOOTH), 0,
					0, null);
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, settings.width, settings.height);
		}

		if (showMouse) {
			g.setColor(Color.WHITE);
			g.drawRect(mouseX - (settings.width / (2 * settings.zoomFactor)),
					mouseY - (settings.height / (2 * settings.zoomFactor)), settings.width / settings.zoomFactor,
					settings.height / settings.zoomFactor);
		}
	}

}
