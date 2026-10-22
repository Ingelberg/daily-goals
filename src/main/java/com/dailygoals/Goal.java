package com.dailygoals;

import com.google.gson.annotations.SerializedName;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.util.Locale;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.util.QuantityFormatter;

/**
 * One goal as the user set it up in the sidebar. The list of goals is stored as JSON in the
 * plugin's config and is shared by every account; progress is stored per account in
 * {@link GoalProgress}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class Goal
{
	static final int ALL_DAYS = 0b111_1111;

	@RequiredArgsConstructor
	enum Type
	{
		/** Gain an amount of XP in a skill, or in total. */
		XP("Gain XP"),
		/** Reach a level in a skill. */
		LEVEL("Reach a level"),
		/** Kill a boss or complete an activity a number of times. */
		@SerializedName(value = "COUNT", alternate = "KC")
		COUNT("Count");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@RequiredArgsConstructor
	enum Period
	{
		DAILY("Daily"),
		WEEKLY("Weekly");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	String id;
	Type type = Type.XP;
	Skill skill = Skill.ATTACK;
	/** XP goal on total XP instead of {@link #skill}. */
	boolean overall;
	@SerializedName(value = "activity", alternate = "boss")
	Activity activity = Activity.ZULRAH;
	/** Name typed in for {@link Activity#OTHER}. */
	@SerializedName(value = "customName", alternate = "customBoss")
	String customName = "";
	/** XP or count per period; the target level for level goals. */
	int amount = 100_000;
	Period period = Period.DAILY;
	/** Days a daily goal counts on, one bit per day from Monday (bit 0) to Sunday (bit 6). */
	int days = ALL_DAYS;
	/** Switched off: still counted in the background, but no overlay, infobox or messages. */
	boolean enabled = true;
	boolean overlay = true;
	boolean infobox = true;

	Goal copy()
	{
		return new Goal(id, type, skill, overall, activity, customName, amount, period, days, enabled, overlay, infobox);
	}

	/** XP and level goals both follow XP. */
	boolean isXp()
	{
		return type != Type.COUNT;
	}

	boolean isLevel()
	{
		return type == Type.LEVEL;
	}

	boolean isWeekly()
	{
		return period == Period.WEEKLY;
	}

	boolean isActiveOn(DayOfWeek day)
	{
		return (days & (1 << (day.getValue() - 1))) != 0;
	}

	/**
	 * XP needed from {@code startXp} to reach the target level, for level goals.
	 */
	long xpToLevel(long startXp)
	{
		int level = Math.max(1, Math.min(Experience.MAX_VIRT_LEVEL, amount));
		return Math.max(0, Experience.getXpForLevel(level) - startXp);
	}

	/**
	 * Key for what the goal counts. Progress starts over when it changes. Null when the goal is
	 * {@link Activity#OTHER} with no name yet.
	 */
	@Nullable
	String targetKey()
	{
		String key;
		if (isLevel())
		{
			key = "level:" + skill.name() + ":" + amount;
		}
		else if (isXp())
		{
			key = "xp:" + (overall ? "OVERALL" : skill.name());
		}
		else if (activity != Activity.OTHER)
		{
			key = "kc:" + activity.name();
		}
		else
		{
			String custom = Activity.normalize(customName());
			if (custom.isEmpty())
			{
				return null;
			}
			key = "kc:other:" + custom;
		}
		return isWeekly() ? key + ":weekly" : key;
	}

	/**
	 * Whether a count read from a game message belongs to this goal.
	 */
	boolean counts(CountMessage count)
	{
		if (isXp())
		{
			return false;
		}

		switch (count.getKind())
		{
			case KILL:
				if (activity == Activity.OTHER)
				{
					String custom = Activity.normalize(customName());
					return !custom.isEmpty() && custom.equals(Activity.normalize(count.getName()));
				}
				return activity.matches(count.getName());
			case CLUE:
				return count.getName().equals(activity.clueTier());
			default:
				return activity.getKind() == count.getKind();
		}
	}

	/**
	 * What the goal counts, e.g. "Attack XP", "Level 80 Mining" or "Zulrah kills".
	 */
	String name()
	{
		if (isLevel())
		{
			return "Level " + amount + " " + skill.getName();
		}
		if (isXp())
		{
			return (overall ? "Overall" : skill.getName()) + " XP";
		}
		return activity.goalName(customName().trim());
	}

	String rateLabel()
	{
		if (isXp())
		{
			return "XP/h";
		}
		return activity.getKind() == Activity.Kind.KILL ? "Kills/h" : "Per hour";
	}

	private String customName()
	{
		return customName == null ? "" : customName;
	}

	/**
	 * Parse an amount such as "250,000", "150k" or "1.5m". Null if it isn't a usable amount.
	 */
	@Nullable
	static Integer parseAmount(String text)
	{
		try
		{
			long amount = QuantityFormatter.parseQuantity(text.replace(",", "").replace(" ", "").toLowerCase(Locale.ROOT));
			return amount >= 1 && amount <= Integer.MAX_VALUE ? (int) amount : null;
		}
		catch (ParseException | NumberFormatException e)
		{
			return null;
		}
	}
}
