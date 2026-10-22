package com.dailygoals;

import lombok.Value;

/**
 * A goal's numbers right now, worked out from its progress. Used by the overlay, infobox,
 * announcements and panel.
 */
@Value
class GoalStatus
{
	Goal goal;
	GoalProgress progress;
	/** The goal amount for this period before carry-over (weekend multiplier applied). */
	long amount;
	/** False on a day the goal doesn't count. */
	boolean active;
	long done;
	/** The amount plus anything missed last period. */
	long target;
	long left;
	long over;
	boolean complete;
	/** XP or count per hour this login. */
	long rate;
	/** Time to reach the goal at the current rate; -1 when unknown. */
	long etaMillis;
	/** Ahead (positive) or behind (negative) an even pace that finishes at the reset. */
	long pace;
	/** How far through the period an even pace is, 0 to 1. */
	double paceFraction;
	int streak;
	/** The weekend multiplier in percent when it applies to this period, otherwise 0. */
	int weekendPercent;

	static GoalStatus of(Goal goal, GoalProgress progress, GoalSchedule schedule, int weekendMultiplier, long rate, long now)
	{
		int weekendPercent = weekendPercent(goal, progress, schedule, weekendMultiplier);
		long amount = amount(goal, progress, weekendPercent);
		boolean active = schedule.isActive(goal, progress.day);
		long target = progress.goal(amount);
		long left = progress.left(amount);
		long eta = left > 0 && rate > 0 ? left * 3_600_000L / rate : -1;
		double paceFraction = schedule.elapsedFraction(goal, progress.day, now);
		long pace = progress.progress() - (long) (paceFraction * target);
		return new GoalStatus(goal, progress, amount, active, progress.progress(), target, left, progress.over(amount),
			progress.isDone(amount), rate, eta, pace, paceFraction, progress.currentStreak(amount, active), weekendPercent);
	}

	/**
	 * The goal amount for the progress's period. Level goals need the XP from the period start to
	 * the target level; daily goals are scaled on weekends.
	 */
	static long amount(Goal goal, GoalProgress progress, int weekendPercent)
	{
		if (goal.isLevel())
		{
			// Not seen yet: avoid reading as already reached
			return progress.baseline == null ? 1 : goal.xpToLevel(progress.baseline);
		}
		return weekendPercent == 0 ? goal.getAmount() : Math.max(1, (long) goal.getAmount() * weekendPercent / 100);
	}

	/**
	 * The weekend multiplier in percent when it applies to the progress's period, otherwise 0.
	 * It only applies to daily XP and count goals.
	 */
	static int weekendPercent(Goal goal, GoalProgress progress, GoalSchedule schedule, int weekendMultiplier)
	{
		boolean applies = weekendMultiplier != 100 && !goal.isWeekly() && !goal.isLevel() && schedule.isWeekend(progress.day);
		return applies ? weekendMultiplier : 0;
	}

	boolean isBehind()
	{
		return active && !complete && pace < 0;
	}

	/**
	 * Progress as a percentage of the target; goes past 100 when over.
	 */
	double percent()
	{
		return target <= 0 ? 100 : 100.0 * done / target;
	}
}
