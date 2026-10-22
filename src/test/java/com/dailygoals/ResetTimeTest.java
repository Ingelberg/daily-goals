package com.dailygoals;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ResetTimeTest
{
	private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

	@Test
	public void afterResetTimeIsToday()
	{
		ZonedDateTime now = ZonedDateTime.of(2026, 10, 22, 14, 0, 0, 0, ZoneOffset.UTC);
		assertEquals(utc(2026, 10, 22, 6, 0), GoalSchedule.lastDailyReset(now, LocalTime.of(6, 0)));
	}

	@Test
	public void beforeResetTimeIsYesterday()
	{
		ZonedDateTime now = ZonedDateTime.of(2026, 10, 22, 5, 59, 0, 0, ZoneOffset.UTC);
		assertEquals(utc(2026, 10, 21, 6, 0), GoalSchedule.lastDailyReset(now, LocalTime.of(6, 0)));
	}

	@Test
	public void exactlyAtResetTimeIsToday()
	{
		ZonedDateTime now = ZonedDateTime.of(2026, 10, 22, 0, 0, 0, 0, ZoneOffset.UTC);
		assertEquals(utc(2026, 10, 22, 0, 0), GoalSchedule.lastDailyReset(now, LocalTime.MIDNIGHT));
	}

	@Test
	public void localTimeZone()
	{
		// 01:30 in Stockholm (UTC+2 in October before the switch) is after a 01:00 local reset
		ZonedDateTime now = ZonedDateTime.of(2026, 10, 22, 1, 30, 0, 0, STOCKHOLM);
		assertEquals(utc(2026, 10, 21, 23, 0), GoalSchedule.lastDailyReset(now, LocalTime.of(1, 0)));
	}

	@Test
	public void dstGapMovesForward()
	{
		// 02:30 doesn't exist in Stockholm on 2026-03-29; the reset falls at 03:30 local = 01:30 UTC
		ZonedDateTime now = ZonedDateTime.of(2026, 3, 29, 12, 0, 0, 0, STOCKHOLM);
		assertEquals(utc(2026, 3, 29, 1, 30), GoalSchedule.lastDailyReset(now, LocalTime.of(2, 30)));
	}

	@Test
	public void parseTime()
	{
		assertEquals(LocalTime.of(6, 30), GoalSchedule.parseTime("06:30"));
		assertEquals(LocalTime.of(6, 30), GoalSchedule.parseTime(" 6.30 "));
		assertEquals(LocalTime.of(6, 30), GoalSchedule.parseTime("0630"));
		assertEquals(LocalTime.of(18, 0), GoalSchedule.parseTime("18"));
		assertEquals(LocalTime.of(23, 59), GoalSchedule.parseTime("23:59"));
		assertEquals(LocalTime.MIDNIGHT, GoalSchedule.parseTime("24:00"));
		assertEquals(LocalTime.MIDNIGHT, GoalSchedule.parseTime("noon"));
		assertEquals(LocalTime.MIDNIGHT, GoalSchedule.parseTime(""));
	}

	private static long utc(int year, int month, int day, int hour, int minute)
	{
		return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
	}
}
