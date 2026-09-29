# Dynamic Sword Skills (Unofficial 1.7.10)

**English** | [简体中文](README_zh_CN.md)

An unofficial rebuild of **Dynamic Sword Skills** by coolAlias, targeting
**Minecraft 1.7.10 + Forge 10.13.4.1614**.

The mod for those who just want the combat skills from ZSS and none of the
mobs, items, world gen, etc.

In addition, there are 'skill' items which grant the user use of a skill, even
if the user does not know the skill - these can be found scattered throughout
the land.

## Upstream

This rebuild is based on the **`1.7.10` branch** of the original author's repository:

- Repository: https://github.com/coolAlias/DynamicSwordSkills
- Branch: https://github.com/coolAlias/DynamicSwordSkills/tree/1.7.10
- Original author: coolAlias

**⚠️ The source used here is NOT the same as the final Minecraft 1.7.10 version the
original author published on CurseForge on 2017-10-30.**

The GitHub `1.7.10` branch kept receiving commits after that CurseForge release
(the last one is dated 2020-05-02), and the two are genuinely different code
bases rather than the same build with a few fixes on top. For example, the
CurseForge 2017 build ships 87 classes and still carries a `zeldaswordskills`
entry in its `mcmod.info` dependencies - an incorrect declaration, since the mod
has no required dependencies of its own (its content was split off from Zelda
Sword Skills, but it does not need that mod installed). The branch used here
contains 112 source files and no such entry.

CurseForge page: https://www.curseforge.com/minecraft/mc-mods/dynamic-sword-skills

## Modifications

**Date of modification: 2026-09-29** *(the "relevant date" required by GPLv3 §5(a))*

Everything that differs from the upstream source is listed below. **Entries marked
(default on) change gameplay as soon as the mod is installed**; the rest are off by
default, configurable, or have no gameplay effect.

### Added

**Configuration options** — 17 new settings in the `general` category, all synchronised
from server to client:

| Option | Default | Effect |
|---|---|---|
| `wideSwordBeamHitbox` | **on** | Sword Beam hit detection matches its visual width |
| `swordBeamAnywhere` | **on** | Sword Beam no longer requires standing on the ground |
| `swordBeamNoCooldown` | **on** | No cooldown between Sword Beam uses |
| `swordBeamIgnoreWaterDrag` | **on** | Sword Beam ignores water drag |
| `swordBeamSpeedMultiplier` | 1.0 | Sword Beam flight speed multiplier |
| `swordBeamNoRangeLimit` | off | Removes the Sword Beam lifespan cap |
| `leapingBlowNoAttackRequired` | **on** | Leaping Blow without pressing the attack key |
| `armorBreakBonusDamage` | **on** | Armor Break deals extra damage (+4 tuning values) |
| `dashSpeedDamageBonus` | **on** | Dash damage scales with movement speed (+4 tuning values) |

**Skill changes** (all on by default)

- **Sword Beam** — hit detection widened to match the beam's visual width, reusing the
  vanilla `calculateIntercept` path instead of a custom algorithm.
- **Armor Break** — extra damage that grows with level, taken as the larger of a flat
  amount and a percentage of the attack's damage. The percentage keeps it meaningful
  with high-damage modded weapons, where a flat amount would be negligible. Costs
  additional exhaustion.
- **Dash** — damage now scales with movement speed (Swiftness), and now includes weapon
  damage and enchantment bonuses. Costs additional exhaustion.

**Key bindings** — ported back from the original author's 2017 CurseForge release:

| Key | Action |
|---|---|
| `V` | Toggle the combo HUD |
| `~` | Toggle auto-target (while sneaking, toggles targeting players instead) |

### Fixed

- **Armor Break** — the skill failed to activate when attacking while blocking (holding
  right-click, then pressing attack).
- **Dash** — occasionally dealt no damage despite playing the sound and recoiling.
  Introduced by the 2020 source; not present in the 2017 release.
- **Sword Beam** — flight speed was never recalculated after the skill level was applied,
  so a level 5 beam had the same range as a level 1 beam (~18 blocks). It is now ~30
  blocks at level 5. **This fix is not controlled by any config option.**
- **Skills becoming unusable after a dimension change** — key binding state was lost.
- **Skills becoming unusable after returning from the End** — player skill data was bound
  to an entity that had been removed.
- The auto-target toggle ported from the 2017 release did not save its setting; it now
  persists across restarts.
- Removed a dead VersionChecker link.
- Removed an incorrect `zeldaswordskills` dependency declaration from `mcmod.info`. The
  mod has no required dependencies.

### Changed

- `zh_CN.lang` rewritten — the upstream file was incomplete.
- The optional Battlegear2 integration has been removed entirely, so the source no longer
  references the Battlegear2 API at all. Note that the original mod did not require
  Battlegear2 at runtime either — that integration was optional and was simply skipped
  when the mod was absent.
- Built with the GTNH / RetroFuturaGradle toolchain.

### Not modified

- Skill progression, orb drop rates and loot behaviour keep their upstream defaults.
- The mod id is unchanged (`dynamicswordskills`), so existing worlds keep working.

## Basic Controls

This mod is designed to be used almost entirely with the vanilla control scheme.

Some skills, such as Spin Attack, may require pressing multiple movement keys
at once; **gamepad** users may want to enable the **Additional Controls**
setting in the config which adds WASD-equivalent keybindings that can be mapped
to a separate controller button. Please note that these keys cannot actually be
used to move.

### Custom Keys
- `x` - Activate or deactivate a targeting skill
- `tab` - Switch to the next available target while locked on with a targeting skill
- `p` - Opens the Skill GUI
- `v` - Toggle the combo HUD
- `~` - Toggle auto-target (while sneaking, toggles targeting players instead)

### Optional Additional Controls
- Up Arrow - Equivalent of vanilla `W`
- Down Arrow - Equivalent of vanilla `S`
- Left Arrow - Equivalent of vanilla `A`
- Right Arrow - Equivalent of vanilla `D`

## Commands

`/grantskill <skill | all> <player> <level>`
Grants a single skill or all skills to the target player at the specified level

- If `<level>` is omitted, the command increases the skill(s) level by 1 instead
- If `<player>` is omitted, the command sender will be targeted

`/removeskill <skill | all> <player>`
Removes all levels in a single skill or all skills from the target player

- If `<player>` is omitted, the command sender will be targeted

## Skills

Skills are learned from Skill Orbs, which may drop occasionally from various
creatures or be found in chests, depending on the server configuration settings.

Some creatures are mapped to a particular skill orb, in which case they will
always try to drop an orb, usually their designated one but it may be random
depending on config settings. Mobs not mapped to a particular skill orb may try
to drop a random orb, but even at 100% they are not guaranteed to drop an orb,
just to try.

When a mob tries to drop a skill orb, the base drop chance ranges from 0% to 10%
(default for all is 5%); using weapons with the Looting enchantment
significantly improves the drop rate.

Once a skill is learned, check the in-game Skill GUI (default key to open is
`p`) and click on a skill orb to view information about its effect and how to
activate it.

## Building

This repository does not track the Gradle wrapper jar (all `*.jar` files are
excluded by `.gitignore`). Generate the wrapper once before building:

    gradle wrapper

Then build as usual:

    ./gradlew build          # Linux / macOS
    gradlew.bat build        # Windows

## License

GNU General Public License v3. See the `LICENSE` file for the full text.
