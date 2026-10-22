package com.dailygoals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GoalTest
{
	@Test
	public void targetKeys()
	{
		assertEquals("xp:MINING", xp(Skill.MINING).targetKey());
		assertEquals("kc:VORKATH", count(Activity.VORKATH, "").targetKey());
		assertEquals("kc:other:hallowedsepulchre", count(Activity.OTHER, "Hallowed Sepulchre").targetKey());
		assertNull(count(Activity.OTHER, "  ").targetKey());
	}

	@Test
	public void overall()
	{
		Goal goal = xp(Skill.MINING);
		goal.setOverall(true);
		assertEquals("xp:OVERALL", goal.targetKey());
		assertEquals("Overall XP", goal.name());
	}

	@Test
	public void levelGoal()
	{
		Goal goal = xp(Skill.MINING);
		goal.setType(Goal.Type.LEVEL);
		goal.setAmount(2);
		assertEquals("Level 2 Mining", goal.name());
		assertEquals("level:MINING:2", goal.targetKey());
		// Level 2 is at 83 XP
		assertEquals(83, goal.xpToLevel(0));
		assertEquals(0, goal.xpToLevel(500));
		assertTrue(goal.isXp());
	}

	@Test
	public void weeklyAndDays()
	{
		Goal goal = xp(Skill.MINING);
		goal.setDays(0b001_1111);
		assertTrue(goal.isActiveOn(DayOfWeek.MONDAY));
		assertTrue(goal.isActiveOn(DayOfWeek.FRIDAY));
		assertFalse(goal.isActiveOn(DayOfWeek.SATURDAY));
		goal.setPeriod(Goal.Period.WEEKLY);
		assertEquals("xp:MINING:weekly", goal.targetKey());
	}

	@Test
	public void names()
	{
		assertEquals("Mining XP", xp(Skill.MINING).name());
		assertEquals("Vorkath kills", count(Activity.VORKATH, "").name());
		assertEquals("Hespori kills", count(Activity.OTHER, " Hespori ").name());
		assertEquals("Medium clues", count(Activity.CLUES_MEDIUM, "").name());
		assertEquals("Rifts closed", count(Activity.RIFTS_CLOSED, "").name());
	}

	@Test
	public void rateLabels()
	{
		assertEquals("XP/h", xp(Skill.MINING).rateLabel());
		assertEquals("Kills/h", count(Activity.VORKATH, "").rateLabel());
		assertEquals("Per hour", count(Activity.RIFTS_CLOSED, "").rateLabel());
	}

	@Test
	public void categories()
	{
		assertEquals(Activity.Category.BOSS, Activity.VORKATH.getCategory());
		assertEquals(Activity.Category.RAID, Activity.TOMBS_OF_AMASCUT.getCategory());
		assertEquals(Activity.Category.MINIGAME, Activity.TEMPOROSS.getCategory());
		assertEquals(Activity.Category.CLUE, Activity.CLUES_ALL.getCategory());
		assertEquals(Activity.Category.OTHER, Activity.OTHER.getCategory());
		assertEquals("medium", Activity.CLUES_MEDIUM.clueTier());
	}

	@Test
	public void parseAmount()
	{
		assertEquals(Integer.valueOf(250_000), Goal.parseAmount("250,000"));
		assertEquals(Integer.valueOf(150_000), Goal.parseAmount("150k"));
		assertEquals(Integer.valueOf(150_000), Goal.parseAmount("150K"));
		assertEquals(Integer.valueOf(1_500_000), Goal.parseAmount("1.5m"));
		assertEquals(Integer.valueOf(25), Goal.parseAmount(" 25 "));
		assertNull(Goal.parseAmount("0"));
		assertNull(Goal.parseAmount("lots"));
		assertNull(Goal.parseAmount(""));
		assertNull(Goal.parseAmount("5b"));
	}

	@Test
	public void jsonRoundTrip()
	{
		Gson gson = new Gson();
		List<Goal> goals = Arrays.asList(xp(Skill.SAILING), count(Activity.OTHER, "Hespori"));
		List<Goal> back = gson.fromJson(gson.toJson(goals), new TypeToken<List<Goal>>()
		{
		}.getType());
		assertEquals(goals, back);
	}

	@Test
	public void readsOlderSaves()
	{
		// Before the cleanup: "KC" type, "boss" and "customBoss" fields, no enabled/period/days
		Goal goal = new Gson().fromJson("{\"id\":\"a\",\"type\":\"KC\",\"boss\":\"OTHER\",\"customBoss\":\"Hespori\",\"amount\":5}", Goal.class);
		assertEquals(Goal.Type.COUNT, goal.getType());
		assertEquals(Activity.OTHER, goal.getActivity());
		assertEquals("Hespori", goal.getCustomName());
		assertTrue(goal.isEnabled());
		assertEquals(Goal.Period.DAILY, goal.getPeriod());
		assertEquals(Goal.ALL_DAYS, goal.getDays());
	}

	private static Goal xp(Skill skill)
	{
		return new Goal("id", Goal.Type.XP, skill, false, Activity.ZULRAH, "", 100, Goal.Period.DAILY, Goal.ALL_DAYS, true, true, true);
	}

	private static Goal count(Activity activity, String customName)
	{
		return new Goal("id", Goal.Type.COUNT, Skill.ATTACK, false, activity, customName, 100, Goal.Period.DAILY, Goal.ALL_DAYS, true, true, true);
	}
}
