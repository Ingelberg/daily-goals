package com.dailygoals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * The form to add or edit a goal. Rows that don't apply (the XP/level choice, the custom name,
 * the days) are swapped in and out as the choices change.
 */
class GoalEditor extends JPanel
{
	private static final int DEFAULT_XP_AMOUNT = 100_000;
	private static final int DEFAULT_COUNT_AMOUNT = 10;
	private static final int DEFAULT_LEVEL = 99;
	private static final String[] DAY_LETTERS = {"M", "T", "W", "T", "F", "S", "S"};
	private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

	private final Consumer<Goal> onSave;
	private final Runnable onCancel;

	private final JLabel title = PanelStyle.heading("", ColorScheme.BRAND_ORANGE);
	private final TargetPicker targetPicker;
	private final JComboBox<Goal.Type> xpTypeBox = new JComboBox<>(new Goal.Type[]{Goal.Type.XP, Goal.Type.LEVEL});
	private final JTextField customNameInput = new JTextField();
	private final JLabel amountLabel = PanelStyle.smallLabel("");
	private final JTextField amountField = new JTextField();
	private final JComboBox<Goal.Period> periodBox = new JComboBox<>(Goal.Period.values());
	private final JToggleButton[] dayButtons = new JToggleButton[7];
	private final JCheckBox infoboxCheck = new JCheckBox("Show infobox (amount left)");
	private final JCheckBox overlayCheck = new JCheckBox("Show overlay");
	private final JLabel error = PanelStyle.smallLabel("");

	/** Rows that come and go: the XP/level choice or the custom name. */
	private final JPanel targetRows = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
	/** "Repeats", plus the days for daily goals. */
	private final JPanel scheduleRows = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
	private final JPanel xpTypeField = PanelStyle.field("Goal", xpTypeBox);
	private final JPanel customNameField = PanelStyle.field("Boss name (as in the kill count message)", customNameInput);
	private final JPanel periodField = PanelStyle.field("Repeats", periodBox);
	private final JPanel daysField;

	/** The goal being edited (a copy), or null when the form is closed. */
	@Nullable
	private Goal editing;

	GoalEditor(SkillIconManager skillIconManager, SpriteManager spriteManager, Consumer<Goal> onSave, Runnable onCancel)
	{
		this.onSave = onSave;
		this.onCancel = onCancel;
		this.targetPicker = new TargetPicker(skillIconManager, spriteManager);

		setLayout(new DynamicGridLayout(0, 1, 0, 6));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 2, 0, 0, ColorScheme.BRAND_ORANGE),
			new EmptyBorder(8, 8, 8, 8)));

		JPanel days = new JPanel(new GridLayout(1, 7, 2, 0));
		days.setOpaque(false);
		for (int i = 0; i < 7; i++)
		{
			DayButton day = new DayButton(DAY_LETTERS[i], DAY_NAMES[i]);
			dayButtons[i] = day;
			days.add(day);
		}
		daysField = PanelStyle.field("On these days", days);

		targetPicker.addActionListener(e -> onTypeChanged());
		xpTypeBox.addActionListener(e -> onTypeChanged());
		periodBox.addActionListener(e -> updateRows());
		amountField.addActionListener(e -> save());
		customNameInput.addActionListener(e -> save());
		infoboxCheck.setOpaque(false);
		overlayCheck.setOpaque(false);
		error.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);

		JPanel amount = new JPanel(new BorderLayout(0, 2));
		amount.setOpaque(false);
		amount.add(amountLabel, BorderLayout.NORTH);
		amount.add(amountField, BorderLayout.CENTER);

		JButton saveButton = PanelStyle.smallButton("Save");
		saveButton.addActionListener(e -> save());
		JButton cancelButton = PanelStyle.smallButton("Cancel");
		cancelButton.addActionListener(e -> onCancel.run());
		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(saveButton);
		buttons.add(cancelButton);

		targetRows.setOpaque(false);
		scheduleRows.setOpaque(false);
		add(title);
		add(PanelStyle.field("Track", targetPicker));
		add(targetRows);
		add(amount);
		add(scheduleRows);
		add(infoboxCheck);
		add(overlayCheck);
		add(error);
		add(buttons);
	}

	/**
	 * Fill the form from a goal, or with defaults for a new one.
	 */
	void open(@Nullable Goal goal)
	{
		editing = goal == null ? new Goal() : goal.copy();
		title.setText(goal == null ? "New goal" : "Edit goal");

		targetPicker.select(editing);
		xpTypeBox.setSelectedItem(editing.isLevel() ? Goal.Type.LEVEL : Goal.Type.XP);
		customNameInput.setText(editing.getCustomName());
		amountField.setText(editing.isLevel() ? String.valueOf(editing.getAmount()) : QuantityFormatter.formatNumber(editing.getAmount()));
		periodBox.setSelectedItem(editing.getPeriod());
		for (int i = 0; i < 7; i++)
		{
			dayButtons[i].setSelected((editing.getDays() & (1 << i)) != 0);
		}
		infoboxCheck.setSelected(editing.isInfobox());
		overlayCheck.setSelected(editing.isOverlay());
		error.setText("");
		updateRows();
	}

	boolean isEditing(String goalId)
	{
		return editing != null && goalId.equals(editing.getId());
	}

	void close()
	{
		editing = null;
	}

	/**
	 * Check the form and hand the goal to {@code onSave}; shows an error instead if something's off.
	 */
	void save()
	{
		if (editing == null)
		{
			return;
		}

		Goal.Type type = selectedType();
		Skill skill = targetPicker.selectedSkill();
		Activity activity = targetPicker.selectedActivity();
		String customName = customNameInput.getText().trim();
		Goal.Period period = (Goal.Period) periodBox.getSelectedItem();
		Integer amount = Goal.parseAmount(amountField.getText());
		int days = selectedDays();

		String problem = problem(type, skill, activity, customName, amount, period, days);
		if (problem != null)
		{
			error.setText("<html>" + problem + "</html>");
			return;
		}

		editing.setType(type);
		editing.setOverall(type == Goal.Type.XP && skill == null);
		if (skill != null)
		{
			editing.setSkill(skill);
		}
		if (activity != null)
		{
			editing.setActivity(activity);
		}
		editing.setCustomName(customName);
		editing.setAmount(amount);
		editing.setPeriod(period);
		editing.setDays(period == Goal.Period.DAILY ? days : Goal.ALL_DAYS);
		editing.setInfobox(infoboxCheck.isSelected());
		editing.setOverlay(overlayCheck.isSelected());
		onSave.accept(editing);
	}

	@Nullable
	private static String problem(Goal.Type type, @Nullable Skill skill, @Nullable Activity activity, String customName,
		@Nullable Integer amount, Goal.Period period, int days)
	{
		if (type == Goal.Type.LEVEL)
		{
			if (skill == null)
			{
				return "Reach a level needs a skill, not Overall.";
			}
			if (amount == null || amount < 2 || amount > Experience.MAX_VIRT_LEVEL)
			{
				return "Enter a level from 2 to " + Experience.MAX_VIRT_LEVEL + ".";
			}
		}
		else if (amount == null)
		{
			return "Enter an amount, like 150000 or 150k.";
		}

		if (activity == Activity.OTHER && customName.isEmpty())
		{
			return "Type the boss name.";
		}
		if (period == Goal.Period.DAILY && days == 0)
		{
			return "Pick at least one day.";
		}
		return null;
	}

	/**
	 * A boss or activity is a count; a skill is XP or a level, as picked.
	 */
	private Goal.Type selectedType()
	{
		if (targetPicker.isActivitySelected())
		{
			return Goal.Type.COUNT;
		}
		return xpTypeBox.getSelectedItem() == Goal.Type.LEVEL ? Goal.Type.LEVEL : Goal.Type.XP;
	}

	private int selectedDays()
	{
		int days = 0;
		for (int i = 0; i < 7; i++)
		{
			days |= dayButtons[i].isSelected() ? 1 << i : 0;
		}
		return days;
	}

	private void onTypeChanged()
	{
		// Swap an untouched default amount for one that suits the goal type
		Integer amount = Goal.parseAmount(amountField.getText());
		boolean untouched = amount != null && (amount == DEFAULT_XP_AMOUNT || amount == DEFAULT_COUNT_AMOUNT || amount == DEFAULT_LEVEL);
		if (untouched)
		{
			switch (selectedType())
			{
				case LEVEL:
					amountField.setText(String.valueOf(DEFAULT_LEVEL));
					break;
				case COUNT:
					amountField.setText(String.valueOf(DEFAULT_COUNT_AMOUNT));
					break;
				default:
					amountField.setText(QuantityFormatter.formatNumber(DEFAULT_XP_AMOUNT));
					break;
			}
		}
		updateRows();
	}

	private void updateRows()
	{
		Goal.Type type = selectedType();
		boolean weekly = periodBox.getSelectedItem() == Goal.Period.WEEKLY;

		targetRows.removeAll();
		if (type != Goal.Type.COUNT)
		{
			targetRows.add(xpTypeField);
		}
		else if (targetPicker.selectedActivity() == Activity.OTHER)
		{
			targetRows.add(customNameField);
		}

		scheduleRows.removeAll();
		scheduleRows.add(periodField);
		if (!weekly)
		{
			scheduleRows.add(daysField);
		}

		if (type == Goal.Type.LEVEL)
		{
			amountLabel.setText("Target level");
			amountField.setToolTipText("The level to reach, 2 to " + Experience.MAX_VIRT_LEVEL);
		}
		else
		{
			amountLabel.setText(weekly ? "Weekly amount" : "Daily amount");
			amountField.setToolTipText("XP or count per " + (weekly ? "week" : "day") + ". Shorthand works: 150k, 1.5m");
		}

		revalidate();
		repaint();
	}

	/**
	 * A day toggle painted green when the goal counts that day and red when it doesn't. Painted
	 * by hand because the client's look and feel ignores toggle button background colours.
	 */
	private static class DayButton extends JToggleButton
	{
		private static final Color ON = new Color(46, 125, 50);
		private static final Color OFF = new Color(140, 45, 45);

		private final String dayName;

		DayButton(String letter, String dayName)
		{
			super(letter, true);
			this.dayName = dayName;
			setFont(FontManager.getRunescapeSmallFont());
			setFocusPainted(false);
			setRolloverEnabled(true);
			setPreferredSize(new Dimension(20, 18));
			updateTooltip();
			addItemListener(e -> updateTooltip());
		}

		private void updateTooltip()
		{
			setToolTipText(dayName + ": " + (isSelected() ? "on" : "off"));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Color color = isSelected() ? ON : OFF;
			g.setColor(getModel().isRollover() ? color.brighter() : color);
			g.fillRect(0, 0, getWidth(), getHeight());

			g.setFont(getFont());
			g.setColor(Color.WHITE);
			FontMetrics metrics = g.getFontMetrics();
			int x = (getWidth() - metrics.stringWidth(getText())) / 2;
			int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
			g.drawString(getText(), x, y);
		}

		@Override
		protected void paintBorder(Graphics g)
		{
			// The colour is the border
		}
	}
}
