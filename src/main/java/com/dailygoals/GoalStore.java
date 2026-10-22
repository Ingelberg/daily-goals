package com.dailygoals;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.util.Text;

/**
 * Reads and writes the plugin's data in RuneLite's config. Each character (RuneScape profile)
 * has its own goals and progress.
 */
@Slf4j
@Singleton
class GoalStore
{
	static final String GOALS_KEY = "goals";
	static final String PROGRESS_KEY = "progress";
	private static final Type GOAL_LIST_TYPE = new TypeToken<List<Goal>>()
	{
	}.getType();
	private static final Type PROGRESS_TYPE = new TypeToken<Map<String, GoalProgress>>()
	{
	}.getType();

	/**
	 * Another character's goals, for copying to the current one.
	 */
	@Value
	static class CharacterGoals
	{
		String name;
		List<Goal> goals;
	}

	private final ConfigManager configManager;
	private final Gson gson;

	/** Last progress JSON read or written, so unchanged progress isn't written every tick. */
	@Nullable
	private String savedProgress;

	@Inject
	GoalStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	List<Goal> loadGoals(String profile)
	{
		return parseGoals(configManager.getConfiguration(DailyGoalsConfig.GROUP, profile, GOALS_KEY));
	}

	void saveGoals(String profile, List<Goal> goals)
	{
		configManager.setConfiguration(DailyGoalsConfig.GROUP, profile, GOALS_KEY, gson.toJson(goals));
	}

	/**
	 * Goals used to be one list shared by every character. Give that list to the first character
	 * that logs in without goals of its own, then remove it.
	 */
	void migrateSharedGoals(String profile)
	{
		String shared = configManager.getConfiguration(DailyGoalsConfig.GROUP, GOALS_KEY);
		if (shared == null)
		{
			return;
		}

		if (configManager.getConfiguration(DailyGoalsConfig.GROUP, profile, GOALS_KEY) == null)
		{
			configManager.setConfiguration(DailyGoalsConfig.GROUP, profile, GOALS_KEY, shared);
		}
		configManager.unsetConfiguration(DailyGoalsConfig.GROUP, GOALS_KEY);
	}

	/**
	 * Every other character that has goals, in RuneLite's profile order.
	 */
	List<CharacterGoals> otherCharacters(String currentProfile)
	{
		List<CharacterGoals> characters = new ArrayList<>();
		for (RuneScapeProfile profile : configManager.getRSProfiles())
		{
			if (profile.getKey().equals(currentProfile))
			{
				continue;
			}

			List<Goal> goals = loadGoals(profile.getKey());
			if (!goals.isEmpty())
			{
				characters.add(new CharacterGoals(characterName(profile), goals));
			}
		}
		return characters;
	}

	/**
	 * The character's name, with the world type for anything but normal worlds, e.g.
	 * "Zezima (Deadman)".
	 */
	String characterName(String profileKey)
	{
		for (RuneScapeProfile profile : configManager.getRSProfiles())
		{
			if (profile.getKey().equals(profileKey))
			{
				return characterName(profile);
			}
		}
		return "This character";
	}

	private static String characterName(RuneScapeProfile profile)
	{
		String name = profile.getDisplayName() == null ? "Unnamed character" : profile.getDisplayName();
		return profile.getType() == RuneScapeProfileType.STANDARD ? name : name + " (" + Text.titleCase(profile.getType()) + ")";
	}

	/**
	 * The account's progress per goal id; empty when there's none yet.
	 */
	Map<String, GoalProgress> loadProgress(String profile)
	{
		String json = configManager.getConfiguration(DailyGoalsConfig.GROUP, profile, PROGRESS_KEY);
		savedProgress = json;
		if (json == null)
		{
			return new HashMap<>();
		}

		try
		{
			Map<String, GoalProgress> progress = gson.fromJson(json, PROGRESS_TYPE);
			if (progress != null)
			{
				progress.values().removeIf(p -> p == null || p.target == null);
				progress.values().forEach(p ->
				{
					if (p.history == null)
					{
						p.history = new ArrayList<>();
					}
				});
				return progress;
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Couldn't read saved progress: {}", json, e);
		}
		return new HashMap<>();
	}

	/**
	 * Write the account's progress, if it changed since the last read or write.
	 */
	void saveProgress(String profile, Map<String, GoalProgress> progress)
	{
		String json = gson.toJson(progress);
		if (!json.equals(savedProgress))
		{
			configManager.setConfiguration(DailyGoalsConfig.GROUP, profile, PROGRESS_KEY, json);
			savedProgress = json;
		}
	}

	/**
	 * Forget the last saved progress, e.g. when another account logs in.
	 */
	void clearProgressCache()
	{
		savedProgress = null;
	}

	private List<Goal> parseGoals(@Nullable String json)
	{
		List<Goal> saved = null;
		try
		{
			saved = json == null ? null : gson.fromJson(json, GOAL_LIST_TYPE);
		}
		catch (JsonParseException e)
		{
			log.warn("Couldn't read saved goals: {}", json, e);
		}

		List<Goal> goals = new ArrayList<>();
		if (saved != null)
		{
			for (Goal goal : saved)
			{
				if (goal != null)
				{
					goals.add(fillDefaults(goal));
				}
			}
		}
		return goals;
	}

	/**
	 * Fill in anything an older or hand-edited save left out.
	 */
	private static Goal fillDefaults(Goal goal)
	{
		if (goal.getId() == null)
		{
			goal.setId(UUID.randomUUID().toString());
		}
		if (goal.getType() == null)
		{
			goal.setType(Goal.Type.XP);
		}
		if (goal.getSkill() == null)
		{
			goal.setSkill(Skill.ATTACK);
		}
		if (goal.getActivity() == null)
		{
			goal.setActivity(Activity.OTHER);
		}
		if (goal.getPeriod() == null)
		{
			goal.setPeriod(Goal.Period.DAILY);
		}
		if ((goal.getDays() & Goal.ALL_DAYS) == 0)
		{
			goal.setDays(Goal.ALL_DAYS);
		}
		goal.setAmount(Math.max(1, goal.getAmount()));
		return goal;
	}
}
