package mandelbrot.ui.swing;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mandelbrot.Settings;
import mandelbrot.SnapshotProvider;

public class SettingsPane extends JPanel {

	private static final String RES_1 = "400";
	private static final String RES_2 = "800";
	private static final String RES_3 = "1024";
	private static final String RES_4 = "1440";

	private static final String SCALE_1 = "1";
	private static final String SCALE_2 = "2";
	private static final String SCALE_3 = "4";
	
	private static final int MAX_COLORS = 6;

	private final Settings settings;
	private final SnapshotProvider snapshotProvider;

	SettingsPane(Settings settings, SnapshotProvider snapshotProvider) {
		this.settings = settings;
		this.snapshotProvider = snapshotProvider;
		setLayout(new GridLayout(0, 1));
		addResolutionButtons();
		addScalingButtons();
		addPrecisionControls();
		addIterControls();
		addColorControls();
		addRepaintButton();
	}

	private void addResolutionButtons() {
		JLabel resLbl = new JLabel("Resolution");
		resLbl.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(resLbl);
		ActionListener listener = e -> {
			int res = Integer.parseInt(e.getActionCommand());
			settings.height = res;
			settings.width = res;
			SwingUtilities.getWindowAncestor(SettingsPane.this).pack();
		};
		ButtonGroup group = new ButtonGroup();
		for (String res : new String[] { RES_1, RES_2, RES_3, RES_4 }) {
			JRadioButton resBtn = new JRadioButton(res + "x" + res);
			if (res == RES_2) {
				resBtn.setSelected(true);
			}
			resBtn.setActionCommand(res);
			resBtn.addActionListener(listener);
			group.add(resBtn);
			add(resBtn);
		}
	}

	private void addScalingButtons() {
		JLabel scaleLbl = new JLabel("Smoothing");
		scaleLbl.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(scaleLbl);
		ActionListener listener = e -> {
			int scale = Integer.parseInt(e.getActionCommand());
			settings.scaleFactor = scale;
		};
		ButtonGroup group = new ButtonGroup();
		for (String scale : new String[] { SCALE_1, SCALE_2, SCALE_3 }) {
			JRadioButton scaleBtn = new JRadioButton(scale + "x");
			if (scale == SCALE_1) {
				scaleBtn.setSelected(true);
			}
			scaleBtn.setActionCommand(scale);
			scaleBtn.addActionListener(listener);
			group.add(scaleBtn);
			add(scaleBtn);
		}
	}

	private void addPrecisionControls() {
		JLabel precLabel = new JLabel("Precision");
		precLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(precLabel);

		SpinnerModel model = new SpinnerNumberModel(settings.mathContext.getPrecision(), 16, 128, 1);
		JSpinner precSpinner = new JSpinner(model);
		precSpinner.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) precSpinner.getModel()).getNumber().intValue();
			settings.mathContext = new MathContext(value, RoundingMode.HALF_UP);
		});
		precSpinner.setEnabled(!settings.doublePrecision);

		ButtonGroup group = new ButtonGroup();
		JRadioButton doublePrecBtn = new JRadioButton("double");
		doublePrecBtn.setSelected(true);
		doublePrecBtn.addActionListener(e -> {
			settings.doublePrecision = true;
			precSpinner.setEnabled(false);
		});
		group.add(doublePrecBtn);
		add(doublePrecBtn);

		JRadioButton arbitPrecBtn = new JRadioButton("arbitrary");
		arbitPrecBtn.addActionListener(e -> {
			settings.doublePrecision = false;
			precSpinner.setEnabled(true);
		});
		group.add(arbitPrecBtn);
		add(arbitPrecBtn);
		add(precSpinner);
	}

	private void addIterControls() {
		JLabel iterLabel = new JLabel("Max Iterations");
		iterLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(iterLabel);
		SpinnerModel iterModel = new SpinnerNumberModel(settings.maxIter, 1, 1000000, 1);
		JSpinner iterSpinner = new JSpinner(iterModel);
		iterSpinner.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) iterSpinner.getModel()).getNumber().intValue();
			settings.maxIter = value;
			settings.regenerateColors();
		});
		add(iterSpinner);
		JLabel threshLabel = new JLabel("Inf Threshold");
		threshLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(threshLabel);
		SpinnerModel threshModel = new SpinnerNumberModel((int) settings.infThreshDouble, 1, 1000000, 1);
		JSpinner threshSpinner = new JSpinner(threshModel);
		threshSpinner.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) threshSpinner.getModel()).getNumber().intValue();
			settings.infThreshDouble = value;
			settings.infThreshArbitrary = new BigDecimal(value);
		});
		add(threshSpinner);
	}

	private void addColorControls() {
		JLabel thickLabel = new JLabel("Color Width");
		thickLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(thickLabel);
		SpinnerModel thickModel = new SpinnerNumberModel(settings.colorThickness, 1, 1000000, 1);
		JSpinner thickSpinner = new JSpinner(thickModel);
		thickSpinner.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) thickSpinner.getModel()).getNumber().intValue();
			settings.colorThickness = value;
			settings.regenerateColors();
		});
		add(thickSpinner);

		JLabel colorLabel = new JLabel("Colors");
		colorLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
		add(colorLabel);

		List<String> colors = Arrays.stream(settings.colors)
				.map(c -> String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()))
				.collect(Collectors.toList());
		Color[] newColors = new Color[MAX_COLORS];
		System.arraycopy(settings.colors, 0, newColors, 0, settings.colors.length);
		for (int i = 0; i < MAX_COLORS; i++) {
			final int colorIndex = i;
			JTextField colorField = new JTextField();
			if (colors.size() > i) {
				colorField.setText(colors.get(i));
			}
			colorField.getDocument().addDocumentListener(new DocumentListener() {
				
				private void update() {
					String text = colorField.getText();
					if (text.matches("#?[0-9a-fA-F]{6}")) {
						newColors[colorIndex] = Color.decode(text);
					} else {
						newColors[colorIndex] = null;
					}
					List<Color> newColorsFiltered = Arrays.stream(newColors).filter(c -> c!= null).collect(Collectors.toList());
					if (newColorsFiltered.size() > 0) {
						settings.colors = newColorsFiltered.toArray(new Color[newColorsFiltered.size()]);
						settings.regenerateColors();
					}
				}
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					update();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					update();
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					update();
				}
			});
			add(colorField);
		}
	}

	private void addRepaintButton() {
		JButton button = new JButton("Redraw");
		button.addActionListener(e -> {
			snapshotProvider.repaint();
		});
		add(button);
	}
}
