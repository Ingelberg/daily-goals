package com.dailygoals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.runelite.client.util.Text;

/**
 * A count read from a game chat message, such as a boss kill count or a clue completion.
 */
@Value
class CountMessage
{
	// Same as the Chat Commands plugin's kill count pattern
	private static final Pattern KILL_COUNT = Pattern.compile("Your (?<pre>completion count for |subdued |completed )?(?:<col=[0-9a-f]{6}>)?(?<boss>.+?)(?:</col>)? (?<post>(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?)is: ?(?:<col=[0-9a-f]{6}>|@.+?@)(?<kc>[0-9,]+)</col>");
	// Matched with tags removed, as the Loot Tracker plugin does
	private static final Pattern CLUE = Pattern.compile("You have completed ([0-9,]+) (beginner|easy|medium|hard|elite|master) Treasure Trails?\\.");
	// Same as the Chat Commands plugin
	private static final Pattern RIFTS = Pattern.compile("Amount of Rifts you have closed: <col=ff0000>([0-9,]+)</col>\\.", Pattern.CASE_INSENSITIVE);
	private static final Pattern SEPULCHRE = Pattern.compile("You have completed Floor (\\d) of the Hallowed Sepulchre! Total completions: <col=ff0000>([0-9,]+)</col>\\.");

	Activity.Kind kind;
	/** The boss name for kills, the tier ("medium") for clues, otherwise empty. */
	String name;
	long value;
	/** True when {@link #value} is how many to add; false when it's the new running total. */
	boolean increment;

	/**
	 * The counts in a chat message. Usually none; one clue message gives both its tier's total
	 * and one more for "any tier" goals.
	 */
	static List<CountMessage> parse(String message)
	{
		List<CountMessage> counts = new ArrayList<>();

		Matcher matcher = KILL_COUNT.matcher(message);
		// "Your X is: N" without kill/count words is a different message (see Chat Commands)
		if (matcher.find() && !(matcher.group("pre") == null && matcher.group("post").isEmpty()))
		{
			counts.add(new CountMessage(Activity.Kind.KILL, matcher.group("boss"), number(matcher.group("kc")), false));
			return counts;
		}

		matcher = CLUE.matcher(Text.removeTags(message));
		if (matcher.find())
		{
			counts.add(new CountMessage(Activity.Kind.CLUE, matcher.group(2), number(matcher.group(1)), false));
			counts.add(new CountMessage(Activity.Kind.CLUE_ANY, "", 1, true));
			return counts;
		}

		matcher = RIFTS.matcher(message);
		if (matcher.find())
		{
			counts.add(new CountMessage(Activity.Kind.RIFTS, "", number(matcher.group(1)), false));
			return counts;
		}

		matcher = SEPULCHRE.matcher(message);
		if (matcher.find())
		{
			counts.add(new CountMessage(Activity.Kind.SEPULCHRE, "", number(matcher.group(2)), false));
		}
		return counts;
	}

	private static long number(String text)
	{
		return Long.parseLong(text.replace(",", ""));
	}
}
