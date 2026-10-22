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

public class GoalScheduleTest
{
	private static final GoalSchedule SCHEDULE = new GoalSchedule(ZoneOffset.UTC, LocalTime.of(4, 0), DayOfWeek.WEDNESDAY);

	@Test
	public void dailyPeriods()
	{
		Goal goal = goal(Goal.Period.DAILY, Goal.ALL_DAYS);
		// Thursday 2026-10-22 14:00 is in the period that started at 04:00 that day
		long start = SCHEDULE.periodStart(goal, utc(2026, 10, 22, 14));
		assertEquals(utc(2026, 10, 22, 4), start);
		assertEquals(utc(2026, 10, 23, 4), SCHEDULE.nextPeriodStart(goal, start));
		assertEquals(utc(2026, 10, 21, 4), SCHEDULE.previousPeriodStart(goal, start));
		// 10 hours into a 24 hour day
		assertEquals(10 / 24.0, SCHEDULE.elapsedFraction(goal, start, utc(2026, 10, 22, 14)), 1e-9);
	}

	@Test
	public void weeklyPeriods()
	{
		Goal goal = goal(Goal.Period.WEEKLY, Goal.ALL_DAYS);
		// Thursday belongs to the week that started Wednesday 04:00
		long start = SCHEDULE.periodStart(goal, utc(2026, 10, 22, 14));
		assertEquals(utc(2026, 10, 21, 4), start);
		assertEquals(utc(2026, 10, 28, 4), SCHEDULE.nextPeriodStart(goal, start));
		// Wednesday before 04:00 is still the previous week
		assertEquals(utc(2026, 10, 14, 4), SCHEDULE.periodStart(goal, utc(2026, 10, 21, 3)));
	}

	@Test
	public void weekdaysOnly()
	{
		Goal goal = goal(Goal.Period.DAILY, 0b001_1111);
		assertTrue(SCHEDULE.isActive(goal, utc(2026, 10, 23, 4)));   // Friday
		assertFalse(SCHEDULE.isActive(goal, utc(2026, 10, 24, 4)));  // Saturday
		assertFalse(SCHEDULE.isActive(goal, utc(2026, 10, 25, 4)));  // Sunday

		// Friday to Monday skips only rest days
		assertFalse(SCHEDULE.missedBetween(goal, utc(2026, 10, 23, 4), utc(2026, 10, 26, 4)));
		// Friday to Tuesday skips Monday
		assertTrue(SCHEDULE.missedBetween(goal, utc(2026, 10, 23, 4), utc(2026, 10, 27, 4)));
	}

	@Test
	public void weekend()
	{
		assertFalse(SCHEDULE.isWeekend(utc(2026, 10, 23, 4)));  // Friday
		assertTrue(SCHEDULE.isWeekend(utc(2026, 10, 24, 4)));   // Saturday
		assertTrue(SCHEDULE.isWeekend(utc(2026, 10, 25, 4)));   // Sunday
		// Monday 02:00 is still Sunday's period with a 04:00 reset
		Goal goal = goal(Goal.Period.DAILY, Goal.ALL_DAYS);
		assertTrue(SCHEDULE.isWeekend(SCHEDULE.periodStart(goal, utc(2026, 10, 26, 2))));
	}

	@Test
	public void labels()
	{
		assertEquals("Thu 22 Oct", SCHEDULE.label(goal(Goal.Period.DAILY, Goal.ALL_DAYS), utc(2026, 10, 22, 4)));
		assertEquals("Week of 21 Oct", SCHEDULE.label(goal(Goal.Period.WEEKLY, Goal.ALL_DAYS), utc(2026, 10, 21, 4)));
	}

	private static Goal goal(Goal.Period period, int days)
	{
		return new Goal("id", Goal.Type.XP, Skill.ATTACK, false, Activity.ZULRAH, "", 100, period, days, true, true, true);
	}

	private static long utc(int year, int month, int day, int hour)
	{
		return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
	}
}
