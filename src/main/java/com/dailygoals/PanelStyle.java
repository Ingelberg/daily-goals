package com.dailygoals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Small Swing building blocks shared by the sidebar's parts, so they look alike.
 */
final class PanelStyle
{
	static final Color GOOD = ColorScheme.PROGRESS_COMPLETE_COLOR;

	private PanelStyle()
	{
	}

	static JLabel smallLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	static JLabel valueLabel()
	{
		JLabel label = new JLabel();
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}

	static JLabel heading(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(color);
		return label;
	}

	/**
	 * A small label above a control.
	 */
	static JPanel field(String name, JComponent control)
	{
		JPanel field = new JPanel(new BorderLayout(0, 2));
		field.setOpaque(false);
		field.add(smallLabel(name), BorderLayout.NORTH);
		field.add(control, BorderLayout.CENTER);
		return field;
	}

	/**
	 * "Key:" on the left, value on the right.
	 */
	static JPanel statRow(JLabel key, JLabel value)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(key, BorderLayout.WEST);
		row.add(value, BorderLayout.CENTER);
		return row;
	}

	static JButton smallButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusPainted(false);
		button.setMargin(new Insets(2, 4, 2, 4));
		return button;
	}

	static JButton iconButton(ImageIcon icon, String tooltip, boolean enabled)
	{
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.setEnabled(enabled);
		button.setFocusPainted(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setPreferredSize(new Dimension(22, 18));
		return button;
	}

	/**
	 * A small filled triangle; Swing greys it out when its button is disabled.
	 */
	static ImageIcon arrowIcon(boolean up)
	{
		BufferedImage image = new BufferedImage(9, 5, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillPolygon(new int[]{0, 9, 4}, up ? new int[]{5, 5, 0} : new int[]{0, 0, 5}, 3);
		g.dispose();
		return new ImageIcon(image);
	}
}
