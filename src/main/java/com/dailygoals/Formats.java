package com.dailygoals;

import net.runelite.client.util.QuantityFormatter;

/**
 * Number and time formatting shared by the overlay, infobox and panel.
 */
final class Formats
{
	private Formats()
	{
	}

	/**
	 * "12,345".
	 */
	static String number(long value)
	{
		return QuantityFormatter.formatNumber(value);
	}

	/**
	 * "12.3K", for small spaces like the infobox.
	 */
	static String shortNumber(long value)
	{
		return QuantityFormatter.quantityToStackSize(value);
	}

	/**
	 * {@code value} as a percentage of {@code target}: "45.0%".
	 */
	static String percent(long value, long target)
	{
		return String.format("%.1f%%", ratio(value, target));
	}

	/**
	 * Whole percent, rounded down so it doesn't read 100% before it's done: "45%", for the infobox.
	 */
	static String shortPercent(long value, long target)
	{
		return (long) Math.floor(ratio(value, target)) + "%";
	}

	/**
	 * "1h 23m", "23m", "<1m" or "5d"; "-" when unknown (negative).
	 */
	static String duration(long millis)
	{
		if (millis < 0)
		{
			return "-";
		}
		long minutes = millis / 60_000;
		if (minutes < 1)
		{
			return "<1m";
		}
		long hours = minutes / 60;
		if (hours >= 100)
		{
			return hours / 24 + "d";
		}
		return hours > 0 ? hours + "h " + minutes % 60 + "m" : minutes + "m";
	}

	/**
	 * "+12,000" ahead of pace or "-5,000" behind it.
	 */
	static String pace(long pace)
	{
		return (pace >= 0 ? "+" : "-") + number(Math.abs(pace));
	}

	/**
	 * Pace as a percentage of the target: "+12.0%" or "-5.0%".
	 */
	static String pacePercent(long pace, long target)
	{
		return (pace >= 0 ? "+" : "-") + percent(Math.abs(pace), target);
	}

	private static double ratio(long value, long target)
	{
		return target <= 0 ? 100 : 100.0 * value / target;
	}
}
