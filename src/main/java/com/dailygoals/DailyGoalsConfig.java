package com.dailygoals;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

/**
 * The settings page: when goals reset, carry-over, and display options. The goals themselves
 * are edited in the sidebar and stored by {@link GoalStore}.
 */
@ConfigGroup(DailyGoalsConfig.GROUP)
public interface DailyGoalsConfig extends Config
{
	String GROUP = "dailygoals";

	@RequiredArgsConstructor
	enum WeekDay
	{
		MONDAY(DayOfWeek.MONDAY),
		TUESDAY(DayOfWeek.TUESDAY),
		WEDNESDAY(DayOfWeek.WEDNESDAY),
		THURSDAY(DayOfWeek.THURSDAY),
		FRIDAY(DayOfWeek.FRIDAY),
		SATURDAY(DayOfWeek.SATURDAY),
		SUNDAY(DayOfWeek.SUNDAY);

		@Getter
		private final DayOfWeek dayOfWeek;

		@Override
		public String toString()
		{
			String name = name();
			return name.charAt(0) + name.substring(1).toLowerCase();
		}
	}

	@RequiredArgsConstructor
	enum CarryOver
	{
		OFF("Off", false, false),
		EXTRA("Extra progress", true, false),
		MISSED("Missed amount", false, true),
		BOTH("Both", true, true);

		private final String label;
		@Getter
		private final boolean carryExtra;
		@Getter
		private final boolean carryMissed;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@RequiredArgsConstructor
	enum TimeZoneMode
	{
		LOCAL("Local time"),
		UTC("UTC (game reset)");

		private final String label;

		ZoneId zone()
		{
			return this == UTC ? ZoneOffset.UTC : ZoneId.systemDefault();
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@RequiredArgsConstructor
	enum DisplayLine
	{
		DONE("Done"),
		LEFT("Left"),
		GOAL("Goal"),
		PERCENT("Progress"),
		RATE("XP/h"),
		ETA("Done in"),
		PACE("Pace");

		private final String label;

		/**
		 * Label for a goal's line: per hour reads "Kills/h" and so on for count goals.
		 */
		String label(Goal goal)
		{
			return this == RATE ? goal.rateLabel() : label;
		}

		@Override
		public String toString()
		{
			return this == RATE ? "XP/h (kills/h)" : label;
		}
	}

	@RequiredArgsConstructor
	enum Show
	{
		INFOBOXES("Infoboxes", true, false),
		OVERLAYS("Overlays", false, true),
		BOTH("Both", true, true);

		private final String label;
		@Getter
		private final boolean infoboxes;
		@Getter
		private final boolean overlays;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@RequiredArgsConstructor
	enum ValueFormat
	{
		AMOUNT("XP / count"),
		PERCENT("Percent");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(
		name = "Reset",
		description = "When goals start over and what carries over",
		position = 0
	)
	String resetSection = "reset";

	@ConfigSection(
		name = "Display",
		description = "Overlays, infoboxes, notifications and chat messages",
		position = 1
	)
	String displaySection = "display";

	@ConfigItem(
		keyName = "resetTime",
		section = resetSection,
		name = "Reset time",
		description = "Time of day goals start over, as HH:MM in 24-hour time (e.g. 04:00 or 06:30)",
		position = 0
	)
	default String resetTime()
	{
		return "04:00";
	}

	@ConfigItem(
		keyName = "weeklyResetDay",
		section = resetSection,
		name = "Weekly reset day",
		description = "Day weekly goals start over, at the reset time. (The OSRS weekly reset is Wednesday)",
		position = 1
	)
	default WeekDay weeklyResetDay()
	{
		return WeekDay.MONDAY;
	}

	@ConfigItem(
		keyName = "timeZone",
		section = resetSection,
		name = "Time zone",
		description = "Whether the reset time is your computer's local time or UTC. The OSRS daily reset is 00:00 UTC",
		position = 2
	)
	default TimeZoneMode timeZone()
	{
		return TimeZoneMode.LOCAL;
	}

	@ConfigItem(
		keyName = "carryOver",
		section = resetSection,
		name = "Carry over",
		description = "Extra progress: XP/kills above the goal count toward the next day or week."
			+ " Missed amount: what you didn't finish is added to the next goal."
			+ " Only the last day you played carries over; rest days pass it on",
		position = 3
	)
	default CarryOver carryOver()
	{
		return CarryOver.OFF;
	}

	@Range(min = 10, max = 1000)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "weekendMultiplier",
		section = resetSection,
		name = "Weekend multiplier",
		description = "Daily goals on Saturday and Sunday are this much of the normal amount:"
			+ " 150% makes a 100K goal 150K, 50% halves it. 100% turns it off. Level and weekly goals aren't changed",
		position = 4
	)
	default int weekendMultiplier()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "show",
		name = "Show",
		description = "Show infoboxes, overlays or both, for goals that have them ticked in the sidebar",
		section = displaySection,
		position = 0
	)
	default Show show()
	{
		return Show.BOTH;
	}

	@ConfigItem(
		keyName = "valueFormat",
		name = "Values",
		description = "Show amounts as XP / count or as a percentage of the goal,"
			+ " on infoboxes and the overlay's Done, Left and Pace lines",
		section = displaySection,
		position = 1
	)
	default ValueFormat valueFormat()
	{
		return ValueFormat.AMOUNT;
	}

	@ConfigItem(
		keyName = "hideWhenDone",
		name = "Hide overlay when done",
		description = "Hide a goal's overlay once it's reached for the day; the infobox stays",
		section = displaySection,
		position = 2
	)
	default boolean hideWhenDone()
	{
		return false;
	}

	@ConfigItem(
		keyName = "paceColors",
		name = "Pace colours",
		description = "Turn the progress bar amber when you're behind the pace needed to finish by the reset",
		section = displaySection,
		position = 3
	)
	default boolean paceColors()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayTopLine",
		name = "Overlay top line",
		description = "What the top line of each goal's overlay shows",
		section = displaySection,
		position = 4
	)
	default DisplayLine overlayTopLine()
	{
		return DisplayLine.DONE;
	}

	@ConfigItem(
		keyName = "overlayBottomLine",
		name = "Overlay bottom line",
		description = "What the bottom line of each goal's overlay shows",
		section = displaySection,
		position = 5
	)
	default DisplayLine overlayBottomLine()
	{
		return DisplayLine.LEFT;
	}

	@ConfigItem(
		keyName = "notifyComplete",
		name = "Notify when done",
		description = "Send a notification when a goal is reached",
		section = displaySection,
		position = 6
	)
	default Notification notifyComplete()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "notifyMilestones",
		name = "Notify at 50% / 75%",
		description = "Send a notification when a goal is half and three quarters done"
			+ " (a chat message is posted either way when chat messages are on)",
		section = displaySection,
		position = 7
	)
	default Notification notifyMilestones()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "chatMessages",
		name = "Chat messages",
		description = "Post a chat message at 50% / 75%, when a goal is reached and when goals reset",
		section = displaySection,
		position = 8
	)
	default boolean chatMessages()
	{
		return true;
	}
}
