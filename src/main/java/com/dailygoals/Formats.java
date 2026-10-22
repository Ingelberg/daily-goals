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
}
