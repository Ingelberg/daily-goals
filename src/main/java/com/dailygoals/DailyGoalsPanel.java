package com.dailygoals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar: a header with "Add goal", the logged-in character with a menu to copy goals from
 * other characters, the goal editor when it's open, and a {@link GoalCard} per goal. Reset,
 * carry-over and display options are on the settings page.
 */
class DailyGoalsPanel extends PluginPanel
{
	private final DailyGoalsPlugin plugin;
	private final GoalEditor editor;
	private final JButton addButton = PanelStyle.smallButton("Add goal");
	/** Shows whose goals these are; opens the copy-from-another-character menu. */
	private final JButton characterButton = PanelStyle.smallButton("");
	/** Holds the editor while it's open; empty otherwise. */
	private final JPanel editorSlot = new JPanel(new DynamicGridLayout(0, 1, 0, 0));
	private final JPanel goalList = new JPanel(new DynamicGridLayout(0, 1, 0, 8));
	private final JLabel noGoalsLabel = PanelStyle.smallLabel("<html>No goals for this character yet. Click <b>Add goal</b>,"
		+ " or copy goals from another character with the menu above.</html>");
	private final JLabel loggedOutLabel = PanelStyle.smallLabel("<html>Log in to see this character's goals.</html>");

	private final List<GoalCard> cards = new ArrayList<>();
	/** The goals the cards were built for, to tell when they need rebuilding. */
	private List<Goal> cardGoals;
	private List<String> cardIds = new ArrayList<>();
	private List<GoalView> shownViews = new ArrayList<>();
	/** The logged-in character's name; null while logged out. */
	@Nullable
	private String character;

	DailyGoalsPanel(DailyGoalsPlugin plugin, SkillIconManager skillIconManager, SpriteManager spriteManager)
	{
		this.plugin = plugin;
		this.editor = new GoalEditor(skillIconManager, spriteManager, this::onEditorSave, this::closeEditor);

		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new DynamicGridLayout(0, 1, 0, 10));

		addButton.addActionListener(e -> openEditor(null));
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.add(PanelStyle.heading("Daily Goals", Color.WHITE), BorderLayout.WEST);
		header.add(addButton, BorderLayout.EAST);

		characterButton.setHorizontalAlignment(SwingConstants.LEFT);
		characterButton.addActionListener(e -> showCharacterMenu());

		editorSlot.setOpaque(false);
		goalList.setOpaque(false);

		add(header);
		add(characterButton);
		add(editorSlot);
		add(goalList);
		add(PanelStyle.smallLabel("<html>Reset time, weekly reset day, carry over and overlay options are in the plugin's settings.</html>"));

		setCharacter(null);
	}

	/**
	 * The character whose goals are shown, or null when logged out.
	 */
	void setCharacter(@Nullable String name)
	{
		character = name;
		characterButton.setText(name == null ? "Not logged in" : name + "  ▾");
		characterButton.setToolTipText(name == null ? null : "These goals belong to " + name + ". Click to copy goals from your other characters");
		characterButton.setEnabled(name != null);
		addButton.setEnabled(name != null);
		if (name == null)
		{
			closeEditor();
		}

		// Rebuild so the empty-list message matches
		cardGoals = null;
		showGoals(shownViews);
	}

	/**
	 * Show the goals and their progress. Cards are rebuilt only when the goals themselves change;
	 * otherwise they're just updated.
	 */
	void showGoals(List<GoalView> views)
	{
		shownViews = views;
		List<Goal> goals = views.stream().map(GoalView::getGoal).collect(Collectors.toList());
		if (!goals.equals(cardGoals))
		{
			cardGoals = goals;
			rebuildCards(goals);
		}

		for (int i = 0; i < cards.size(); i++)
		{
			cards.get(i).update(views.get(i));
		}
		goalList.revalidate();
		goalList.repaint();
	}

	private void rebuildCards(List<Goal> goals)
	{
		cards.clear();
		cardIds = goals.stream().map(Goal::getId).collect(Collectors.toList());
		goalList.removeAll();
		if (goals.isEmpty())
		{
			goalList.add(character == null ? loggedOutLabel : noGoalsLabel);
		}

		for (int i = 0; i < goals.size(); i++)
		{
			GoalCard card = new GoalCard(plugin, goals.get(i), i, goals.size(), this::onEditClicked);
			card.setEditing(editor.isEditing(goals.get(i).getId()));
			cards.add(card);
			goalList.add(card);
		}
	}

	/**
	 * A menu with each other character that has goals; each lists its goals with "+" to copy one
	 * to this character, plus "Add all".
	 */
	private void showCharacterMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		List<GoalStore.CharacterGoals> others = plugin.otherCharacters();
		if (others.isEmpty())
		{
			JMenuItem none = new JMenuItem("No other characters have goals yet");
			none.setEnabled(false);
			menu.add(none);
		}

		for (GoalStore.CharacterGoals other : others)
		{
			JMenu submenu = new JMenu(other.getName());
			JMenuItem all = new JMenuItem("+  Add all (" + other.getGoals().size() + ")");
			all.setToolTipText("Copy all of " + other.getName() + "'s goals to " + character);
			all.addActionListener(e -> plugin.addGoals(other.getGoals()));
			submenu.add(all);
			submenu.addSeparator();

			for (Goal goal : other.getGoals())
			{
				JMenuItem item = new JMenuItem("+  " + goal.name() + summary(goal));
				item.setToolTipText("Copy this goal to " + character);
				item.addActionListener(e -> plugin.addGoals(Collections.singletonList(goal)));
				submenu.add(item);
			}
			menu.add(submenu);
		}

		menu.show(characterButton, 0, characterButton.getHeight());
	}

	/**
	 * " (100K a day)"; nothing for level goals, whose name says it all.
	 */
	private static String summary(Goal goal)
	{
		if (goal.isLevel())
		{
			return "";
		}
		return " (" + Formats.shortNumber(goal.getAmount()) + (goal.isWeekly() ? " a week)" : " a day)");
	}

	/**
	 * Edit opens the editor; clicking it again on the same goal saves and closes it.
	 */
	private void onEditClicked(Goal goal)
	{
		if (editor.isEditing(goal.getId()))
		{
			editor.save();
		}
		else
		{
			openEditor(goal);
		}
	}

	private void openEditor(@Nullable Goal goal)
	{
		editor.open(goal);
		editorSlot.removeAll();
		editorSlot.add(editor);
		updateEditButtons();
		revalidate();
		repaint();
		SwingUtilities.invokeLater(() -> getScrollPane().getVerticalScrollBar().setValue(0));
	}

	private void closeEditor()
	{
		editor.close();
		editorSlot.removeAll();
		updateEditButtons();
		revalidate();
		repaint();
	}

	private void onEditorSave(Goal goal)
	{
		plugin.saveGoal(goal);
		closeEditor();
	}

	private void updateEditButtons()
	{
		for (int i = 0; i < cards.size(); i++)
		{
			cards.get(i).setEditing(editor.isEditing(cardIds.get(i)));
		}
	}
}
