package com.dailygoals;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.SkillColor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

/**
 * Daily and weekly goals: XP, levels, boss kills and other counts.
 * <p>
 * Threading: progress, overlays and infoboxes are only touched on the client thread. The
 * sidebar runs on the Swing thread; it changes goals through {@link #saveGoal} and friends,
 * which write config, and gets {@link GoalView} snapshots back from {@link #updatePanel}.
 */
@Slf4j
@PluginDescriptor(
	name = "Daily Goals",
	description = "Daily and weekly XP, level, boss and activity goals with streaks",
	tags = {"xp", "kc", "goal", "daily", "weekly", "boss", "kill", "count", "tracker", "infobox", "streak", "clue"}
)
public class DailyGoalsPlugin extends Plugin
{
	static final Color BEHIND_PACE_COLOR = new Color(230, 160, 40);
	private static final Color COUNT_BAR_COLOR = new Color(161, 40, 40);
	private static final Color OVERALL_BAR_COLOR = new Color(82, 161, 82);
	private static final int[] MILESTONES = {50, 75};
	// Skill XP isn't loaded the moment LOGGED_IN fires; the XP Tracker waits two ticks too
	private static final int LOGIN_SETTLE_TICKS = 2;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DailyGoalsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private GoalStore store;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private Notifier notifier;

	@Inject
	private ChatMessageManager chatMessageManager;

	private DailyGoalsPanel panel;
	private NavigationButton navButton;

	/** The goals, in panel order. Replaced as a whole, so the Swing thread can read it. */
	private volatile List<Goal> goals = Collections.emptyList();

	/** Progress per goal id for the logged-in account; null while logged out. */
	@Nullable
	private Map<String, GoalProgress> progress;
	private String progressProfile;

	/** Per-hour rates for this login, per goal id. */
	private final Map<String, RateTracker> rates = new HashMap<>();
	/** Icons by {@link Goal#targetKey()}. */
	private final Map<String, BufferedImage> icons = new HashMap<>();
	private final Set<String> iconsRequested = new HashSet<>();
	private final Map<String, DailyGoalsOverlay> overlays = new HashMap<>();
	private final Map<String, DailyGoalsInfoBox> infoBoxes = new HashMap<>();
	private List<GoalView> shownViews;
	private int loginTicks;

	@Provides
	DailyGoalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DailyGoalsConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new DailyGoalsPanel(this, skillIconManager, spriteManager);
		navButton = NavigationButton.builder()
			.tooltip("Daily Goals")
			.icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		loginTicks = 0;
		clientThread.invokeLater(() ->
		{
			goals = Collections.unmodifiableList(store.loadGoals());
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				loginTicks = LOGIN_SETTLE_TICKS;
			}
			update();
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			saveProgress();
			clearProgress();
			overlays.values().forEach(overlayManager::remove);
			overlays.clear();
			infoBoxes.values().forEach(infoBoxManager::removeInfoBox);
			infoBoxes.clear();
			icons.clear();
			iconsRequested.clear();
			goals = Collections.emptyList();
			shownViews = null;
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
				saveProgress();
				clearProgress();
				update();
				// fall through
			case LOGGING_IN:
			case HOPPING:
			case CONNECTION_LOST:
				loginTicks = 0;
				break;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Progress is saved as it changes; the next update loads the new account's
		clearProgress();
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		clientThread.invokeLater(() ->
		{
			goals = Collections.unmodifiableList(store.loadGoals());
			update();
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (loginTicks < LOGIN_SETTLE_TICKS)
		{
			loginTicks++;
			return;
		}
		update();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// The same message types the Chat Commands plugin reads kill counts from
		if (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM
			&& event.getType() != ChatMessageType.TRADE
			&& event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION)
		{
			return;
		}

		List<CountMessage> counts = CountMessage.parse(event.getMessage());
		if (counts.isEmpty())
		{
			return;
		}

		update();
		if (progress == null)
		{
			return;
		}

		GoalSchedule schedule = GoalSchedule.of(config);
		for (CountMessage count : counts)
		{
			for (Goal goal : goals)
			{
				GoalProgress goalProgress = progress.get(goal.getId());
				if (goalProgress == null || !goal.counts(count))
				{
					continue;
				}

				if (count.isIncrement())
				{
					goalProgress.add(count.getValue());
				}
				else
				{
					goalProgress.observeTotal(count.getValue());
				}
				announce(goal, goalProgress, schedule);
			}
		}
		saveProgress();
		updatePanel();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!DailyGoalsConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			switch (event.getKey())
			{
				case GoalStore.GOALS_KEY:
					goals = Collections.unmodifiableList(store.loadGoals());
					break;
				case "resetTime":
				case "timeZone":
				case "weeklyResetDay":
					// Moving the reset shouldn't wipe the period in progress: re-date it instead
					moveProgressToCurrentPeriods();
					break;
			}
			update();
		});
	}

	// Called from the panel (Swing thread)

	/**
	 * Add a goal, or replace the one with the same id.
	 */
	void saveGoal(Goal goal)
	{
		if (goal.getId() == null)
		{
			goal.setId(UUID.randomUUID().toString());
		}

		List<Goal> list = new ArrayList<>(goals);
		boolean replaced = false;
		for (int i = 0; i < list.size(); i++)
		{
			if (list.get(i).getId().equals(goal.getId()))
			{
				list.set(i, goal);
				replaced = true;
			}
		}
		if (!replaced)
		{
			list.add(goal);
		}
		writeGoals(list);
	}

	void setGoalEnabled(String id, boolean enabled)
	{
		Goal goal = getGoal(id);
		if (goal != null && goal.isEnabled() != enabled)
		{
			Goal changed = goal.copy();
			changed.setEnabled(enabled);
			saveGoal(changed);
		}
	}

	void removeGoal(String id)
	{
		writeGoals(goals.stream().filter(g -> !g.getId().equals(id)).collect(Collectors.toList()));
	}

	/**
	 * Move a goal up (-1) or down (+1) in the list.
	 */
	void moveGoal(String id, int direction)
	{
		List<Goal> list = new ArrayList<>(goals);
		for (int i = 0; i < list.size(); i++)
		{
			int j = i + direction;
			if (list.get(i).getId().equals(id) && j >= 0 && j < list.size())
			{
				Collections.swap(list, i, j);
				writeGoals(list);
				return;
			}
		}
	}

	/**
	 * Start a goal's count for this period over from now. Also used by the overlay's menu.
	 */
	void restartPeriod(String goalId)
	{
		clientThread.invokeLater(() ->
		{
			GoalProgress goalProgress = getProgress(goalId);
			if (goalProgress != null)
			{
				goalProgress.restartPeriod();
				rates.remove(goalId);
				update();
			}
		});
	}

	// Used by the overlay and infobox (client thread)

	@Nullable
	Goal getGoal(String id)
	{
		for (Goal goal : goals)
		{
			if (goal.getId().equals(id))
			{
				return goal;
			}
		}
		return null;
	}

	/**
	 * A goal's numbers right now, or null when there's nothing to show (logged out).
	 */
	@Nullable
	GoalStatus status(String goalId)
	{
		Goal goal = getGoal(goalId);
		GoalProgress goalProgress = getProgress(goalId);
		return goal == null || goalProgress == null ? null : status(goal, goalProgress, GoalSchedule.of(config));
	}

	@Nullable
	BufferedImage getIcon(Goal goal)
	{
		String key = goal.targetKey();
		return key == null ? null : icons.get(key);
	}

	/**
	 * Bar colour for a goal: the skill's colour (red for counts), amber when behind pace.
	 */
	Color barColor(GoalStatus status)
	{
		return config.paceColors() && status.isBehind() ? BEHIND_PACE_COLOR : baseColor(status.getGoal());
	}

	// Tracking

	/**
	 * Load the account's progress if needed, start a new period for goals whose reset has
	 * passed, record XP, then bring the overlays, infoboxes and panel up to date.
	 */
	private void update()
	{
		String profile = configManager.getRSProfileKey();
		boolean tracking = profile != null
			&& client.getGameState() == GameState.LOGGED_IN
			&& loginTicks >= LOGIN_SETTLE_TICKS;

		if (tracking)
		{
			if (progress == null || !profile.equals(progressProfile))
			{
				progress = store.loadProgress(profile);
				progressProfile = profile;
			}
			track(GoalSchedule.of(config), System.currentTimeMillis());
			saveProgress();
		}

		for (Goal goal : goals)
		{
			requestIcon(goal);
		}
		syncOverlays();
		syncInfoBoxes();
		updatePanel();
	}

	private void track(GoalSchedule schedule, long now)
	{
		List<String> resets = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (Goal goal : goals)
		{
			String target = goal.targetKey();
			if (target == null)
			{
				continue;
			}
			ids.add(goal.getId());

			long periodStart = schedule.periodStart(goal, now);
			GoalProgress goalProgress = progress.get(goal.getId());
			if (goalProgress == null || !target.equals(goalProgress.target))
			{
				// New goal, or it now counts something else
				goalProgress = new GoalProgress(target, periodStart);
				progress.put(goal.getId(), goalProgress);
				rates.remove(goal.getId());
			}
			else if (goalProgress.day < periodStart)
			{
				String carried = rollover(goal, goalProgress, schedule, periodStart);
				if (goal.isEnabled() && carried != null)
				{
					resets.add(carried);
				}
				rates.remove(goal.getId());
			}

			if (goal.isXp())
			{
				goalProgress.observeXp(goal.isOverall() ? client.getOverallExperience() : client.getSkillExperience(goal.getSkill()));
			}
			rates.computeIfAbsent(goal.getId(), id -> new RateTracker(goal.isXp())).observe(goalProgress.gained, now);
			announce(goal, goalProgress, schedule);
		}
		progress.keySet().retainAll(ids);
		rates.keySet().retainAll(ids);

		if (!resets.isEmpty())
		{
			chat("Goals reset." + String.join("", resets));
		}
	}

	/**
	 * Start a new period for a goal.
	 *
	 * @return carry-over details for the reset chat message, "" for none, or null when the
	 * period that ended didn't count
	 */
	@Nullable
	private String rollover(Goal goal, GoalProgress goalProgress, GoalSchedule schedule, long periodStart)
	{
		boolean endedActive = schedule.isActive(goal, goalProgress.day);
		boolean skipped = schedule.missedBetween(goal, goalProgress.day, periodStart);
		long amount = status(goal, goalProgress, schedule).getAmount();
		// Carry-over toward a level makes no sense: the XP still needed is worked out fresh
		DailyGoalsConfig.CarryOver carryOver = goal.isLevel() ? DailyGoalsConfig.CarryOver.OFF : config.carryOver();
		goalProgress.rollover(periodStart, amount, carryOver, endedActive, skipped);
		log.debug("New period for {}: extra {}, missed {}, streak {}", goal.name(), goalProgress.extra, goalProgress.missed, goalProgress.streak);

		if (!endedActive)
		{
			return null;
		}
		if (goalProgress.extra > 0)
		{
			return " " + goal.name() + ": " + Formats.number(goalProgress.extra) + " carried over.";
		}
		if (goalProgress.missed > 0)
		{
			return " " + goal.name() + ": " + Formats.number(goalProgress.missed) + " missed, added to the next goal.";
		}
		return "";
	}

	/**
	 * Announce milestones and completion, once each per period.
	 */
	private void announce(Goal goal, GoalProgress goalProgress, GoalSchedule schedule)
	{
		GoalStatus status = status(goal, goalProgress, schedule);
		boolean quiet = !goal.isEnabled() || !status.isActive();

		if (status.isComplete())
		{
			if (!goalProgress.completed)
			{
				goalProgress.completed = true;
				goalProgress.milestone = 100;
				if (!quiet)
				{
					String streak = status.getStreak() > 1 ? " Streak: " + status.getStreak() + "." : "";
					String message = "Goal complete: " + goal.name() + " (" + Formats.number(status.getDone()) + ")!" + streak;
					notifier.notify(config.notifyComplete(), message);
					chat(message);
				}
			}
			return;
		}

		// The goal was raised after it was reached
		goalProgress.completed = false;

		for (int milestone : MILESTONES)
		{
			if (status.percent() >= milestone && goalProgress.milestone < milestone)
			{
				goalProgress.milestone = milestone;
				if (!quiet)
				{
					String message = goal.name() + ": " + milestone + "% of the goal ("
						+ Formats.number(status.getDone()) + " / " + Formats.number(status.getTarget()) + ")";
					notifier.notify(config.notifyMilestones(), message);
					chat(message);
				}
			}
		}
	}

	private GoalStatus status(Goal goal, GoalProgress goalProgress, GoalSchedule schedule)
	{
		RateTracker rate = rates.get(goal.getId());
		long now = System.currentTimeMillis();
		return GoalStatus.of(goal, goalProgress, schedule, config.weekendMultiplier(), rate == null ? 0 : rate.perHour(now), now);
	}

	private void moveProgressToCurrentPeriods()
	{
		if (progress == null)
		{
			return;
		}

		GoalSchedule schedule = GoalSchedule.of(config);
		long now = System.currentTimeMillis();
		for (Goal goal : goals)
		{
			GoalProgress goalProgress = progress.get(goal.getId());
			if (goalProgress != null)
			{
				goalProgress.day = schedule.periodStart(goal, now);
			}
		}
		saveProgress();
	}

	@Nullable
	private GoalProgress getProgress(String goalId)
	{
		return progress == null ? null : progress.get(goalId);
	}

	private void saveProgress()
	{
		if (progress != null && progressProfile != null)
		{
			store.saveProgress(progressProfile, progress);
		}
	}

	private void clearProgress()
	{
		progress = null;
		progressProfile = null;
		store.clearProgressCache();
		rates.clear();
	}

	private void writeGoals(List<Goal> list)
	{
		goals = Collections.unmodifiableList(list);
		store.saveGoals(list);
	}

	// Display

	private void requestIcon(Goal goal)
	{
		String key = goal.targetKey();
		if (key == null || !iconsRequested.add(key))
		{
			return;
		}

		if (goal.isXp())
		{
			icons.put(key, goal.isOverall() && !goal.isLevel()
				? ImageUtil.loadImageResource(DailyGoalsPlugin.class, "overall.png")
				: skillIconManager.getSkillImage(goal.getSkill()));
			return;
		}

		spriteManager.getSpriteAsync(goal.getActivity().getSpriteId(), 0, sprite ->
			clientThread.invokeLater(() ->
			{
				icons.put(key, sprite);
				update();
			}));
	}

	private void syncOverlays()
	{
		Set<String> wanted = new HashSet<>();
		for (Goal goal : goals)
		{
			if (config.show().isOverlays() && goal.isEnabled() && goal.isOverlay() && goal.targetKey() != null)
			{
				wanted.add(goal.getId());
				overlays.computeIfAbsent(goal.getId(), id ->
				{
					DailyGoalsOverlay overlay = new DailyGoalsOverlay(this, config, id);
					overlayManager.add(overlay);
					return overlay;
				});
			}
		}

		overlays.entrySet().removeIf(entry ->
		{
			if (wanted.contains(entry.getKey()))
			{
				return false;
			}
			overlayManager.remove(entry.getValue());
			return true;
		});
	}

	private void syncInfoBoxes()
	{
		Set<String> wanted = new HashSet<>();
		for (Goal goal : goals)
		{
			String key = goal.targetKey();
			BufferedImage icon = getIcon(goal);
			if (!config.show().isInfoboxes() || !goal.isEnabled() || !goal.isInfobox() || key == null || icon == null)
			{
				continue;
			}
			wanted.add(goal.getId());

			DailyGoalsInfoBox infoBox = infoBoxes.get(goal.getId());
			if (infoBox == null)
			{
				infoBox = new DailyGoalsInfoBox(icon, this, config, goal.getId(), key);
				infoBoxes.put(goal.getId(), infoBox);
				infoBoxManager.addInfoBox(infoBox);
			}
			else if (!key.equals(infoBox.getIconTarget()))
			{
				// The goal now counts something else; show its icon
				infoBox.setImage(icon);
				infoBox.setIconTarget(key);
				infoBoxManager.updateInfoBoxImage(infoBox);
			}
		}

		infoBoxes.entrySet().removeIf(entry ->
		{
			if (wanted.contains(entry.getKey()))
			{
				return false;
			}
			infoBoxManager.removeInfoBox(entry.getValue());
			return true;
		});
	}

	/**
	 * Send the panel fresh snapshots, if anything changed.
	 */
	private void updatePanel()
	{
		GoalSchedule schedule = GoalSchedule.of(config);
		List<GoalView> views = new ArrayList<>();
		for (Goal goal : goals)
		{
			GoalProgress goalProgress = getProgress(goal.getId());
			if (goalProgress == null)
			{
				views.add(GoalView.loggedOut(goal, getIcon(goal), baseColor(goal)));
			}
			else
			{
				GoalStatus status = status(goal, goalProgress, schedule);
				views.add(GoalView.of(status, getIcon(goal), barColor(status), schedule));
			}
		}

		if (!views.equals(shownViews))
		{
			shownViews = views;
			SwingUtilities.invokeLater(() -> panel.showGoals(views));
		}
	}

	private static Color baseColor(Goal goal)
	{
		if (!goal.isXp())
		{
			return COUNT_BAR_COLOR;
		}
		return goal.isOverall() && !goal.isLevel() ? OVERALL_BAR_COLOR : SkillColor.find(goal.getSkill()).getColor();
	}

	private void chat(String message)
	{
		if (!config.chatMessages())
		{
			return;
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(message)
				.build())
			.build());
	}
}
