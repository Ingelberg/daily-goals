package com.dailygoals;

import java.util.ArrayList;
import java.util.List;

/**
 * One account's progress on a goal for the current period (day or week), plus its streak and
 * history. Stored as JSON per RuneScape profile, so field names must not change.
 */
class GoalProgress
{
	static final int HISTORY_SIZE = 60;

	/** {@link Goal#targetKey()} this progress belongs to; when the goal changes, progress starts over. */
	String target;
	/** Epoch millis of the reset that started the current period. */
	long day;
	/** XP or count when the period started; null until first seen. */
	Long baseline;
	/** Gained since the period started. */
	long gained;
	/** Progress carried in from the previous period. */
	long extra;
	/** Unfinished amount carried in from the previous period, added to the goal. */
	long missed;
	/** Whether this period's completion has been announced. */
	boolean completed;
	/** Highest milestone percentage (50, 75) announced this period. */
	int milestone;
	/** Finished periods in a row that reached the goal. */
	int streak;
	int bestStreak;
	/** Finished periods the goal counted in, oldest first. */
	List<PeriodResult> history = new ArrayList<>();

	/**
	 * How one finished period went.
	 */
	static class PeriodResult
	{
		long day;
		long done;
		long goal;

		PeriodResult()
		{
		}

		PeriodResult(long day, long done, long goal)
		{
			this.day = day;
			this.done = done;
			this.goal = goal;
		}

		boolean isDone()
		{
			return done >= goal;
		}
	}

	// For Gson, so the field defaults above apply to older saves
	GoalProgress()
	{
	}

	GoalProgress(String target, long day)
	{
		this.target = target;
		this.day = day;
	}

	long progress()
	{
		return gained + extra;
	}

	long goal(long amount)
	{
		return amount + missed;
	}

	long left(long amount)
	{
		return Math.max(0, goal(amount) - progress());
	}

	/**
	 * How far past the goal progress is.
	 */
	long over(long amount)
	{
		return Math.max(0, progress() - goal(amount));
	}

	boolean isDone(long amount)
	{
		return progress() >= goal(amount);
	}

	/**
	 * The streak including this period, once this period is done.
	 */
	int currentStreak(long amount, boolean active)
	{
		return streak + (active && isDone(amount) ? 1 : 0);
	}

	/**
	 * Record the current XP in the goal's skill.
	 */
	void observeXp(long xp)
	{
		if (baseline == null)
		{
			baseline = xp;
		}
		gained = Math.max(0, xp - baseline);
	}

	/**
	 * Record a running total from a game message. The first one seen this period counts as one.
	 */
	void observeTotal(long total)
	{
		if (baseline == null)
		{
			baseline = total - 1;
		}
		gained = Math.max(gained, total - baseline);
	}

	/**
	 * Count something whose message has no running total.
	 */
	void add(long count)
	{
		gained += count;
	}

	/**
	 * Start this period's count over from the next value seen. The streak and history stay.
	 */
	void restartPeriod()
	{
		baseline = null;
		gained = 0;
		completed = false;
		milestone = 0;
	}

	/**
	 * Start a new period. The next baseline is the last value seen, so anything gained while the
	 * client was closed counts toward the new period.
	 *
	 * @param amount      the goal amount for the period that just ended
	 * @param endedActive whether the goal counted in the period that ended; rest days pass any
	 *                    carry-over on unchanged and don't touch the streak
	 * @param skipped     whether a period the goal counts in was skipped entirely, which ends the streak
	 */
	void rollover(long newDay, long amount, DailyGoalsConfig.CarryOver carryOver, boolean endedActive, boolean skipped)
	{
		if (endedActive)
		{
			long target = goal(amount);
			history.add(new PeriodResult(day, progress(), target));
			while (history.size() > HISTORY_SIZE)
			{
				history.remove(0);
			}

			streak = progress() >= target ? streak + 1 : 0;

			long leftover = progress() - target;
			extra = carryOver.isCarryExtra() && leftover > 0 ? leftover : 0;
			missed = carryOver.isCarryMissed() && leftover < 0 ? -leftover : 0;
		}
		bestStreak = Math.max(bestStreak, streak);
		if (skipped)
		{
			streak = 0;
		}

		if (baseline != null)
		{
			baseline += gained;
		}
		gained = 0;
		completed = false;
		milestone = 0;
		day = newDay;
	}
}
