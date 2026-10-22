package com.dailygoals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar: a header with "Add goal", the goal editor when it's open, and a {@link GoalCard}
 * per goal. Reset, carry-over and display options are on the settings page.
 */
class DailyGoalsPanel extends PluginPanel
{
	private final DailyGoalsPlugin plugin;
	private final GoalEditor editor;
	/** Holds the editor while it's open; empty otherwise. */
	private final JPanel editorSlot = new JPanel(new DynamicGridLayout(0, 1, 0, 0));
	private final JPanel goalList = new JPanel(new DynamicGridLayout(0, 1, 0, 8));
	private final JLabel noGoalsLabel = PanelStyle.smallLabel("<html>No goals yet. Click <b>Add goal</b> to set one up.</html>");
	private final JLabel loggedOutLabel = PanelStyle.smallLabel("<html>Log in to track today's progress.</html>");

	private final List<GoalCard> cards = new ArrayList<>();
	/** The goals the cards were built for, to tell when they need rebuilding. */
	private List<Goal> cardGoals;
	private boolean cardsTracking;
	private List<String> cardIds = new ArrayList<>();

	DailyGoalsPanel(DailyGoalsPlugin plugin, SkillIconManager skillIconManager, SpriteManager spriteManager)
	{
		this.plugin = plugin;
		this.editor = new GoalEditor(skillIconManager, spriteManager, this::onEditorSave, this::closeEditor);

		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new DynamicGridLayout(0, 1, 0, 10));

		JButton addButton = PanelStyle.smallButton("Add goal");
		addButton.addActionListener(e -> openEditor(null));
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.add(PanelStyle.heading("Daily Goals", Color.WHITE), BorderLayout.WEST);
		header.add(addButton, BorderLayout.EAST);

		editorSlot.setOpaque(false);
		goalList.setOpaque(false);

		add(header);
		add(editorSlot);
		add(goalList);
		add(PanelStyle.smallLabel("<html>Reset time, weekly reset day, carry over and overlay options are in the plugin's settings.</html>"));

		showGoals(new ArrayList<>());
	}

	/**
	 * Show the goals and their progress. Cards are rebuilt only when the goals themselves change
	 * (or on logging in or out); otherwise they're just updated.
	 */
	void showGoals(List<GoalView> views)
	{
		List<Goal> goals = views.stream().map(GoalView::getGoal).collect(Collectors.toList());
		boolean tracking = views.stream().anyMatch(GoalView::isTracking);

		if (!goals.equals(cardGoals) || tracking != cardsTracking)
		{
			cardGoals = goals;
			cardsTracking = tracking;
			rebuildCards(goals, tracking);
		}

		for (int i = 0; i < cards.size(); i++)
		{
			cards.get(i).update(views.get(i));
		}
		goalList.revalidate();
		goalList.repaint();
	}

	private void rebuildCards(List<Goal> goals, boolean tracking)
	{
		cards.clear();
		cardIds = goals.stream().map(Goal::getId).collect(Collectors.toList());
		goalList.removeAll();
		if (goals.isEmpty())
		{
			goalList.add(noGoalsLabel);
		}
		else if (!tracking)
		{
			goalList.add(loggedOutLabel);
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
