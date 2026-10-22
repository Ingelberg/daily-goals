package com.dailygoals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

/**
 * One goal's on-screen box, laid out like the XP Tracker's: icon and two lines of text, with a
 * progress bar below.
 */
class DailyGoalsOverlay extends OverlayPanel
{
	// Same sizes as the XP Tracker's box
	private static final int BORDER_SIZE = 2;
	private static final int TEXT_AND_PROGRESS_BAR_GAP = 2;
	private static final int TEXT_AND_ICON_GAP = 4;
	private static final Rectangle TEXT_AND_ICON_COMPONENT_BORDER = new Rectangle(2, 1, 4, 0);
	private static final Color BAR_BACKGROUND = new Color(61, 56, 49);
	private static final Color GOOD = new Color(80, 220, 80);

	private final PanelComponent iconTextSplitPanel = new PanelComponent();
	private final DailyGoalsPlugin plugin;
	private final DailyGoalsConfig config;
	private final String goalId;

	DailyGoalsOverlay(DailyGoalsPlugin plugin, DailyGoalsConfig config, String goalId)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.goalId = goalId;
		panelComponent.setBorder(new Rectangle(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
		panelComponent.setGap(new Point(0, TEXT_AND_PROGRESS_BAR_GAP));
		iconTextSplitPanel.setBorder(TEXT_AND_ICON_COMPONENT_BORDER);
		iconTextSplitPanel.setBackgroundColor(null);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Daily Goals overlay");
		addMenuEntry(RUNELITE_OVERLAY, "Reset today", "Daily goal", e -> plugin.restartPeriod(goalId));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		GoalStatus status = plugin.status(goalId);
		BufferedImage icon = status == null ? null : plugin.getIcon(status.getGoal());
		if (status == null || icon == null || !status.isActive() || (config.hideWhenDone() && status.isComplete()))
		{
			return null;
		}

		iconTextSplitPanel.getChildren().clear();

		// RS small font, so the overlay isn't huge
		graphics.setFont(FontManager.getRunescapeSmallFont());

		final SplitComponent textSplit = SplitComponent.builder()
			.first(line(config.overlayTopLine(), status))
			.second(line(config.overlayBottomLine(), status))
			.orientation(ComponentOrientation.VERTICAL)
			.build();

		final SplitComponent iconTextSplit = SplitComponent.builder()
			.first(new ImageComponent(icon))
			.second(textSplit)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(TEXT_AND_ICON_GAP, 0))
			.build();

		iconTextSplitPanel.getChildren().add(iconTextSplit);

		final ProgressBarComponent progressBar = new ProgressBarComponent();
		progressBar.setBackgroundColor(BAR_BACKGROUND);
		progressBar.setForegroundColor(plugin.barColor(status));
		progressBar.setLeftLabel("0");
		progressBar.setRightLabel(Formats.shortNumber(status.getTarget()));
		progressBar.setValue(Math.min(100, status.percent()));

		panelComponent.getChildren().add(iconTextSplitPanel);
		panelComponent.getChildren().add(progressBar);

		return super.render(graphics);
	}

	@Override
	public String getName()
	{
		// Each goal's box keeps its own saved position
		return super.getName() + goalId;
	}

	private LineComponent line(DailyGoalsConfig.DisplayLine line, GoalStatus status)
	{
		// The Values setting: Done, Left and Pace as XP / count, or as a percentage of the goal
		boolean percent = config.valueFormat() == DailyGoalsConfig.ValueFormat.PERCENT;
		long target = status.getTarget();

		String value;
		Color color = Color.WHITE;
		switch (line)
		{
			case DONE:
				value = percent ? Formats.percent(status.getDone(), target) : Formats.number(status.getDone());
				break;
			case LEFT:
				if (status.getOver() > 0)
				{
					value = "+" + (percent ? Formats.percent(status.getOver(), target) : Formats.number(status.getOver()));
					color = GOOD;
				}
				else
				{
					value = percent ? Formats.percent(status.getLeft(), target) : Formats.number(status.getLeft());
				}
				break;
			case GOAL:
				value = Formats.number(status.getTarget());
				break;
			case RATE:
				value = Formats.number(status.getRate());
				break;
			case ETA:
				value = status.isComplete() ? "Done" : Formats.duration(status.getEtaMillis());
				break;
			case PACE:
				value = status.isComplete() ? "Done"
					: percent ? Formats.pacePercent(status.getPace(), target) : Formats.pace(status.getPace());
				color = status.isComplete() || status.getPace() >= 0 ? GOOD : DailyGoalsPlugin.BEHIND_PACE_COLOR;
				break;
			case PERCENT:
			default:
				// Past the goal it shows how far over, e.g. "+10.0%"
				double progress = status.percent();
				if (progress > 100)
				{
					value = String.format("+%.1f%%", progress - 100);
					color = GOOD;
				}
				else
				{
					value = String.format("%.1f%%", progress);
				}
				break;
		}

		return LineComponent.builder()
			.left(line.label(status.getGoal()) + ":")
			.right(value)
			.rightColor(color)
			.build();
	}
}
