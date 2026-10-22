package com.dailygoals;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * A goal's periods (days or weeks) under the current reset settings. A period is identified by
 * the epoch millis it starts at. DST gaps are handled by {@link ZonedDateTime#of}, which moves
 * a missing reset time forward.
 */
@RequiredArgsConstructor
final class GoalSchedule
{
	private static final long DAY = 86_400_000L;
	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter WEEK_FORMAT = DateTimeFormatter.ofPattern("'Week of' d MMM", Locale.ENGLISH);
	// "6", "06:30", "6.30", "0630"
	private static final Pattern TIME = Pattern.compile("(\\d{1,2})(?:[:.]?(\\d{2}))?");

	private final ZoneId zone;
	private final LocalTime resetTime;
	private final DayOfWeek weeklyResetDay;

	static GoalSchedule of(DailyGoalsConfig config)
	{
		return new GoalSchedule(config.timeZone().zone(), parseTime(config.resetTime()), config.weeklyResetDay().getDayOfWeek());
	}

	long periodStart(Goal goal, long at)
	{
		ZonedDateTime now = Instant.ofEpochMilli(at).atZone(zone);
		return goal.isWeekly() ? lastWeeklyReset(now, resetTime, weeklyResetDay) : lastDailyReset(now, resetTime);
	}

	long nextPeriodStart(Goal goal, long start)
	{
		// Half a day past a full period is safely inside the next one, DST or not
		return periodStart(goal, start + (goal.isWeekly() ? 7 * DAY : DAY) + DAY / 2);
	}

	long previousPeriodStart(Goal goal, long start)
	{
		return periodStart(goal, start - 1);
	}

	/**
	 * Whether the goal counts in the period starting at {@code start}; daily goals can skip days.
	 */
	boolean isActive(Goal goal, long start)
	{
		return goal.isWeekly() || goal.isActiveOn(dayOfWeek(start));
	}

	/**
	 * Whether the daily period starting at {@code start} is a Saturday or Sunday.
	 */
	boolean isWeekend(long start)
	{
		DayOfWeek day = dayOfWeek(start);
		return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
	}

	/**
	 * How far through the period {@code now} is, 0 to 1.
	 */
	double elapsedFraction(Goal goal, long start, long now)
	{
		long end = nextPeriodStart(goal, start);
		double fraction = (now - start) / (double) (end - start);
		return Math.max(0, Math.min(1, fraction));
	}

	/**
	 * Whether a period the goal counts in lies strictly between two periods, i.e. was skipped.
	 */
	boolean missedBetween(Goal goal, long ended, long next)
	{
		int guard = 0;
		for (long p = nextPeriodStart(goal, ended); p < next && guard++ < 1000; p = nextPeriodStart(goal, p))
		{
			if (isActive(goal, p))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * "Thu 22 Oct", or "Week of 20 Oct" for weekly goals.
	 */
	String label(Goal goal, long start)
	{
		return (goal.isWeekly() ? WEEK_FORMAT : DAY_FORMAT).format(Instant.ofEpochMilli(start).atZone(zone));
	}

	private DayOfWeek dayOfWeek(long start)
	{
		return Instant.ofEpochMilli(start).atZone(zone).getDayOfWeek();
	}

	/**
	 * Epoch millis of the most recent daily reset at or before {@code now}.
	 */
	static long lastDailyReset(ZonedDateTime now, LocalTime time)
	{
		ZonedDateTime reset = ZonedDateTime.of(now.toLocalDate(), time, now.getZone());
		if (reset.isAfter(now))
		{
			reset = ZonedDateTime.of(now.toLocalDate().minusDays(1), time, now.getZone());
		}
		return reset.toInstant().toEpochMilli();
	}

	/**
	 * Epoch millis of the most recent weekly reset (on {@code day} at {@code time}) at or before {@code now}.
	 */
	static long lastWeeklyReset(ZonedDateTime now, LocalTime time, DayOfWeek day)
	{
		LocalDate date = now.toLocalDate().with(TemporalAdjusters.previousOrSame(day));
		ZonedDateTime reset = ZonedDateTime.of(date, time, now.getZone());
		if (reset.isAfter(now))
		{
			reset = ZonedDateTime.of(date.minusWeeks(1), time, now.getZone());
		}
		return reset.toInstant().toEpochMilli();
	}

	/**
	 * Parse the reset time setting. Anything unreadable means midnight.
	 */
	static LocalTime parseTime(String text)
	{
		Matcher matcher = TIME.matcher(text == null ? "" : text.trim());
		if (!matcher.matches())
		{
			return LocalTime.MIDNIGHT;
		}

		int hour = Integer.parseInt(matcher.group(1));
		int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
		if (hour > 23 || minute > 59)
		{
			return LocalTime.MIDNIGHT;
		}
		return LocalTime.of(hour, minute);
	}
}
