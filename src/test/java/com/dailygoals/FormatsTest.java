package com.dailygoals;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatsTest
{
	@Test
	public void numbers()
	{
		assertEquals("12,345", Formats.number(12_345));
		assertEquals("10K", Formats.shortNumber(10_000));
	}

	@Test
	public void durations()
	{
		assertEquals("-", Formats.duration(-1));
		assertEquals("<1m", Formats.duration(30_000));
		assertEquals("23m", Formats.duration(23 * 60_000));
		assertEquals("1h 5m", Formats.duration(65 * 60_000));
		assertEquals("5d", Formats.duration(120 * 3_600_000L));
	}

	@Test
	public void percentages()
	{
		assertEquals("45.0%", Formats.percent(45_000, 100_000));
		assertEquals("110.0%", Formats.percent(110, 100));
		// Rounded down, so it never reads 100% before the goal is done
		assertEquals("99%", Formats.shortPercent(99_999, 100_000));
		assertEquals("10%", Formats.shortPercent(10_000, 100_000));
		assertEquals("+12.0%", Formats.pacePercent(12, 100));
		assertEquals("-5.0%", Formats.pacePercent(-5, 100));
	}

	@Test
	public void pace()
	{
		assertEquals("+12,000", Formats.pace(12_000));
		assertEquals("+0", Formats.pace(0));
		assertEquals("-5,000", Formats.pace(-5_000));
	}
}
