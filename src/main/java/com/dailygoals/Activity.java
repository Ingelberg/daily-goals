package com.dailygoals;

import java.util.Arrays;
import java.util.Locale;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * Bosses and other activities a count goal can track. Bosses are matched against the name in
 * the game's "Your X kill count is: N" message, with aliases for messages that word the name
 * differently. The other activities have their own messages; see {@link Kind}.
 * <p>
 * Constant names are stored in saved goals, so don't rename them.
 */
enum Activity
{
	ABYSSAL_SIRE(HiscoreSkill.ABYSSAL_SIRE),
	ALCHEMICAL_HYDRA(HiscoreSkill.ALCHEMICAL_HYDRA),
	AMOXLIATL(HiscoreSkill.AMOXLIATL),
	ARAXXOR(HiscoreSkill.ARAXXOR),
	ARTIO(HiscoreSkill.ARTIO),
	BARROWS_CHESTS(HiscoreSkill.BARROWS_CHESTS, "Barrows chest"),
	BRUTUS(HiscoreSkill.BRUTUS),
	BRYOPHYTA(HiscoreSkill.BRYOPHYTA),
	CALLISTO(HiscoreSkill.CALLISTO),
	CALVARION(HiscoreSkill.CALVARION),
	CERBERUS(HiscoreSkill.CERBERUS),
	CHAMBERS_OF_XERIC(HiscoreSkill.CHAMBERS_OF_XERIC),
	CHAMBERS_OF_XERIC_CHALLENGE_MODE(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE),
	CHAOS_ELEMENTAL(HiscoreSkill.CHAOS_ELEMENTAL),
	CHAOS_FANATIC(HiscoreSkill.CHAOS_FANATIC),
	COMMANDER_ZILYANA(HiscoreSkill.COMMANDER_ZILYANA),
	CORPOREAL_BEAST(HiscoreSkill.CORPOREAL_BEAST),
	CRAZY_ARCHAEOLOGIST(HiscoreSkill.CRAZY_ARCHAEOLOGIST),
	DAGANNOTH_PRIME(HiscoreSkill.DAGANNOTH_PRIME),
	DAGANNOTH_REX(HiscoreSkill.DAGANNOTH_REX),
	DAGANNOTH_SUPREME(HiscoreSkill.DAGANNOTH_SUPREME),
	DERANGED_ARCHAEOLOGIST(HiscoreSkill.DERANGED_ARCHAEOLOGIST),
	DOOM_OF_MOKHAIOTL(HiscoreSkill.DOOM_OF_MOKHAIOTL),
	DUKE_SUCELLUS(HiscoreSkill.DUKE_SUCELLUS),
	FRACTURED_ARCHIVE(HiscoreSkill.FRACTURED_ARCHIVE),
	GENERAL_GRAARDOR(HiscoreSkill.GENERAL_GRAARDOR),
	GIANT_MOLE(HiscoreSkill.GIANT_MOLE),
	GROTESQUE_GUARDIANS(HiscoreSkill.GROTESQUE_GUARDIANS),
	HESPORI(HiscoreSkill.HESPORI),
	KALPHITE_QUEEN(HiscoreSkill.KALPHITE_QUEEN),
	KING_BLACK_DRAGON(HiscoreSkill.KING_BLACK_DRAGON),
	KRAKEN(HiscoreSkill.KRAKEN),
	KREEARRA(HiscoreSkill.KREEARRA),
	KRIL_TSUTSAROTH(HiscoreSkill.KRIL_TSUTSAROTH),
	LUNAR_CHESTS(HiscoreSkill.LUNAR_CHESTS, "Lunar Chest"),
	MAD_ANGEL(HiscoreSkill.MAD_ANGEL),
	MAGGOT_KING(HiscoreSkill.MAGGOT_KING),
	MIMIC(HiscoreSkill.MIMIC),
	NEX(HiscoreSkill.NEX),
	NIGHTMARE(HiscoreSkill.NIGHTMARE),
	PHOSANIS_NIGHTMARE(HiscoreSkill.PHOSANIS_NIGHTMARE),
	OBOR(HiscoreSkill.OBOR),
	PHANTOM_MUSPAH(HiscoreSkill.PHANTOM_MUSPAH),
	SARACHNIS(HiscoreSkill.SARACHNIS),
	SCORPIA(HiscoreSkill.SCORPIA),
	SCURRIUS(HiscoreSkill.SCURRIUS),
	SHELLBANE_GRYPHON(HiscoreSkill.SHELLBANE_GRYPHON),
	SKOTIZO(HiscoreSkill.SKOTIZO),
	SOL_HEREDIT(HiscoreSkill.SOL_HEREDIT),
	SPINDEL(HiscoreSkill.SPINDEL),
	TEMPOROSS(HiscoreSkill.TEMPOROSS),
	THE_GAUNTLET(HiscoreSkill.THE_GAUNTLET),
	THE_CORRUPTED_GAUNTLET(HiscoreSkill.THE_CORRUPTED_GAUNTLET),
	THE_HUEYCOATL(HiscoreSkill.THE_HUEYCOATL),
	THE_LEVIATHAN(HiscoreSkill.THE_LEVIATHAN),
	THE_ROYAL_TITANS(HiscoreSkill.THE_ROYAL_TITANS, "Royal Titan"),
	THE_WHISPERER(HiscoreSkill.THE_WHISPERER),
	THEATRE_OF_BLOOD(HiscoreSkill.THEATRE_OF_BLOOD),
	THEATRE_OF_BLOOD_HARD_MODE(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE),
	THERMONUCLEAR_SMOKE_DEVIL(HiscoreSkill.THERMONUCLEAR_SMOKE_DEVIL),
	TOMBS_OF_AMASCUT(HiscoreSkill.TOMBS_OF_AMASCUT),
	TOMBS_OF_AMASCUT_EXPERT(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT),
	TZKAL_ZUK(HiscoreSkill.TZKAL_ZUK),
	TZTOK_JAD(HiscoreSkill.TZTOK_JAD),
	VARDORVIS(HiscoreSkill.VARDORVIS),
	VENENATIS(HiscoreSkill.VENENATIS),
	VETION(HiscoreSkill.VETION),
	VORKATH(HiscoreSkill.VORKATH),
	WINTERTODT(HiscoreSkill.WINTERTODT),
	YAMA(HiscoreSkill.YAMA),
	ZALCANO(HiscoreSkill.ZALCANO),
	ZULRAH(HiscoreSkill.ZULRAH),

	CLUES_ALL("Clue scrolls (all)", Kind.CLUE_ANY, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Clue scrolls"),
	CLUES_BEGINNER("Clue scrolls (beginner)", Kind.CLUE, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Beginner clues"),
	CLUES_EASY("Clue scrolls (easy)", Kind.CLUE, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Easy clues"),
	CLUES_MEDIUM("Clue scrolls (medium)", Kind.CLUE, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Medium clues"),
	CLUES_HARD("Clue scrolls (hard)", Kind.CLUE, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Hard clues"),
	CLUES_ELITE("Clue scrolls (elite)", Kind.CLUE, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Elite clues"),
	CLUES_MASTER("Clue scrolls (master)", Kind.CLUE, HiscoreSkill.CLUE_SCROLL_ALL.getSpriteId(), "Master clues"),
	RIFTS_CLOSED("Rifts closed (GotR)", Kind.RIFTS, HiscoreSkill.RIFTS_CLOSED.getSpriteId(), "Rifts closed"),
	SEPULCHRE_FLOORS("Hallowed Sepulchre floors", Kind.SEPULCHRE, HiscoreSkill.AGILITY.getSpriteId(), "Sepulchre floors"),

	OTHER("Other", Kind.KILL, SpriteID.SideIcons.COMBAT, null);

	/**
	 * Which game message counts toward it. See {@link CountMessage} for the patterns.
	 */
	enum Kind
	{
		/** "Your X kill count is: N." */
		KILL,
		/** "You have completed N medium Treasure Trails." for one tier. */
		CLUE,
		/** The same message for any tier; each one counts as one. */
		CLUE_ANY,
		/** "Amount of Rifts you have closed: N." */
		RIFTS,
		/** "You have completed Floor F of the Hallowed Sepulchre! Total completions: N." */
		SEPULCHRE
	}

	/**
	 * The heading the goal picker lists it under.
	 */
	@RequiredArgsConstructor
	enum Category
	{
		BOSS("Bosses"),
		RAID("Raids"),
		MINIGAME("Minigames & activities"),
		CLUE("Clue scrolls"),
		OTHER("Other");

		@Getter
		private final String label;
	}

	@Getter
	private final String name;
	@Getter
	private final Kind kind;
	@Getter
	private final int spriteId;
	/** What a goal on a non-boss activity counts, e.g. "Medium clues"; null for bosses. */
	@Nullable
	private final String countName;
	/** Normalized names a kill count message may use for this boss. */
	private final String[] keys;

	Activity(HiscoreSkill hiscore, String... aliases)
	{
		this.name = hiscore.getName();
		this.kind = Kind.KILL;
		this.spriteId = hiscore.getSpriteId();
		this.countName = null;
		this.keys = new String[aliases.length + 1];
		for (int i = 0; i < aliases.length; i++)
		{
			keys[i] = normalize(aliases[i]);
		}
		keys[aliases.length] = normalize(hiscore.getName());
	}

	Activity(String name, Kind kind, int spriteId, @Nullable String countName)
	{
		this.name = name;
		this.kind = kind;
		this.spriteId = spriteId;
		this.countName = countName;
		this.keys = new String[0];
	}

	/**
	 * Whether the boss name from a kill count message is this boss.
	 */
	boolean matches(String chatName)
	{
		return kind == Kind.KILL && Arrays.asList(keys).contains(normalize(chatName));
	}

	Category getCategory()
	{
		switch (this)
		{
			case CHAMBERS_OF_XERIC:
			case CHAMBERS_OF_XERIC_CHALLENGE_MODE:
			case THEATRE_OF_BLOOD:
			case THEATRE_OF_BLOOD_HARD_MODE:
			case TOMBS_OF_AMASCUT:
			case TOMBS_OF_AMASCUT_EXPERT:
				return Category.RAID;
			case BARROWS_CHESTS:
			case LUNAR_CHESTS:
			case THE_GAUNTLET:
			case THE_CORRUPTED_GAUNTLET:
			case TEMPOROSS:
			case WINTERTODT:
			case ZALCANO:
			case RIFTS_CLOSED:
			case SEPULCHRE_FLOORS:
				return Category.MINIGAME;
			case OTHER:
				return Category.OTHER;
			default:
				return kind == Kind.CLUE || kind == Kind.CLUE_ANY ? Category.CLUE : Category.BOSS;
		}
	}

	/**
	 * The clue tier word in the completion message ("medium"), for single-tier clue goals.
	 */
	@Nullable
	String clueTier()
	{
		return kind == Kind.CLUE && countName != null ? countName.split(" ")[0].toLowerCase(Locale.ROOT) : null;
	}

	/**
	 * What a goal on this counts, e.g. "Zulrah kills" or "Medium clues".
	 */
	String goalName(String customName)
	{
		if (countName != null)
		{
			return countName;
		}
		String boss = this == OTHER ? customName : name;
		return (boss.isEmpty() ? "Boss" : boss) + " kills";
	}

	/**
	 * The goal picker shows the full name; "Other" asks for a name.
	 */
	String pickerName()
	{
		return this == OTHER ? "Other (type a name)" : name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Lower case, no leading "the", letters and digits only, so "Kree'arra" == "Kree'Arra" and
	 * "Gauntlet" == "The Gauntlet".
	 */
	static String normalize(String name)
	{
		String s = name.toLowerCase(Locale.ROOT).trim();
		if (s.startsWith("the "))
		{
			s = s.substring(4);
		}
		return s.replaceAll("[^a-z0-9]", "");
	}
}
