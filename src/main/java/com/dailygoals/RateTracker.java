package com.dailygoals;

/**
 * Per-hour rate for one goal during this login. Like the XP Tracker, XP goals start the clock at
 * the first XP drop. Kill count goals start it when tracking begins, since one kill right after
 * the start would otherwise read as thousands per hour.
 */
class RateTracker
{
	private final boolean startOnFirstGain;
	private boolean started;
	private long startMillis;
	private long startProgress;
	private long lastProgress = -1;

	RateTracker(boolean startOnFirstGain)
	{
		this.startOnFirstGain = startOnFirstGain;
	}

	void observe(long progress, long now)
	{
		if (!started && (!startOnFirstGain ? lastProgress < 0 : lastProgress >= 0 && progress > lastProgress))
		{
			started = true;
			startMillis = now;
			startProgress = startOnFirstGain ? lastProgress : progress;
		}
		lastProgress = progress;
	}

	long perHour(long now)
	{
		long elapsed = now - startMillis;
		if (!started || elapsed <= 0)
		{
			return 0;
		}
		return (lastProgress - startProgress) * 3_600_000L / elapsed;
	}
}
