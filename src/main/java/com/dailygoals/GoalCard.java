package com.dailygoals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * One goal in the sidebar: icon and name with on/off switch and reorder arrows, a progress bar,
 * the numbers, a history chart, and Edit / Reset / Remove buttons.
 * <p>
 * The card is built for one {@link Goal} and updated with fresh {@link GoalView}s; the panel
 * builds new cards when the goals themselves change.
 */
class GoalCard extends JPanel
{
	private static final int ICON_SIZE = 25;
	private static final ImageIcon UP_ICON = PanelStyle.arrowIcon(true);
	private static final ImageIcon DOWN_ICON = PanelStyle.arrowIcon(false);
	// Same look as the plugin list's on/off switch
	private static final BufferedImage SWITCH_IMAGE = ImageUtil.loadImageResource(GoalCard.class, "switcher_on.png");
	private static final ImageIcon SWITCH_ON = new ImageIcon(SWITCH_IMAGE);
	private static final ImageIcon SWITCH_OFF = new ImageIcon(ImageUtil.flipImage(
		ImageUtil.luminanceScale(ImageUtil.grayscaleImage(SWITCH_IMAGE), 0.61f), true, false));

	private final boolean enabled;
	private final JLabel icon = new JLabel();
	private final ProgressBar progressBar = new ProgressBar();
	private final JLabel doneValue = PanelStyle.valueLabel();
	private final JLabel leftKey = PanelStyle.smallLabel("Left:");
	private final JLabel leftValue = PanelStyle.valueLabel();
	private final JLabel rateKey = PanelStyle.smallLabel("XP/h:");
	private final JLabel rateValue = PanelStyle.valueLabel();
	private final JLabel etaValue = PanelStyle.valueLabel();
	private final JLabel paceValue = PanelStyle.valueLabel();
	private final JLabel streakValue = PanelStyle.valueLabel();
	private final HistoryChart chart = new HistoryChart();
	private final JLabel notes = PanelStyle.smallLabel(" ");
	private final JButton editButton = PanelStyle.smallButton("Edit");
	private final JButton resetButton = PanelStyle.smallButton("Reset");
	private BufferedImage shownIcon;

	/**
	 * @param onEdit called when Edit is clicked; the panel opens the editor, or saves it when
	 *               this goal is already open
	 */
	GoalCard(DailyGoalsPlugin plugin, Goal goal, int index, int count, Consumer<Goal> onEdit)
	{
		enabled = goal.isEnabled();
		setLayout(new DynamicGridLayout(0, 1, 0, 5));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));

		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		progressBar.setMaximumValue(1000);

		JPanel stats = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
		stats.setOpaque(false);
		stats.add(PanelStyle.statRow(PanelStyle.smallLabel("Done:"), doneValue));
		stats.add(PanelStyle.statRow(leftKey, leftValue));
		stats.add(PanelStyle.statRow(rateKey, rateValue));
		stats.add(PanelStyle.statRow(PanelStyle.smallLabel("Done in:"), etaValue));
		stats.add(PanelStyle.statRow(PanelStyle.smallLabel("Pace:"), paceValue));
		stats.add(PanelStyle.statRow(PanelStyle.smallLabel("Streak:"), streakValue));

		editButton.addActionListener(e -> onEdit.accept(goal));
		resetButton.setToolTipText("Start this period's count over from now");
		resetButton.addActionListener(e -> plugin.restartPeriod(goal.getId()));
		JButton removeButton = PanelStyle.smallButton("Remove");
		removeButton.addActionListener(e ->
		{
			int choice = JOptionPane.showConfirmDialog(this, "Remove the goal \"" + goal.name() + "\"?",
				"Daily Goals", JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION)
			{
				plugin.removeGoal(goal.getId());
			}
		});
		JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
		buttons.setOpaque(false);
		buttons.add(editButton);
		buttons.add(resetButton);
		buttons.add(removeButton);

		add(header(plugin, goal, index, count));
		add(progressBar);
		add(stats);
		add(PanelStyle.smallLabel(goal.isWeekly() ? "Last 30 weeks" : "Last 30 days"));
		add(chart);
		add(notes);
		add(buttons);
		setEditing(false);
	}

	private JPanel header(DailyGoalsPlugin plugin, Goal goal, int index, int count)
	{
		icon.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		icon.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel name = new JLabel(goal.name());
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(enabled ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR);
		name.setToolTipText(goal.name() + (goal.isWeekly() ? " (weekly)" : ""));

		JToggleButton toggle = new JToggleButton(SWITCH_OFF, enabled);
		toggle.setSelectedIcon(SWITCH_ON);
		toggle.setToolTipText(enabled ? "Turn this goal off" : "Turn this goal on");
		SwingUtil.removeButtonDecorations(toggle);
		toggle.addActionListener(e -> plugin.setGoalEnabled(goal.getId(), toggle.isSelected()));

		JButton up = PanelStyle.iconButton(UP_ICON, "Move up", index > 0);
		up.addActionListener(e -> plugin.moveGoal(goal.getId(), -1));
		JButton down = PanelStyle.iconButton(DOWN_ICON, "Move down", index < count - 1);
		down.addActionListener(e -> plugin.moveGoal(goal.getId(), 1));
		JPanel arrows = new JPanel(new GridLayout(1, 2, 2, 0));
		arrows.setOpaque(false);
		arrows.add(up);
		arrows.add(down);

		JPanel controls = new JPanel(new BorderLayout(4, 0));
		controls.setOpaque(false);
		controls.add(toggle, BorderLayout.WEST);
		controls.add(arrows, BorderLayout.EAST);

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.add(icon, BorderLayout.WEST);
		header.add(name, BorderLayout.CENTER);
		header.add(controls, BorderLayout.EAST);
		return header;
	}

	/**
	 * While this goal is in the editor, Edit reads Save.
	 */
	void setEditing(boolean editing)
	{
		editButton.setText(editing ? "Save" : "Edit");
		editButton.setToolTipText(editing ? "Save and close the editor" : "Edit this goal");
	}

	void update(GoalView view)
	{
		if (view.getIcon() != shownIcon)
		{
			shownIcon = view.getIcon();
			icon.setIcon(shownIcon == null ? null : new ImageIcon(ImageUtil.resizeImage(shownIcon, ICON_SIZE, ICON_SIZE)));
		}

		updateProgressBar(view);
		updateStats(view);
		chart.setBars(view.getHistory(), view.getColor());
		resetButton.setEnabled(view.isTracking());

		List<String> lines = new ArrayList<>();
		if (view.getWeekendPercent() > 0)
		{
			lines.add("Weekend goal: " + view.getWeekendPercent() + "%");
		}
		if (view.getExtra() > 0)
		{
			lines.add("Carried over: " + Formats.number(view.getExtra()));
		}
		else if (view.getMissed() > 0)
		{
			lines.add("Missed last time, added: " + Formats.number(view.getMissed()));
		}
		notes.setText(lines.isEmpty() ? " " : "<html>" + String.join("<br>", lines) + "</html>");
	}

	private void updateProgressBar(GoalView view)
	{
		boolean live = view.isTracking() && view.isActive() && enabled;
		int permille = view.getTarget() <= 0 ? 1000 : (int) Math.min(1000, view.getDone() * 1000 / view.getTarget());

		progressBar.setForeground(view.getColor());
		progressBar.setValue(view.isTracking() ? permille : 0);
		progressBar.setLeftLabel("0");
		progressBar.setRightLabel(Formats.shortNumber(view.getTarget()));
		progressBar.setCenterLabel(centerLabel(view));
		// A tick where an even pace would be
		progressBar.setPositions(live && !view.isComplete()
			? Collections.singletonList(Math.max(1, Math.min(999, view.getPacePermille())))
			: Collections.emptyList());
		progressBar.setDimmedText(!enabled ? "Off" : !view.isTracking() ? "Not logged in" : "Rest day");
		progressBar.setDimmed(!live);
	}

	private void updateStats(GoalView view)
	{
		rateKey.setText(view.getGoal().rateLabel() + ":");
		leftValue.setForeground(view.isComplete() ? PanelStyle.GOOD : Color.WHITE);

		if (!view.isTracking())
		{
			for (JLabel label : new JLabel[]{doneValue, leftValue, rateValue, etaValue, paceValue, streakValue})
			{
				label.setText("-");
			}
			paceValue.setForeground(Color.WHITE);
			return;
		}

		doneValue.setText(Formats.number(view.getDone()) + " / " + Formats.number(view.getTarget()));
		if (view.getOver() > 0)
		{
			leftKey.setText("Over:");
			leftValue.setText("+" + Formats.number(view.getOver()));
		}
		else
		{
			leftKey.setText("Left:");
			leftValue.setText(view.isComplete() ? "Done" : Formats.number(view.getLeft()));
		}
		rateValue.setText(Formats.number(view.getRate()));
		etaValue.setText(view.isComplete() ? "Done" : Formats.duration(view.getEtaMillis()));

		if (view.isComplete() || !view.isActive())
		{
			paceValue.setText(view.isComplete() ? "Done" : "Rest day");
			paceValue.setForeground(PanelStyle.GOOD);
		}
		else
		{
			paceValue.setText(Formats.pace(view.getPace()));
			paceValue.setForeground(view.getPace() >= 0 ? PanelStyle.GOOD : DailyGoalsPlugin.BEHIND_PACE_COLOR);
		}

		String best = view.getBestStreak() > view.getStreak() ? " (best " + view.getBestStreak() + ")" : "";
		streakValue.setText(view.getStreak() + best);
	}

	private static String centerLabel(GoalView view)
	{
		if (view.getOver() > 0 && view.getTarget() > 0)
		{
			return String.format("+%.1f%%", 100.0 * view.getOver() / view.getTarget());
		}
		if (view.isComplete())
		{
			return "Done!";
		}
		return String.format("%.1f%%", view.getTarget() <= 0 ? 100 : 100.0 * view.getDone() / view.getTarget());
	}
}
