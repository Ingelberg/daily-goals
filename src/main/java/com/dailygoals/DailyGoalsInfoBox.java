package com.dailygoals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * Shows the XP or count left for one goal (or the percent done), or how far past it you are ("+10K").
 */
class DailyGoalsInfoBox extends InfoBox
{
	private final DailyGoalsPlugin plugin;
	private final DailyGoalsConfig config;
	private final String goalId;

	/** {@link Goal#targetKey()} the image was made for, so a changed goal gets a new icon. */
	@Getter
	@Setter
	private String iconTarget;

	DailyGoalsInfoBox(BufferedImage image, DailyGoalsPlugin plugin, DailyGoalsConfig config, String goalId, String iconTarget)
	{
		super(image, plugin);
		this.plugin = plugin;
		this.config = config;
		this.goalId = goalId;
		this.iconTarget = iconTarget;
	}

	@Override
	public String getText()
	{
		GoalStatus status = plugin.status(goalId);
		if (status == null)
		{
			return null;
		}
		// The Values setting: XP / count left, or percent done
		boolean percent = config.valueFormat() == DailyGoalsConfig.ValueFormat.PERCENT;
		if (status.getOver() > 0)
		{
			return "+" + (percent ? Formats.shortPercent(status.getOver(), status.getTarget()) : Formats.shortNumber(status.getOver()));
		}
		if (status.isComplete())
		{
			return "Done";
		}
		return percent ? Formats.shortPercent(status.getDone(), status.getTarget()) : Formats.shortNumber(status.getLeft());
	}

	@Override
	public Color getTextColor()
	{
		GoalStatus status = plugin.status(goalId);
		return status != null && status.isComplete() ? Color.GREEN : Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		GoalStatus status = plugin.status(goalId);
		if (status == null)
		{
			return null;
		}

		Goal goal = status.getGoal();
		StringBuilder tooltip = new StringBuilder(goal.isWeekly() ? "Weekly goal: " : "Daily goal: ").append(goal.name())
			.append("</br>Done: ").append(Formats.number(status.getDone()))
			.append(" / ").append(Formats.number(status.getTarget()));
		if (status.getOver() > 0)
		{
			tooltip.append("</br>Over by: ").append(Formats.number(status.getOver()));
		}
		else
		{
			tooltip.append("</br>Left: ").append(Formats.number(status.getLeft()))
				.append("</br>Done in: ").append(Formats.duration(status.getEtaMillis()))
				.append("</br>Pace: ").append(Formats.pace(status.getPace()));
		}
		tooltip.append("</br>").append(goal.rateLabel()).append(": ").append(Formats.number(status.getRate()));
		if (status.getStreak() > 0)
		{
			tooltip.append("</br>Streak: ").append(status.getStreak());
		}

		GoalProgress progress = status.getProgress();
		if (progress.extra > 0)
		{
			tooltip.append("</br>Carried over: ").append(Formats.number(progress.extra));
		}
		if (progress.missed > 0)
		{
			tooltip.append("</br>Missed last time: ").append(Formats.number(progress.missed));
		}
		return tooltip.toString();
	}

	@Override
	public boolean render()
	{
		GoalStatus status = plugin.status(goalId);
		return status != null && status.isActive()
			&& !(config.hideWhenComplete().isInfoboxes() && status.isComplete());
	}

	@Override
	public String getName()
	{
		return super.getName() + goalId;
	}
}
