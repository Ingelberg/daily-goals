package com.dailygoals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;

/**
 * A small bar chart of a goal's recent periods: green reached, red missed, the goal's colour for
 * the current period, a grey line for not played and a dot for rest days. Hover for details.
 */
class HistoryChart extends JComponent
{
	private static final int HEIGHT = 22;
	private static final Color MISSED = new Color(150, 60, 60);

	private List<GoalView.HistoryBar> bars = Collections.emptyList();
	private Color currentColor = Color.WHITE;

	HistoryChart()
	{
		setPreferredSize(new Dimension(100, HEIGHT));
		// Turns tooltips on; the text comes from getToolTipText(MouseEvent)
		setToolTipText("");
	}

	void setBars(List<GoalView.HistoryBar> bars, Color currentColor)
	{
		if (!bars.equals(this.bars) || !currentColor.equals(this.currentColor))
		{
			this.bars = bars;
			this.currentColor = currentColor;
			repaint();
		}
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		if (bars.isEmpty())
		{
			return null;
		}
		int index = event.getX() * bars.size() / Math.max(1, getWidth());
		return index >= 0 && index < bars.size() ? bars.get(index).getLabel() : null;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		int count = bars.size();
		if (count == 0)
		{
			return;
		}

		int width = getWidth();
		int height = getHeight();
		g.setColor(ColorScheme.DARK_GRAY_COLOR);
		g.fillRect(0, 0, width, height);

		for (int i = 0; i < count; i++)
		{
			GoalView.HistoryBar bar = bars.get(i);
			int x = i * width / count;
			int w = Math.max(1, (i + 1) * width / count - x - 1);
			switch (bar.getKind())
			{
				case REST:
					g.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
					g.fillRect(x + w / 2, height - 3, 1, 1);
					break;
				case NONE:
					g.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
					g.fillRect(x, height - 2, w, 2);
					break;
				default:
					g.setColor(barColor(bar.getKind()));
					int h = Math.max(2, (int) Math.round(bar.getRatio() * (height - 2)));
					g.fillRect(x, height - h, w, h);
					break;
			}
		}
	}

	private Color barColor(GoalView.HistoryBar.Kind kind)
	{
		switch (kind)
		{
			case DONE:
				return PanelStyle.GOOD;
			case MISSED:
				return MISSED;
			default:
				return currentColor;
		}
	}
}
