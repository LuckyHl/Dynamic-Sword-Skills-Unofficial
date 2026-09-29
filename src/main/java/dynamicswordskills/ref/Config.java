/**
    Copyright (C) <2017> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.ref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.api.WeaponRegistry;
import dynamicswordskills.client.gui.IGuiOverlay.HALIGN;
import dynamicswordskills.client.gui.IGuiOverlay.VALIGN;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.client.SyncConfigPacket;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

public class Config
{
	/** Config ID for GuiConfig */
	public static final String CONFIG_ID = "dss.config";
	public static Configuration config;
	/** Flag set after {@link #postInit()} has been called */
	private static boolean loaded;
	/*================== CLIENT SIDE SETTINGS =====================*/
	/* General client settings */
	private static boolean enableAdditionalControls;
	private static boolean enableAutoTarget;
	private static boolean enableTargetPassive;
	private static boolean enableTargetPlayer;
	private static boolean allowVanillaControls;
	private static boolean requireDoubleTap;
	private static boolean requireLockOn;
	/* Skill Manual GUI */
	private static boolean clickedGroupFilterSound;
	private static boolean clickedPageSound;
	private static boolean clickedSkillSound;
	private static boolean showBannedSkills;
	private static boolean showPaginationLabels;
	private static boolean showPlainTextIndex;
	private static boolean showSkillGroupTooltips;
	private static boolean showUnknownSkills;
	private static Map<String, Set<String>> skillGroupLists = Maps.<String, Set<String>>newHashMap();
	/* Combo HUD */
	public static boolean isComboHudEnabled;
	public static int comboHudDisplayTime;
	private static int comboHudMaxHits;
	public static HALIGN comboHudXAlign;
	public static VALIGN comboHudYAlign;
	public static int comboHudXOffset;
	public static int comboHudYOffset;
	/* Ending Blow HUD */
	public static int endingBlowHudDisplayTime;
	public static boolean endingBlowHudResult;
	public static boolean endingBlowHudText;
	public static HALIGN endingBlowHudXAlign;
	public static VALIGN endingBlowHudYAlign;
	public static int endingBlowHudXOffset;
	public static int endingBlowHudYOffset;
	/*================== WEAPON REGISTRY =====================*/
	/** Items that are considered Swords for all intents and purposes */
	private static String[] swords = new String[0];
	/** Items that are considered Melee Weapons for all intents and purposes */
	private static String[] weapons = new String[0];
	/** Items that are forbidden from being considered as Swords */
	private static String[] forbidden_swords = new String[0];
	/** Items that are forbidden from being considered as Melee Weapons */
	private static String[] forbidden_weapons = new String[0];
	/*================== SERVER =====================*/
	/* General server settings */
	private static boolean backSliceDisarmorPlayer;
	private static Set<String> bannedSkills = Sets.<String>newHashSet();
	private static int baseSwingSpeed;
	private static float parryDisarmTimingBonus;
	private static float parryDisarmPenalty;
	private static boolean requireFullHealth;
	private static boolean risingCutHighJump;
	private static boolean skillSwordCreative;
	private static int skillSwordCreativeLevel;
	private static boolean skillSwordRandom;
	/* Loot / drops settings */
	private static boolean bonusOrbEnable;
	private static int orbLootWeight;
	private static Map<Integer, Float> orbDropChance;
	private static boolean orbDropEnable;
	private static float orbDropGeneralChance;
	private static float orbDropRandomChance;
	private static boolean playerDropEnable;
	private static int playerDropFactor;
	private static int skillSwordLootWeight;
	/* Sword Beam settings, synchronized to clients as part of SyncConfigPacket */
	private static boolean wideSwordBeamHitbox = true;
	private static boolean swordBeamAnywhere = true;
	private static boolean swordBeamNoCooldown = true;
	private static boolean swordBeamNoRangeLimit;
	private static float swordBeamSpeedMultiplier = 1.0F;
	private static boolean swordBeamIgnoreWaterDrag = true;
	private static boolean leapingBlowNoAttackRequired = true;
	private static boolean armorBreakBonusDamage = true;
	private static float armorBreakBonusDamagePerLevel = 3.0F;
	private static float armorBreakBonusDamageRatioPerLevel = 0.3F;
	private static float armorBreakBonusDamageExtraExhaustion = 7.5F;
	private static float armorBreakBonusDamageExtraExhaustionPerLevel = 0.45F;
	private static boolean dashSpeedDamageBonus = true;
	private static float dashSpeedDamageBonusMultiplier = 5.0F;
	private static float dashSpeedDamageExtraExhaustion = 7.1F;
	private static float dashSpeedDamageExtraExhaustionPerLevel = -0.3F;
	private static float dashSpeedDamageRatioPerLevel = 0.3F;

	public static void init(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		refreshClient();
		refreshServer();
		// The Sword Beam switches are read only in applyServerSettings, so this call is what
		// registers their keys with the Configuration on startup. Without it a freshly generated
		// config file would never contain them, and the in-game config GUI - which simply lists
		// whatever keys exist in the Configuration - would not show them either.
		applyServerSettings();
	}

	public static void refreshClient() {
		/* General client settings */
		enableAdditionalControls = config.get("client", "dss.config.client.enableAdditionalControls", false, "Enables additional WASD-equivalent keybindings for activating skills with e.g. a gamepad").setRequiresMcRestart(true).getBoolean(false);
		enableAutoTarget = config.get("client", "dss.config.client.enableAutoTarget", true, "Enable auto-targeting when locked on and the current target becomes invalid").getBoolean(true);
		enableTargetPassive = config.get("client", "dss.config.client.enableTargetPassive", true, "Allow targeting passive mobs with the lock-on mechanic").getBoolean(true);
		enableTargetPlayer = config.get("client", "dss.config.client.enableTargetPlayer", true, "Allow targeting players with the lock-on mechanic").getBoolean(true);
		allowVanillaControls = config.get("client", "dss.config.client.enableVanillaControls", true, "Allow vanilla movement keys to be used to activate skills; must be enabled if Additional Controls are disabled").getBoolean(true);
		if (!enableAdditionalControls && !allowVanillaControls) {
			DynamicSwordSkills.logger.warn("Both Vanilla and Additional Controls are disabled - Vanilla Controls were automatically enabled");
			allowVanillaControls = true;
		}
		requireDoubleTap = config.get("client", "dss.config.client.requireDoubleTap", true, "Require double-tap for Dodge and Parry (always required when Vanilla Controls are enabled)").getBoolean(true);
		requireLockOn = config.get("client", "dss.config.client.requireLockOn", false, "Require locking on to activate skills").getBoolean(false);
		/* Skill Manual GUI */
		clickedGroupFilterSound = config.get("skillgui", "dss.config.client.skillGui.clickedGroupFilterSound", true, "Play a sound when applying or removing a Skill Group filter").getBoolean(true);
		clickedPageSound = config.get("skillgui", "dss.config.client.skillGui.clickedPageSound", true, "Play a sound when the page index changes").getBoolean(true);
		clickedSkillSound = config.get("skillgui", "dss.config.client.skillGui.clickedSkillSound", true, "Play a sound when clicking on a Skill entry").getBoolean(true);
		showBannedSkills = config.get("skillgui", "dss.config.client.skillGui.showBannedSkills", false, "Display entries in the Skill Manual for skills disabled by the server").getBoolean(false);
		showPaginationLabels = config.get("skillgui", "dss.config.client.skillGui.showPaginationLabels", true, "Display text labels for 'Prev' and 'Next' page buttons").getBoolean(true);
		showPlainTextIndex = config.get("skillgui", "dss.config.client.skillGui.showPlainTextIndex", true, "Display table of contents without the standard button texture").getBoolean(true);
		showSkillGroupTooltips= config.get("skillgui", "dss.config.client.skillGui.showSkillGroupTooltips", true, "Display tooltips when hovering over the Table of Contents entries for Skill Groups that support them").getBoolean(true);
		showUnknownSkills = config.get("skillgui", "dss.config.client.skillGui.showUnknownSkills", true, "Display entries in the Skill Manual for skills not yet learned").getBoolean(true);
		if (Config.loaded) {
			refreshSkillGroups();
		}
		/* Combo HUD */
		String[] xalign = {"left", "center", "right"};
		String[] yalign = {"top", "center", "bottom"};
		isComboHudEnabled = config.get("combohud", "dss.config.client.comboHud.enableDisplay", true, "Display the Combo HUD; can also be toggled in-game with the Toggle Combo HUD key binding").getBoolean(true);
		comboHudDisplayTime = config.get("combohud", "dss.config.client.comboHud.displayTime", 5000, "Number of milliseconds Combo HUD will remain on screen (0 to disable)", 0, 20000).getInt();
		comboHudMaxHits = config.get("combohud", "dss.config.client.comboHud.maxHits", 3, "Maximum number of recent hits to display [0-12]", 0, 12).getInt();
		comboHudXAlign = HALIGN.fromString(config.get("combohud", "dss.config.client.comboHud.xalign", "left", "Base HUD alignment on the X-Axis").setValidValues(xalign).getString());
		comboHudXOffset = config.get("combohud", "dss.config.client.comboHud.xoffset", 0, "Number of pixels to offset HUD alignment on the X-Axis").getInt();
		comboHudYAlign = VALIGN.fromString(config.get("combohud", "dss.config.client.comboHud.yalign", "top", "Base HUD alignment on the Y-Axis").setValidValues(yalign).getString());
		comboHudYOffset = config.get("combohud", "dss.config.client.comboHud.yoffset", 0, "Number of pixels to offset HUD alignment on the Y-Axis").getInt();
		/* Ending Blow HUD */
		endingBlowHudDisplayTime = config.get("endingblowhud", "dss.config.client.endingBlowHud.displayTime", 1000, "Number of milliseconds Ending Blow HUD will remain on screen (0 to disable)", 0, 20000).getInt();
		endingBlowHudResult = config.get("endingblowhud", "dss.config.client.endingBlowHud.enableResultNotification", true, "Display success / failure notification when Ending Blow is used").getBoolean(true);
		endingBlowHudText = config.get("endingblowhud", "dss.config.client.endingBlowHud.enableText", false, "Display text instead of icons for Ending Blow notifications").getBoolean(false);
		endingBlowHudXAlign = HALIGN.fromString(config.get("endingblowhud", "dss.config.client.endingBlowHud.xalign", "center", "Base HUD alignment on the X-Axis").setValidValues(xalign).getString());
		endingBlowHudXOffset = config.get("endingblowhud", "dss.config.client.endingBlowHud.xoffset", 0, "Number of pixels to offset HUD alignment on the X-Axis").getInt();
		endingBlowHudYAlign = VALIGN.fromString(config.get("endingblowhud", "dss.config.client.endingBlowHud.yalign", "top", "Base HUD alignment on the Y-Axis").setValidValues(yalign).getString());
		endingBlowHudYOffset = config.get("endingblowhud", "dss.config.client.endingBlowHud.yoffset", 30, "Number of pixels to offset HUD alignment on the Y-Axis").getInt();
		if (config.hasChanged()) {
			config.save();
		}
	}

	public static void refreshServer() {
		/*================== WEAPON REGISTRY =====================*/
		swords = config.get("Weapon Registry", "[Allowed Swords] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", new String[0], "Register an item so that it is considered a SWORD by DSS, i.e. it be used with skills that\nrequire swords, as well as other interactions that require swords, such as cutting grass.\nAll swords are also considered WEAPONS.").getStringList();
		Arrays.sort(swords);
		// Battlegear2 weapons ALL extend ItemSword, but are not really swords
		String[] forbidden = new String[]{
				"battlegear2:dagger.wood","battlegear2:dagger.stone","battlegear2:dagger.gold","battlegear2:dagger.iron","battlegear2:dagger.diamond",	
				"battlegear2:mace.wood","battlegear2:mace.stone","battlegear2:mace.gold","battlegear2:mace.iron","battlegear2:mace.diamond",
				"battlegear2:spear.wood","battlegear2:spear.stone","battlegear2:spear.gold","battlegear2:spear.iron","battlegear2:spear.diamond",
				"battlegear2:waraxe.wood","battlegear2:waraxe.stone","battlegear2:waraxe.gold","battlegear2:waraxe.iron","battlegear2:waraxe.diamond"
		};
		// Forbidden swords need to be added to the Allowed Weapons list or they can't use any skills at all 
		weapons = config.get("Weapon Registry", "[Allowed Weapons] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", forbidden, "Register an item as a generic melee WEAPON. This means it can be used for all\nskills except those that specifically require a sword, as well as some other things.").getStringList();
		Arrays.sort(weapons);
		forbidden_swords = config.get("Weapon Registry", "[Forbidden Swords] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", forbidden, "Forbid one or more items from acting as SWORDs, e.g. if a mod item extends ItemSword but is not really a sword - be sure to add it to the Allowed Weapons list if it should still be considered a weapon!").getStringList();
		Arrays.sort(forbidden_swords);
		forbidden_weapons = config.get("Weapon Registry", "[Forbidden Weapons] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", new String[0], "Forbid one or more items from acting as WEAPONs, e.g. if an item is added by IMC and you don't want it to be usable with skills.\nNote that this will also prevent the item from behaving as a SWORD.").getStringList();
		Arrays.sort(forbidden_weapons);
		/*================== SERVER =====================*/
		/* General server settings */
		backSliceDisarmorPlayer = config.get("general", "dss.config.server.general.backSliceDisarmorPlayer", true, "Allow Back Slice to potentially knock off player armor").getBoolean(true);
		String[] banned = config.get("general", "dss.config.server.general.bannedSkills", new String[0], "Enter the registry names for each skill disallowed on this server, each on a separate line between the '<' and '>'. Disabling a skill prevents players from using that skill, but does not change the player's known skills. Skill items previously generated as loot may be found but not used, and subsequent loot will not generate with that skill. Skill orb-like items may still drop from mobs / players unless disabled separately, but may not be used to learn the skill. This setting is save-game safe: skills may be disabled and re-enabled without affecting the saved game state.").setRequiresMcRestart(true).getStringList();
		bannedSkills.clear();
		bannedSkills.addAll(Lists.<String>newArrayList(banned));
		baseSwingSpeed = config.get("general", "dss.config.server.general.baseSwingSpeed", 0, "Default swing speed (anti-left-click-spam): Sets base number of ticks between each left-click (0 to disable)[0-20]", 0, 20).setRequiresWorldRestart(true).getInt();
		parryDisarmPenalty = 0.01F * (float)config.get("general", "dss.config.server.general.parryDisarmPenalty", 10, "[Parry] Penalty to disarm chance: percent per Parry level of the opponent, default negates defender's skill bonus so disarm is based entirely on timing [0-20]", 0, 20).getInt();
		parryDisarmTimingBonus = 0.001F * (float)config.get("general", "dss.config.server.general.parryDisarmTimingBonus", 25, "[Parry] Bonus to disarm based on timing: tenths of a percent added per tick remaining on the timer [0-50]", 0, 50).getInt();
		requireFullHealth = config.get("general", "dss.config.server.general.requireFullHealth", false, "True to require a completely full health bar to use Super Spin Attack and Sword Beam, or false to allow a small amount to be missing per level").setRequiresWorldRestart(true).getBoolean(false);
		risingCutHighJump = config.get("general", "dss.config.server.general.risingCutHighJump", false, "Allow the player to activate Rising Cut without hitting a target, i.e. perform a High Jump").getBoolean(false);
		skillSwordCreative = config.get("general", "dss.config.server.general.skillSwordCreative", true, "Enable Skill Swords in the Creative Tab (iron only, as examples)").setRequiresMcRestart(true).getBoolean(true);
		skillSwordCreativeLevel = config.get("general", "dss.config.server.general.skillSwordCreativeLevel", 3, "Skill level provided by the Creative Tab Skill Swords [1-5]", 1, 5).setRequiresMcRestart(true).getInt();
		skillSwordRandom = config.get("general", "dss.config.server.general.skillSwordRandom", true, "Enable randomized Skill Swords to add to loot or drop lists").setRequiresMcRestart(true).getBoolean(true);
		/* Loot / drops settings */
		bonusOrbEnable = config.get("drops", "dss.config.server.drops.bonusOrbEnable", false, "Whether all players should start with a Basic Skill orb").getBoolean(false);
		orbLootWeight = config.get("drops", "dss.config.server.drops.orbLootWeight", 1, "Weight for skill orbs when added to vanilla chest loot (0 to disable) [0-100]", 0, 100).setRequiresMcRestart(true).getInt();
		orbDropEnable = config.get("drops", "dss.config.server.drops.orbDropEnable", true, "Enable skill orbs to drop as loot from mobs (may still be disabled individually)").getBoolean(true);
		orbDropGeneralChance = 0.01F * (float)config.get("drops", "dss.config.server.drops.orbDropGeneralChance", 1, "Chance (as a percent) for generic mobs to drop a random skill orb [0-100]", 0, 100).getInt();
		orbDropRandomChance = 0.01F * (float)config.get("drops", "dss.config.server.drops.orbDropRandomChance", 10, "Chance (as a percent) for mobs with a specific skill orb drop to drop a random one instead [0-100]", 0, 100).getInt();
		orbDropChance = new HashMap<Integer, Float>(Skills.getSkillIdMap().size());
		for (Entry<Integer, ResourceLocation> entry : Skills.getSkillIdMap().entrySet()) {
			SkillBase skill = SkillRegistry.get(entry.getValue());
			int i = config.get("drops", "dss.config.server.drops.orbDropChance." + skill.getRegistryName().getResourcePath(), 5, "Chance (in tenths of a percent) for Skill Orb of " + skill.getDisplayName() + " to drop when available (0 to disable) [0-1000]", 0, 1000).getInt();
			orbDropChance.put((int)skill.getId(), (0.001F * (float) i));
		}
		playerDropEnable = config.get("drops", "dss.config.server.drops.playerDropEnable", true, "Enable skill orbs to drop from players when killed in PvP").getBoolean(true);
		playerDropFactor = config.get("drops", "dss.config.server.drops.playerDropFactor", 5, "Factor by which to multiply chance for skill orb to drop by slain players [1-20]", 1, 20).getInt();
		skillSwordLootWeight = config.get("drops", "dss.config.server.drops.skillSwordLootWeight", 1, "Weight for random skill swords when added to vanilla chest loot (0 to disable) [0-100]", 0, 100).setRequiresMcRestart(true).getInt();
		if (config.hasChanged()) {
			config.save();
		}
	}

	/**
	 * Loads settings that are sent to each player on login by {@link SyncConfigPacket}.
	 * Kept separate from {@link #refreshServer()} so that the packet's write method can
	 * re-read them without also re-firing this method's side effects, such as the
	 * weapon registry callbacks and the skill group rebuild.
	 */
	public static void applyServerSettings() {
		wideSwordBeamHitbox = config.get("general", "dss.config.server.general.wideSwordBeamHitbox", true, "Widen the Sword Beam's attack detection to match its visual appearance. Vanilla projectile collision uses a thin ray trace, which makes the beam far narrower than the flat, wide sprite it renders as; when enabled, an additional capsule-shaped sweep is performed along the beam's path so that grazing targets are still struck.").getBoolean(true);
		swordBeamAnywhere = config.get("general", "dss.config.server.general.swordBeamAnywhere", true, "Allow the Sword Beam to be fired while airborne or swimming. By default the player must be standing on the ground to use it, which prevents activation while jumping, falling, in mid-air, or in water; when enabled, only the sneaking requirement remains.").getBoolean(true);
		swordBeamNoCooldown = config.get("general", "dss.config.server.general.swordBeamNoCooldown", true, "Remove the Sword Beam's attack cooldown in all game modes, allowing it to be fired as fast as the attack key can be pressed. By default the cooldown is only ignored in Creative mode; when enabled, it is ignored in Survival as well. This affects the Sword Beam only - other skills and vanilla attacks keep their normal cooldown.").getBoolean(true);
		swordBeamNoRangeLimit = config.get("general", "dss.config.server.general.swordBeamNoRangeLimit", false, "Remove the Sword Beam's travel distance limit, letting the beam fly until something stops it. By default the beam expires after a set number of ticks, giving it a limited range that grows with skill level; when enabled, it only stops on impact and is capped instead by the server's view distance to prevent it being sent as an out-of-range entity.").getBoolean(false);
		swordBeamSpeedMultiplier = (float)Math.max(0.05D, config.get("general", "dss.config.server.general.swordBeamSpeedMultiplier", 1.0D, "Multiplier applied to the Sword Beam's initial flight speed. 1.0 keeps the original speed, which is 1.0 plus 0.15 per skill level (1.15 blocks per tick at level 1, up to 1.75 at level 5). Higher values make the beam travel faster and, with an unchanged lifespan, further; lower values make it slower.").getDouble(1.0D));
		swordBeamIgnoreWaterDrag = config.get("general", "dss.config.server.general.swordBeamIgnoreWaterDrag", true, "Prevent water from slowing the Sword Beam down. Vanilla projectiles lose speed far faster underwater, multiplying their motion by 0.8 each tick instead of 0.99, which makes the beam stall almost immediately after entering water; when enabled, the beam keeps the normal air drag underwater and is slowed no more by water than by air.").getBoolean(true);
		leapingBlowNoAttackRequired = config.get("general", "dss.config.server.general.leapingBlowNoAttackRequired", true, "Let Leaping Blow fire as soon as the player jumps while blocking, without also having to press the attack key. By default the jump only arms the skill and the attack key has to be pressed while airborne to release it; when enabled, jumping with the block key held is enough on its own.").getBoolean(true);
		armorBreakBonusDamage = config.get("general", "dss.config.server.general.armorBreakBonusDamage", true, "Give Armor Break extra damage that grows with its level. Without this the skill deals exactly one normal attack's worth of damage, while the charge it costs takes about as long as two normal attacks, so it is only worth using against targets whose armor makes those two hits add up to less. When enabled, the bonus is the larger of a flat amount per level and a percentage of the attack damage per level, so it stays meaningful for both vanilla and high-damage modded weapons.").getBoolean(true);
		armorBreakBonusDamagePerLevel = (float)config.get("general", "dss.config.server.general.armorBreakBonusDamagePerLevel", 3.0D, "Flat bonus damage added to Armor Break per skill level. Used when it is larger than the ratio-based bonus; relevant for vanilla weapons, whose damage is low enough that a flat amount is worth more.").getDouble(3.0D);
		armorBreakBonusDamageRatioPerLevel = (float)config.get("general", "dss.config.server.general.armorBreakBonusDamageRatioPerLevel", 0.3D, "Bonus damage added to Armor Break per skill level, as a fraction of the attack's damage. Used when it is larger than the flat bonus; relevant for high-damage weapons from adventure or equipment mods, where a flat amount would become negligible.").getDouble(0.3D);
		armorBreakBonusDamageExtraExhaustion = (float)config.get("general", "dss.config.server.general.armorBreakBonusDamageExtraExhaustion", 7.5D, "Extra exhaustion Armor Break costs at level 1 while the bonus damage is enabled, as the price for the extra damage. Exhaustion is spent 4 points at a time, taking 1 point of saturation first and 1 point of hunger once saturation runs out, so a completely full bar of 20 saturation plus 20 hunger holds 160 points. The last use is effectively free because the check happens before the cost is paid, so the number of uses is one higher than a plain division suggests - the defaults are tuned against that, giving about 18 uses at level 1 and 15 at level 5.").getDouble(7.5D);
		armorBreakBonusDamageExtraExhaustionPerLevel = (float)config.get("general", "dss.config.server.general.armorBreakBonusDamageExtraExhaustionPerLevel", 0.45D, "Additional exhaustion Armor Break costs per level beyond the first while the bonus damage is enabled. Keep this small: it is added on top of a large base amount, and because a use costs several whole 4-point steps, even a small change can shift the number of uses by a whole one.").getDouble(0.45D);
		dashSpeedDamageBonus = config.get("general", "dss.config.server.general.dashSpeedDamageBonus", true, "Let movement speed bonuses increase Dash damage and knockback meaningfully. By default the speed factor is 1 + (speed - 0.1), so a Swiftness II potion only adds about 4 percent even though it raises movement speed by 40 percent, which makes speed builds pointless for the skill. When enabled, the speed difference is measured against the sprinting speed instead and multiplied, so Swiftness II becomes worth roughly 26 percent. Enabling this also raises Dash's exhaustion, since the skill becomes stronger.").getBoolean(true);
		dashSpeedDamageBonusMultiplier = (float)config.get("general", "dss.config.server.general.dashSpeedDamageBonusMultiplier", 5.0D, "How strongly movement speed above the sprinting baseline multiplies Dash damage and knockback. The default of 5 turns Swiftness I into about 13 percent and Swiftness II into about 26 percent; raise it if other mods make speed easier to stack, lower it if the skill becomes too strong with speed gear.").getDouble(5.0D);
		dashSpeedDamageExtraExhaustion = (float)config.get("general", "dss.config.server.general.dashSpeedDamageExtraExhaustion", 7.1D, "Extra exhaustion Dash costs at level 1 while the speed damage bonus is enabled, as the price for the stronger skill. Exhaustion is spent 4 points at a time, taking 1 point of saturation first and 1 point of hunger once saturation runs out, so a completely full bar holds 160 points. This value covers the skill itself, which leaves level 1 at about 20 uses and level 5 at about 25. The dash also forces sprinting, and vanilla charges sprint exhaustion for the distance covered at 0.1 per block, costing roughly 0.4 to 0.8 more per use, so the counts seen in game are about 18 at level 1 and about 21 at level 5.").getDouble(7.1D);
		dashSpeedDamageExtraExhaustionPerLevel = (float)config.get("general", "dss.config.server.general.dashSpeedDamageExtraExhaustionPerLevel", -0.3D, "Change in the extra exhaustion Dash costs per level beyond the first while the speed damage bonus is enabled. Negative means higher levels cost less, which is the default: a more skilled user is expected to be more efficient, so the skill gets cheaper as it levels up. Set it to 0 to keep the extra cost flat, or positive to make it grow with level.").getDouble(-0.3D);
		dashSpeedDamageRatioPerLevel = (float)config.get("general", "dss.config.server.general.dashSpeedDamageRatioPerLevel", 0.3D, "Damage the Dash skill itself contributes per level, as a fraction of the player's attack damage including enchantment bonuses. Used when it is larger than the flat amount the skill level provides, which is the case for high-damage weapons from adventure or equipment mods where a flat amount would become negligible. Whichever is larger is used, so leveling the skill stays meaningful with either kind of gear.").getDouble(0.3D);
		if (config.hasChanged()) {
			config.save();
		}
	}

	private static void refreshSkillGroups() {
		List<String> groupList = Lists.<String>newArrayList();
		for (SkillGroup group : SkillGroup.getAll()) {
			groupList.add(group.label);
		}
		String[] groups = config.get("skillgui", "dss.config.client.skillGui.skillGroups", groupList.toArray(new String[0]), "Enter desired Skill Group labels in the order you wish them to appear, each on a separate line between the '<' and '>'").getStringList();
		int i = groups.length;
		for (String label : groups) {
			SkillGroup group = new SkillGroup(label).setDisplayName(label).register();
			if (group == null) {
				continue;
			}
			group.priority = i--;
			// Skill List for this group
			List<String> skillList = Lists.<String>newArrayList();
			for (SkillBase skill : SkillRegistry.getValues()) {
				if (skill.displayInGroup(group)) {
					skillList.add(skill.getRegistryName().toString());
				}
			}
			String[] groupSkills = config.get("skillgrouplists", label, skillList.toArray(new String[0]), "Enter skill registry names for each skill you wish to appear in this category, each on a separate line between the '<' and '>'").getStringList();
			Set<String> set = Sets.newHashSet(groupSkills);
			skillGroupLists.put(group.label, set);
		}
	}

	public static void postInit() {
		WeaponRegistry.INSTANCE.registerItems(swords, "Config", true);
		WeaponRegistry.INSTANCE.registerItems(weapons, "Config", false);
		WeaponRegistry.INSTANCE.forbidItems(forbidden_swords, "Config", true);
		WeaponRegistry.INSTANCE.forbidItems(forbidden_weapons, "Config", false);
		refreshSkillGroups();
		Config.loaded = true;
		if (config.hasChanged()) {
			config.save();
		}
	}
	/*================== CLIENT SIDE SETTINGS =====================*/
	public static int getHitsToDisplay() { return comboHudMaxHits; }
	public static boolean allowVanillaControls() { return allowVanillaControls; }
	public static boolean enableAdditionalControls() { return enableAdditionalControls; }
	public static boolean requiresDoubleTap() { return requireDoubleTap; }
	public static boolean requiresLockOn() { return requireLockOn; }
	public static boolean autoTargetEnabled() { return enableAutoTarget; }
	public static boolean canTargetPassiveMobs() { return enableTargetPassive; }
	public static boolean canTargetPlayers() { return enableTargetPlayer; }
	/** Toggles auto-targeting and returns the new value; the change is saved to disk */
	public static boolean toggleAutoTarget() {
		enableAutoTarget = !enableAutoTarget;
		config.get("client", "dss.config.client.enableAutoTarget", true).setValue(enableAutoTarget);
		config.save();
		return enableAutoTarget;
	}
	/** Toggles whether players may be targeted and returns the new value; the change is saved to disk */
	public static boolean toggleTargetPlayers() {
		enableTargetPlayer = !enableTargetPlayer;
		config.get("client", "dss.config.client.enableTargetPlayer", true).setValue(enableTargetPlayer);
		config.save();
		return enableTargetPlayer;
	}
	/** Toggles the Combo HUD and returns the new value; the change is saved to disk */
	public static boolean toggleComboHud() {
		isComboHudEnabled = !isComboHudEnabled;
		config.get("combohud", "dss.config.client.comboHud.enableDisplay", true).setValue(isComboHudEnabled);
		config.save();
		return isComboHudEnabled;
	}
	/* Skill GUI */
	public static boolean clickedGroupFilterSound() { return clickedGroupFilterSound; }
	public static boolean clickedPageSound() { return clickedPageSound; }
	public static boolean clickedSkillSound() { return clickedSkillSound; }
	public static boolean showBannedSkills() { return showBannedSkills; }
	public static boolean showPaginationLabels() { return showPaginationLabels; }
	public static boolean showPlainTextIndex() { return showPlainTextIndex; }
	public static boolean showSkillGroupTooltips() { return showSkillGroupTooltips; }
	public static boolean showUnknownSkills() { return showUnknownSkills; }
	public static boolean isSkillInGroup(SkillBase skill, SkillGroup group) {
		if (skill.getRegistryName() == null) { return false; }
		Set<String> set = skillGroupLists.get(group.label);
		String name = skill.getRegistryName().toString();
		String alt = skill.getRegistryName().getResourceDomain() + ":*";
		return set != null && (set.contains(name) || set.contains(alt));
	}
	/*================== SKILLS =====================*/
	public static boolean giveBonusOrb() { return bonusOrbEnable; }
	public static int getOrbLootWeight() { return orbLootWeight; }
	public static int getBaseSwingSpeed() { return baseSwingSpeed; }
	public static boolean areRandomSwordsEnabled() { return skillSwordRandom; }
	public static boolean areCreativeSwordsEnabled() { return skillSwordCreative; }
	public static boolean canDisarmorPlayers() { return backSliceDisarmorPlayer; }
	public static float getDisarmPenalty() { return parryDisarmPenalty; }
	public static float getDisarmTimingBonus() { return parryDisarmTimingBonus; }
	public static boolean canHighJump() { return risingCutHighJump; }
	/** @return true if the Sword Beam should use a widened attack detection matching its rendered width */
	public static boolean isWideSwordBeamHitbox() { return wideSwordBeamHitbox; }
	/** @return true if the Sword Beam may be fired without standing on the ground */
	public static boolean canUseSwordBeamAnywhere() { return swordBeamAnywhere; }
	/** @return true if the Sword Beam should ignore the attack cooldown in all game modes */
	public static boolean canSpamSwordBeam() { return swordBeamNoCooldown; }
	/** @return true if the Sword Beam should ignore its travel distance limit and only stop on impact */
	public static boolean canSwordBeamFlyForever() { return swordBeamNoRangeLimit; }
	/** @return multiplier applied to the Sword Beam's initial flight speed; 1.0 is the original speed */
	public static float getSwordBeamSpeedMultiplier() { return swordBeamSpeedMultiplier; }
	/** @return true if the Sword Beam should keep the normal air drag while underwater */
	public static boolean shouldSwordBeamIgnoreWaterDrag() { return swordBeamIgnoreWaterDrag; }
	/** @return true if Leaping Blow should fire on jump alone, without the attack key */
	public static boolean canLeapingBlowWithoutAttack() { return leapingBlowNoAttackRequired; }
	/** @return true if Armor Break should deal extra damage that grows with its level */
	public static boolean shouldArmorBreakBonusDamage() { return armorBreakBonusDamage; }
	/** @return flat bonus damage per Armor Break level */
	public static float getArmorBreakBonusDamagePerLevel() { return armorBreakBonusDamagePerLevel; }
	/** @return ratio bonus damage per Armor Break level, as a fraction of the attack's damage */
	public static float getArmorBreakBonusDamageRatioPerLevel() { return armorBreakBonusDamageRatioPerLevel; }
	/** @return extra exhaustion Armor Break costs at level 1, applied while the bonus damage is enabled */
	public static float getArmorBreakBonusDamageExtraExhaustion() { return armorBreakBonusDamageExtraExhaustion; }
	/** @return additional extra exhaustion per Armor Break level beyond the first */
	public static float getArmorBreakBonusDamageExtraExhaustionPerLevel() { return armorBreakBonusDamageExtraExhaustionPerLevel; }
	/** @return true if movement speed bonuses should meaningfully raise Dash damage and knockback */
	public static boolean shouldDashSpeedDamageBonus() { return dashSpeedDamageBonus; }
	/** @return how strongly movement speed above the sprinting baseline multiplies Dash damage */
	public static float getDashSpeedDamageBonusMultiplier() { return dashSpeedDamageBonusMultiplier; }
	/** @return extra exhaustion Dash costs at level 1, applied while the speed damage bonus is enabled */
	public static float getDashSpeedDamageExtraExhaustion() { return dashSpeedDamageExtraExhaustion; }
	/** @return additional extra exhaustion per Dash level beyond the first */
	public static float getDashSpeedDamageExtraExhaustionPerLevel() { return dashSpeedDamageExtraExhaustionPerLevel; }
	/** @return ratio damage per Dash level, as a fraction of the player's attack damage */
	public static float getDashSpeedDamageRatioPerLevel() { return dashSpeedDamageRatioPerLevel; }
	public static int getSkillSwordLevel() { return skillSwordCreativeLevel; }
	public static int getSkillSwordLootWeight() { return skillSwordLootWeight; }
	/** Returns amount of health that may be missing and still be able to activate certain skills (e.g. Sword Beam) */
	public static float getHealthAllowance(int level) {
		return (requireFullHealth ? 0.0F : (0.6F * level));
	}
	/** @return true if the skill has been disabled either by the server or client settings, or if it is null */
	public static final boolean isSkillDisabled(EntityPlayer player, @Nullable SkillBase skill) {
		return !Config.isSkillAllowed(skill) || DSSPlayerInfo.get(player).isSkillDisabled(skill);
	}
	/** @return true if the skill is allowed by the server, i.e. not banned */
	public static final boolean isSkillAllowed(@Nullable SkillBase skill) {
		return skill != null && !bannedSkills.contains(skill.getRegistryName().toString());
	}
	/*================== DROPS =====================*/
	public static boolean arePlayerDropsEnabled() { return playerDropEnable; }
	public static float getPlayerDropFactor() { return playerDropFactor; }
	public static boolean areOrbDropsEnabled() { return orbDropEnable; }
	public static float getChanceForRandomDrop() { return orbDropRandomChance; }
	public static float getRandomMobDropChance() { return orbDropGeneralChance; }
	public static float getDropChance(int orbID) {
		return (orbDropChance.containsKey(orbID) ? orbDropChance.get(orbID) : 0.0F);
	}

	/**
	 * Updates client settings from server packet
	 */
	public static void syncClientSettings(SyncConfigPacket msg) {
		if (!msg.isMessageValid()) {
			DynamicSwordSkills.logger.error("Invalid SyncConfigPacket attempting to process!");
			return;
		}
		Config.baseSwingSpeed = msg.baseSwingSpeed;
		Config.requireFullHealth = msg.requireFullHealth;
		Config.wideSwordBeamHitbox = msg.wideSwordBeamHitbox;
		Config.swordBeamAnywhere = msg.swordBeamAnywhere;
		Config.swordBeamNoCooldown = msg.swordBeamNoCooldown;
		Config.swordBeamNoRangeLimit = msg.swordBeamNoRangeLimit;
		Config.swordBeamSpeedMultiplier = msg.swordBeamSpeedMultiplier;
		Config.swordBeamIgnoreWaterDrag = msg.swordBeamIgnoreWaterDrag;
		Config.leapingBlowNoAttackRequired = msg.leapingBlowNoAttackRequired;
		Config.armorBreakBonusDamage = msg.armorBreakBonusDamage;
		Config.armorBreakBonusDamagePerLevel = msg.armorBreakBonusDamagePerLevel;
		Config.armorBreakBonusDamageRatioPerLevel = msg.armorBreakBonusDamageRatioPerLevel;
		Config.armorBreakBonusDamageExtraExhaustion = msg.armorBreakBonusDamageExtraExhaustion;
		Config.armorBreakBonusDamageExtraExhaustionPerLevel = msg.armorBreakBonusDamageExtraExhaustionPerLevel;
		Config.dashSpeedDamageBonus = msg.dashSpeedDamageBonus;
		Config.dashSpeedDamageBonusMultiplier = msg.dashSpeedDamageBonusMultiplier;
		Config.dashSpeedDamageExtraExhaustion = msg.dashSpeedDamageExtraExhaustion;
		Config.dashSpeedDamageExtraExhaustionPerLevel = msg.dashSpeedDamageExtraExhaustionPerLevel;
		Config.dashSpeedDamageRatioPerLevel = msg.dashSpeedDamageRatioPerLevel;
		Config.bannedSkills.clear();
		for (Byte b : msg.disabledIds) {
			SkillBase skill = SkillRegistry.getSkillById(b);
			if (skill != null) {
				Config.bannedSkills.add(skill.getRegistryName().toString());
			}
		}
	}

	/**
	 * Refreshes the settings that {@link SyncConfigPacket} transmits, without touching anything
	 * else. Called by the packet so that what it sends is read straight from the configuration
	 * file rather than from the static fields, which on a dedicated server still hold whatever
	 * value was loaded when the server started.
	 */
	public static void updateClientSettings() {
		applyServerSettings();
	}
}
