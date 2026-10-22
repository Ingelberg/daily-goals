package com.dailygoals;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GoalStatusTest
{
	private static final GoalSchedule SCHEDULE = new GoalSchedule(ZoneOffset.UTC, LocalTime.of(4, 0), DayOfWeek.MONDAY);
	private static final long FRIDAY = utc(2026, 10, 23, 4);
	private static final long SATURDAY = utc(2026, 10, 24, 4);

	@Test
	public void weekendMultiplier()
	{
		Goal goal = goal(Goal.Type.XP, 100_000);
		assertEquals(100_000, status(goal, new GoalProgress("xp:ATTACK", FRIDAY), 150).getAmount());

		GoalStatus weekend = status(goal, new GoalProgress("xp:ATTACK", SATURDAY), 150);
		assertEquals(150_000, weekend.getAmount());
		assertEquals(150, weekend.getWeekendPercent());

		// 100% is off
		assertEquals(0, status(goal, new GoalProgress("xp:ATTACK", SATURDAY), 100).getWeekendPercent());

		// Weekly goals aren't scaled
		goal.setPeriod(Goal.Period.WEEKLY);
		assertEquals(100_000, status(goal, new GoalProgress("xp:ATTACK", SATURDAY), 150).getAmount());
	}

	@Test
	public void levelGoalNeedsXpFromPeriodStart()
	{
		Goal goal = goal(Goal.Type.LEVEL, 2);
		GoalProgress progress = new GoalProgress("level:ATTACK:2", SATURDAY);
		// Not seen yet: not reached
		assertEquals(1, status(goal, progress, 200).getAmount());

		progress.observeXp(50);
		// Level 2 is at 83 XP; the weekend multiplier doesn't apply to levels
		GoalStatus status = status(goal, progress, 200);
		assertEquals(33, status.getAmount());
		assertFalse(status.isComplete());

		progress.observeXp(90);
		assertTrue(status(goal, progress, 200).isComplete());
	}

	@Test
	public void percentAndPace()
	{
		Goal goal = goal(Goal.Type.XP, 1_000);
		GoalProgress progress = new GoalProgress("xp:ATTACK", FRIDAY);
		progress.observeXp(0);
		progress.observeXp(1_100);

		// Half way through the day
		GoalStatus status = GoalStatus.of(goal, progress, SCHEDULE, 100, 0, FRIDAY + 12 * 3_600_000L);
		assertEquals(110.0, status.percent(), 1e-9);
		assertEquals(100, status.getOver());
		assertEquals(600, status.getPace());
		assertFalse(status.isBehind());
	}

	private static GoalStatus status(Goal goal, GoalProgress progress, int weekendMultiplier)
	{
		return GoalStatus.of(goal, progress, SCHEDULE, weekendMultiplier, 0, progress.day);
	}

	private static Goal goal(Goal.Type type, int amount)
	{
		return new Goal("id", type, Skill.ATTACK, false, Activity.ZULRAH, "", amount, Goal.Period.DAILY, Goal.ALL_DAYS, true, true, true);
	}

	private static long utc(int year, int month, int day, int hour)
	{
		return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
	}
}
