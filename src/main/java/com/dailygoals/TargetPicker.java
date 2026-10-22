package com.dailygoals;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

/**
 * The goal editor's "Track" list: Overall and every skill, then each kind of activity, under
 * headings that can't be picked. Items are {@link #OVERALL}, {@link Skill}s and {@link Activity}s.
 */
class TargetPicker extends JComboBox<Object>
{
	/** Total XP; {@code Skill.OVERALL} isn't a usable skill. */
	static final String OVERALL = "Overall";
	private static final int ICON_SIZE = 16;

	private final SkillIconManager skillIconManager;
	private final SpriteManager spriteManager;
	private final Map<Activity, ImageIcon> activityIcons = new EnumMap<>(Activity.class);

	TargetPicker(SkillIconManager skillIconManager, SpriteManager spriteManager)
	{
		super(new HeadingModel(choices()));
		this.skillIconManager = skillIconManager;
		this.spriteManager = spriteManager;
		setMaximumRowCount(16);
		setRenderer(new Renderer());
	}

	/**
	 * Select what a goal tracks.
	 */
	void select(Goal goal)
	{
		if (!goal.isXp())
		{
			setSelectedItem(goal.getActivity());
		}
		else
		{
			setSelectedItem(goal.isOverall() && !goal.isLevel() ? OVERALL : goal.getSkill());
		}
	}

	boolean isActivitySelected()
	{
		return getSelectedItem() instanceof Activity;
	}

	/**
	 * The selected skill, or null for Overall or an activity.
	 */
	@Nullable
	Skill selectedSkill()
	{
		return getSelectedItem() instanceof Skill ? (Skill) getSelectedItem() : null;
	}

	@Nullable
	Activity selectedActivity()
	{
		return isActivitySelected() ? (Activity) getSelectedItem() : null;
	}

	private static Object[] choices()
	{
		List<Object> choices = new ArrayList<>();
		choices.add(new Heading("Skills"));
		choices.add(OVERALL);
		Collections.addAll(choices, (Object[]) Skill.values());
		for (Activity.Category category : Activity.Category.values())
		{
			choices.add(new Heading(category.getLabel()));
			for (Activity activity : Activity.values())
			{
				if (activity.getCategory() == category)
				{
					choices.add(activity);
				}
			}
		}
		return choices.toArray();
	}

	@Nullable
	private ImageIcon icon(Object item)
	{
		if (item instanceof Activity)
		{
			return activityIcon((Activity) item);
		}
		if (item instanceof Skill)
		{
			return new ImageIcon(ImageUtil.resizeImage(skillIconManager.getSkillImage((Skill) item, true), ICON_SIZE, ICON_SIZE));
		}
		if (OVERALL.equals(item))
		{
			BufferedImage image = ImageUtil.loadImageResource(TargetPicker.class, "overall_small.png");
			return new ImageIcon(ImageUtil.resizeImage(image, ICON_SIZE, ICON_SIZE));
		}
		return null;
	}

	/**
	 * Activity icons are game sprites, which load in the background.
	 */
	@Nullable
	private ImageIcon activityIcon(Activity activity)
	{
		if (!activityIcons.containsKey(activity))
		{
			activityIcons.put(activity, null);
			spriteManager.getSpriteAsync(activity.getSpriteId(), 0, sprite ->
				SwingUtilities.invokeLater(() ->
				{
					activityIcons.put(activity, new ImageIcon(ImageUtil.resizeImage(sprite, ICON_SIZE, ICON_SIZE)));
					repaint();
				}));
		}
		return activityIcons.get(activity);
	}

	@Value
	private static class Heading
	{
		String text;
	}

	/**
	 * Skips headings: selecting one, or stepping onto one with the arrow keys, moves on to the
	 * nearest item in the direction of travel.
	 */
	private static class HeadingModel extends DefaultComboBoxModel<Object>
	{
		HeadingModel(Object[] items)
		{
			super(items);
		}

		@Override
		public void setSelectedItem(Object item)
		{
			if (!(item instanceof Heading))
			{
				super.setSelectedItem(item);
				return;
			}

			int index = getIndexOf(item);
			int step = index < getIndexOf(getSelectedItem()) ? -1 : 1;
			for (int i = index + step; i >= 0 && i < getSize(); i += step)
			{
				if (!(getElementAt(i) instanceof Heading))
				{
					super.setSelectedItem(getElementAt(i));
					return;
				}
			}
		}
	}

	private class Renderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			boolean heading = value instanceof Heading;
			super.getListCellRendererComponent(list, value, index, isSelected && !heading, cellHasFocus);
			setIcon(icon(value));

			if (heading)
			{
				setText(((Heading) value).getText());
				setFont(FontManager.getRunescapeBoldFont());
				setForeground(ColorScheme.BRAND_ORANGE);
				setBorder(new EmptyBorder(6, 4, 2, 4));
				return this;
			}

			if (value instanceof Skill)
			{
				setText(((Skill) value).getName());
			}
			else if (value instanceof Activity)
			{
				setText(((Activity) value).pickerName());
			}
			setFont(list.getFont());
			// Items are indented under their heading in the open list, but not in the closed box
			setBorder(new EmptyBorder(2, index < 0 ? 4 : 12, 2, 4));
			return this;
		}
	}
}
