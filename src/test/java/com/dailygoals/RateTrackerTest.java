package com.dailygoals;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RateTrackerTest
{
	private static final long MINUTE = 60_000;

	@Test
	public void xpStartsAtFirstGain()
	{
		RateTracker rate = new RateTracker(true);
		rate.observe(0, 0);
		// Idle time before the first drop doesn't count
		rate.observe(0, 10 * MINUTE);
		assertEquals(0, rate.perHour(10 * MINUTE));

		rate.observe(100, 10 * MINUTE);
		rate.observe(1_000, 40 * MINUTE);
		// 1,000 XP over 30 minutes
		assertEquals(2_000, rate.perHour(40 * MINUTE));
	}

	@Test
	public void killsStartWhenTrackingBegins()
	{
		RateTracker rate = new RateTracker(false);
		rate.observe(3, 0);
		rate.observe(4, MINUTE);
		// One kill a minute in is 60/h, not "one kill in no time"
		assertEquals(60, rate.perHour(MINUTE));
		rate.observe(6, 30 * MINUTE);
		assertEquals(6, rate.perHour(30 * MINUTE));
	}

	@Test
	public void nothingBeforeStart()
	{
		assertEquals(0, new RateTracker(true).perHour(MINUTE));
	}
}
