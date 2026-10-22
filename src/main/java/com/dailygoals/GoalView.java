package com.dailygoals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * An immutable snapshot of one goal for the sidebar. It's built on the client thread and handed
 * to the Swing thread, so the panel never touches live {@link GoalProgress}.
 */
@Value
@Builder
class GoalView
{
	static final int HISTORY_BARS = 30;

	Goal goal;
	BufferedImage icon;
	/** Bar colour, amber when behind pace. */
	Color color;
	/** False while logged out; the numbers below are then all zero. */
	boolean tracking;
	/** False on a day the goal doesn't count. */
	boolean active;
	long done;
	long target;
	long left;
	long over;
	long extra;
	long missed;
	boolean complete;
	long rate;
	/** -1 when unknown. */
	long etaMillis;
	long pace;
	/** Where an even pace is on the bar, 0-1000. */
	int pacePermille;
	int streak;
	int bestStreak;
	int weekendPercent;
	/** Oldest first, ending with the current period. */
	List<HistoryBar> history;

	/**
	 * A goal with no progress to show (logged out).
	 */
	static GoalView loggedOut(Goal goal, BufferedImage icon, Color color)
	{
		long amount = goal.isLevel() ? 0 : goal.getAmount();
		return GoalView.builder()
			.goal(goal.copy())
			.icon(icon)
			.color(color)
			.active(true)
			.target(amount)
			.left(amount)
			.etaMillis(-1)
			.history(Collections.emptyList())
			.build();
	}

	static GoalView of(GoalStatus status, BufferedImage icon, Color color, GoalSchedule schedule)
	{
		GoalProgress progress = status.getProgress();
		return GoalView.builder()
			.goal(status.getGoal().copy())
			.icon(icon)
			.color(color)
			.tracking(true)
			.active(status.isActive())
			.done(status.getDone())
			.target(status.getTarget())
			.left(status.getLeft())
			.over(status.getOver())
			.extra(progress.extra)
			.missed(progress.missed)
			.complete(status.isComplete())
			.rate(status.getRate())
			.etaMillis(status.getEtaMillis())
			.pace(status.getPace())
			.pacePermille((int) Math.round(status.getPaceFraction() * 1000))
			.streak(status.getStreak())
			.bestStreak(Math.max(progress.bestStreak, status.getStreak()))
			.weekendPercent(status.getWeekendPercent())
			.history(history(status, schedule))
			.build();
	}

	/**
	 * The last {@link #HISTORY_BARS} periods, oldest first, ending with the current one.
	 */
	private static List<HistoryBar> history(GoalStatus status, GoalSchedule schedule)
	{
		Goal goal = status.getGoal();
		GoalProgress progress = status.getProgress();
		Map<Long, GoalProgress.PeriodResult> results = new HashMap<>();
		for (GoalProgress.PeriodResult result : progress.history)
		{
			results.put(result.day, result);
		}

		List<HistoryBar> bars = new ArrayList<>();
		bars.add(new HistoryBar(status.isActive() ? HistoryBar.Kind.CURRENT : HistoryBar.Kind.REST,
			Math.min(1, status.percent() / 100), schedule.label(goal, progress.day) + " (now)"));

		long day = progress.day;
		for (int i = 1; i < HISTORY_BARS; i++)
		{
			day = schedule.previousPeriodStart(goal, day);
			String label = schedule.label(goal, day);
			GoalProgress.PeriodResult result = results.get(day);
			if (result != null)
			{
				bars.add(new HistoryBar(result.isDone() ? HistoryBar.Kind.DONE : HistoryBar.Kind.MISSED,
					result.goal <= 0 ? 1 : Math.min(1, result.done / (double) result.goal),
					label + ": " + Formats.number(result.done) + " / " + Formats.number(result.goal)));
			}
			else if (!schedule.isActive(goal, day))
			{
				bars.add(new HistoryBar(HistoryBar.Kind.REST, 0, label + ": rest day"));
			}
			else
			{
				bars.add(new HistoryBar(HistoryBar.Kind.NONE, 0, label + ": not played"));
			}
		}
		Collections.reverse(bars);
		return bars;
	}

	/**
	 * One period in the history chart.
	 */
	@Value
	static class HistoryBar
	{
		enum Kind
		{
			/** The goal didn't count that day. */
			REST,
			/** Nothing was recorded (not played). */
			NONE,
			DONE,
			MISSED,
			/** The period in progress. */
			CURRENT
		}

		Kind kind;
		/** Progress toward the goal, 0 to 1. */
		double ratio;
		/** Tooltip text. */
		String label;
	}
}
