package com.dailygoals;

import com.dailygoals.DailyGoalsConfig.CarryOver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GoalProgressTest
{
	@Test
	public void xpCountsFromFirstSeen()
	{
		GoalProgress state = new GoalProgress("xp:ATTACK", 0);
		state.observeXp(1_000_000);
		state.observeXp(1_040_000);
		assertEquals(40_000, state.progress());
		assertEquals(60_000, state.left(100_000));
		assertFalse(state.isDone(100_000));
	}

	@Test
	public void firstKillMessageCountsAsOneKill()
	{
		GoalProgress state = new GoalProgress("kc:ZULRAH", 0);
		state.observeTotal(500);
		assertEquals(1, state.progress());
		state.observeTotal(503);
		assertEquals(4, state.progress());
	}

	@Test
	public void noCarryOver()
	{
		GoalProgress state = played(150);
		state.rollover(1, 100, CarryOver.OFF, true, false);
		assertEquals(0, state.progress());
		assertEquals(100, state.goal(100));
	}

	@Test
	public void extraProgressCarries()
	{
		GoalProgress state = played(150);
		state.rollover(1, 100, CarryOver.EXTRA, true, false);
		assertEquals(50, state.progress());
		assertEquals(100, state.goal(100));
	}

	@Test
	public void missedAmountCarries()
	{
		GoalProgress state = played(70);
		state.rollover(1, 100, CarryOver.MISSED, true, false);
		assertEquals(0, state.progress());
		assertEquals(130, state.goal(100));

		// Only the extra direction is ignored in MISSED mode
		state = played(150);
		state.rollover(1, 100, CarryOver.MISSED, true, false);
		assertEquals(0, state.progress());
		assertEquals(100, state.goal(100));
	}

	@Test
	public void bothCarriesSettleAgainstEachOther()
	{
		GoalProgress state = played(70);
		state.rollover(1, 100, CarryOver.BOTH, true, false);
		assertEquals(130, state.goal(100));

		// 160 done against 130 needed: 30 extra goes to the next day, the missed amount is cleared
		state.observeXp(1_000 + 70 + 160);
		state.rollover(2, 100, CarryOver.BOTH, true, false);
		assertEquals(30, state.progress());
		assertEquals(100, state.goal(100));
	}

	@Test
	public void rolloverKeepsCountingFromLastSeenValue()
	{
		GoalProgress state = played(150);
		state.completed = true;
		state.rollover(1, 100, CarryOver.OFF, true, false);
		assertFalse(state.completed);
		assertEquals(1, state.day);

		// XP gained while the client was closed counts toward the new day
		state.observeXp(1_000 + 150 + 25);
		assertEquals(25, state.progress());
	}

	@Test
	public void rolloverBeforeAnythingSeen()
	{
		GoalProgress state = new GoalProgress("kc:ZULRAH", 0);
		state.rollover(1, 5, CarryOver.BOTH, true, false);
		assertNull(state.baseline);
		assertEquals(10, state.goal(5));
		state.observeTotal(42);
		assertEquals(1, state.progress());
		assertTrue(state.left(5) == 9);
	}

	@Test
	public void streakAndHistory()
	{
		GoalProgress state = played(150);
		state.rollover(1, 100, CarryOver.OFF, true, false);
		assertEquals(1, state.streak);
		assertEquals(1, state.history.size());
		assertEquals(150, state.history.get(0).done);
		assertEquals(100, state.history.get(0).goal);

		// Done again: streak grows, and counts today once today is done
		state.observeXp(1_000 + 150 + 100);
		assertEquals(2, state.currentStreak(100, true));
		state.rollover(2, 100, CarryOver.OFF, true, false);
		assertEquals(2, state.streak);

		// A missed day ends it, but the best is kept
		state.observeXp(1_000 + 250 + 10);
		state.rollover(3, 100, CarryOver.OFF, true, false);
		assertEquals(0, state.streak);
		assertEquals(2, state.bestStreak);
	}

	@Test
	public void restDaysKeepStreakAndCarry()
	{
		GoalProgress state = played(70);
		state.rollover(1, 100, CarryOver.MISSED, true, false);
		assertEquals(30, state.missed);
		// A rest day passes the carry-over on and leaves the streak alone
		state.rollover(2, 100, CarryOver.MISSED, false, false);
		assertEquals(30, state.missed);
		assertEquals(1, state.history.size());
	}

	@Test
	public void skippedDayEndsStreak()
	{
		GoalProgress state = played(150);
		state.rollover(5, 100, CarryOver.OFF, true, true);
		assertEquals(0, state.streak);
		assertEquals(1, state.bestStreak);
	}

	@Test
	public void milestoneResetsEachPeriod()
	{
		GoalProgress state = played(80);
		state.milestone = 75;
		state.rollover(1, 100, CarryOver.OFF, true, false);
		assertEquals(0, state.milestone);
	}

	@Test
	public void overAndCountIncrements()
	{
		GoalProgress state = played(150);
		assertEquals(50, state.over(100));
		assertEquals(0, state.left(100));

		GoalProgress clues = new GoalProgress("kc:CLUES_ALL", 0);
		clues.add(1);
		clues.add(1);
		assertEquals(2, clues.progress());
	}

	@Test
	public void olderSavesGetAnEmptyHistory()
	{
		GoalProgress state = new com.google.gson.Gson().fromJson("{\"target\":\"xp:ATTACK\",\"day\":5,\"gained\":3}", GoalProgress.class);
		assertEquals(0, state.history.size());
		assertEquals(3, state.progress());
	}

	private static GoalProgress played(long xp)
	{
		GoalProgress state = new GoalProgress("xp:ATTACK", 0);
		state.observeXp(1_000);
		state.observeXp(1_000 + xp);
		return state;
	}
}
