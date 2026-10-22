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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;

/**
 * Reads and writes the plugin's data in RuneLite's config: the goal list (shared by all
 * accounts) and each account's progress (under its RuneScape profile).
 */
@Slf4j
@Singleton
class GoalStore
{
	static final String GOALS_KEY = "goals";
	private static final String PROGRESS_KEY = "progress";
	private static final Type GOAL_LIST_TYPE = new TypeToken<List<Goal>>()
	{
	}.getType();
	private static final Type PROGRESS_TYPE = new TypeToken<Map<String, GoalProgress>>()
	{
	}.getType();

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

	List<Goal> loadGoals()
	{
		String json = configManager.getConfiguration(DailyGoalsConfig.GROUP, GOALS_KEY);
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

	void saveGoals(List<Goal> goals)
	{
		configManager.setConfiguration(DailyGoalsConfig.GROUP, GOALS_KEY, gson.toJson(goals));
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
