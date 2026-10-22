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
	public void pace()
	{
		assertEquals("+12,000", Formats.pace(12_000));
		assertEquals("+0", Formats.pace(0));
		assertEquals("-5,000", Formats.pace(-5_000));
	}
}
