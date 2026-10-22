package com.dailygoals;

import java.util.List;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CountMessageTest
{
	@Test
	public void bossKill()
	{
		assertCounts("Your Zulrah kill count is: <col=ff0000>1,234</col>.", Activity.ZULRAH, 1234);
	}

	@Test
	public void apostropheAndCase()
	{
		assertCounts("Your Kree'arra kill count is: <col=ff0000>87</col>.", Activity.KREEARRA, 87);
	}

	@Test
	public void raidCompletions()
	{
		assertCounts("Your completed Chambers of Xeric count is: <col=ff0000>50</col>.", Activity.CHAMBERS_OF_XERIC, 50);
		assertCounts("Your completed Theatre of Blood: Hard Mode count is: <col=ff0000>3</col>.", Activity.THEATRE_OF_BLOOD_HARD_MODE, 3);
		assertCounts("Your completed Tombs of Amascut: Expert Mode count is: <col=ff0000>12</col>.", Activity.TOMBS_OF_AMASCUT_EXPERT, 12);
	}

	@Test
	public void otherWordings()
	{
		assertCounts("Your Barrows chest count is: <col=ff0000>400</col>.", Activity.BARROWS_CHESTS, 400);
		assertCounts("Your Gauntlet completion count is: <col=ff0000>9</col>.", Activity.THE_GAUNTLET, 9);
		assertCounts("Your Corrupted Gauntlet completion count is: <col=ff0000>9</col>.", Activity.THE_CORRUPTED_GAUNTLET, 9);
		assertCounts("Your subdued Wintertodt count is: <col=ff0000>600</col>.", Activity.WINTERTODT, 600);
	}

	@Test
	public void bossesDontMatchEachOther()
	{
		CountMessage gauntlet = only("Your Corrupted Gauntlet completion count is: <col=ff0000>9</col>.");
		assertFalse(goal(Activity.THE_GAUNTLET, "").counts(gauntlet));
		CountMessage phosani = only("Your Phosani's Nightmare kill count is: <col=ff0000>2</col>.");
		assertFalse(goal(Activity.NIGHTMARE, "").counts(phosani));
	}

	@Test
	public void otherBossByName()
	{
		CountMessage hespori = only("Your Hespori kill count is: <col=ff0000>14</col>.");
		assertTrue(goal(Activity.OTHER, "hespori").counts(hespori));
		assertFalse(goal(Activity.OTHER, "").counts(hespori));
	}

	@Test
	public void xpGoalsIgnoreCounts()
	{
		Goal xp = new Goal("id", Goal.Type.XP, Skill.ATTACK, false, Activity.ZULRAH, "", 100, Goal.Period.DAILY, Goal.ALL_DAYS, true, true, true);
		assertFalse(xp.counts(only("Your Zulrah kill count is: <col=ff0000>5</col>.")));
	}

	@Test
	public void clues()
	{
		List<CountMessage> counts = CountMessage.parse("You have completed <col=ff0000>1,204</col> hard Treasure Trails.");
		assertEquals(2, counts.size());

		// The tier's running total...
		assertTrue(goal(Activity.CLUES_HARD, "").counts(counts.get(0)));
		assertFalse(goal(Activity.CLUES_MEDIUM, "").counts(counts.get(0)));
		assertEquals(1204, counts.get(0).getValue());
		assertFalse(counts.get(0).isIncrement());

		// ...and one more for "all tiers"
		assertTrue(goal(Activity.CLUES_ALL, "").counts(counts.get(1)));
		assertEquals(1, counts.get(1).getValue());
		assertTrue(counts.get(1).isIncrement());

		assertEquals(2, CountMessage.parse("You have completed 1 beginner Treasure Trail.").size());
	}

	@Test
	public void riftsAndSepulchre()
	{
		CountMessage rifts = only("Amount of rifts you have closed: <col=ff0000>321</col>.");
		assertTrue(goal(Activity.RIFTS_CLOSED, "").counts(rifts));
		assertEquals(321, rifts.getValue());

		CountMessage floors = only("You have completed Floor 5 of the Hallowed Sepulchre! Total completions: <col=ff0000>88</col>.");
		assertTrue(goal(Activity.SEPULCHRE_FLOORS, "").counts(floors));
		assertEquals(88, floors.getValue());
	}

	@Test
	public void otherMessagesAreIgnored()
	{
		assertTrue(CountMessage.parse("Welcome to Old School RuneScape.").isEmpty());
		// "Your X is: N" without kill/count words isn't a kill count
		assertTrue(CountMessage.parse("Your reward is: <col=ff0000>5</col>.").isEmpty());
	}

	@Test
	public void namesNormalize()
	{
		assertEquals(Activity.normalize("Hallowed Sepulchre"), Activity.normalize("hallowed  sepulchre"));
		assertEquals(Activity.normalize("Kree'Arra"), Activity.normalize("kreearra"));
		assertEquals(Activity.normalize("The Gauntlet"), Activity.normalize("Gauntlet"));
	}

	private static void assertCounts(String message, Activity activity, long value)
	{
		CountMessage count = only(message);
		assertTrue(count.getName(), goal(activity, "").counts(count));
		assertEquals(value, count.getValue());
		assertFalse(count.isIncrement());
	}

	private static CountMessage only(String message)
	{
		List<CountMessage> counts = CountMessage.parse(message);
		assertEquals(message, 1, counts.size());
		return counts.get(0);
	}

	private static Goal goal(Activity activity, String customName)
	{
		return new Goal("id", Goal.Type.COUNT, Skill.ATTACK, false, activity, customName, 10, Goal.Period.DAILY, Goal.ALL_DAYS, true, true, true);
	}
}
