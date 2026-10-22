# Daily Goals

Set daily or weekly goals for XP, levels, boss kills, clues and more. Each goal resets at the time you pick, keeps a streak and a 30-day history, and can show an XP Tracker style overlay and an infobox with what's left.

*This plugin is vibe coded.*

## Using it
Open the target icon in the sidebar and click **Add goal**. Goals belong to the character you're logged in on.

The button under the header shows that character. Click it for a menu of your other characters that have goals. Each one lists its goals with a **+** to copy a goal to this character, and **Add all** copies the lot.

- **Track**: one list, grouped under headings:
  - **Skills**: Overall and every skill. For a skill, choose **Gain XP** or **Reach a level**.
  - **Bosses**, **Raids**, **Minigames & activities**: anything with a kill or completion count, plus Guardians of the Rift rifts and Hallowed Sepulchre floors.
  - **Clue scrolls**: all tiers, or one tier.
  - **Other**: type any name the game uses in its "Your X kill count is" message.
- **Amount**: per day or week. Shorthand like `150k` or `1.5m` works. For a level goal, enter the level.
- **Repeats**: *Daily* on the days you pick (weekdays only, for example), or *Weekly*.
- **Show infobox** / **Show overlay** for this goal.

Each goal gets a card with:
- **Progress bar**: has a tick where an even pace would be, and turns amber when you're behind.
- **Numbers**: done, left (or how far over), XP/h, time to finish, pace and streak.
- **History**: a 30-day (or 30-week) chart. Hover a bar for details.
- **Buttons**:
  - **Edit**: click again to save.
  - **Reset**: starts this period's count over.
  - **Remove**
  - The **switch** turns a goal off without deleting it.
  - The **arrows** change the order.

In game, each goal can have:
- **An overlay**: the XP Tracker's layout, with two lines you choose. Right-click it for **Reset today**.
- **An infobox**: what's left, or `+10K` in green once you're past the goal.

## Settings
**Reset**
- **Reset time**: `HH:MM`, default `04:00`.
- **Weekly reset day**: default Monday.
- **Time zone**: local or UTC. The game's daily reset is 00:00 UTC.
- **Carry over**: none, extra progress, the missed amount, or both.
- **Weekend multiplier**: scales daily goals on Saturday and Sunday; 100% is off.

**Display**
- **Show**: infoboxes, overlays or both.
- **Values**: amounts as XP / count, or as a percentage of the goal. Applies to infoboxes and the overlay's Done, Left and Pace lines.
- **Hide overlay when done** and **Pace colours**.
- **Overlay lines**: Done, Left, Goal, Progress, XP/h, Done in or Pace.
- **Notifications**: when a goal is done, and optionally at 50% / 75%.
- **Chat messages**

## How it counts
- **Per character:** goals, progress, streaks and history are kept per character (RuneScape profile).
- **XP:** counted from your XP at the start of the period. XP gained while the client was closed counts toward the period it's next seen in.
- **Counts:** read from the game's chat messages: kill counts, "You have completed N … Treasure Trails", "Amount of Rifts you have closed" and the Sepulchre floor message. The first message in a period counts as one; after that the plugin counts the change in the total.
- **Streaks:** periods in a row where you reached the goal. Days a goal is set not to run don't break a streak; a day it should have run and you didn't play does.

## How it works
The plugin only reads skill XP and chat messages. It stores everything in RuneLite's config and makes no network requests.

| File | What it does |
|---|---|
| `DailyGoalsPlugin` | Lifecycle, events, the per-tick update, announcements, overlay and infobox wiring |
| `Goal`, `Activity` | A goal as set up by the user; the bosses and activities a count goal can track |
| `GoalProgress` | One account's progress on a goal: the period's count, carry-over, streak, history |
| `GoalSchedule` | When periods start and end under the reset settings |
| `GoalStatus` | A goal's numbers right now (target, left, pace, ETA), worked out from its progress |
| `CountMessage` | Reads counts from chat messages |
| `RateTracker` | XP or count per hour |
| `GoalStore` | Reads and writes each character's goals and progress in config |
| `DailyGoalsOverlay`, `DailyGoalsInfoBox` | The in-game overlay and infobox |
| `DailyGoalsPanel`, `GoalEditor`, `GoalCard`, `TargetPicker`, `HistoryChart` | The sidebar |
| `GoalView`, `PanelStyle`, `Formats` | Panel snapshots, shared Swing helpers, number formatting |

Progress, overlays and infoboxes are only touched on the client thread. The sidebar gets immutable `GoalView` snapshots, and its changes go through config.

## Development
```
./gradlew build   # compile + unit tests
./gradlew run     # launch RuneLite with the plugin loaded (developer mode)
```

The on/off switch and Overall skill icons in `src/main/resources` are copied from RuneLite (BSD 2-Clause).
