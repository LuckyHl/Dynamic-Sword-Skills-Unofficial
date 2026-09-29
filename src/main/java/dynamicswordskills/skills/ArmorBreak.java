/**
    Copyright (C) <2016> <coolAlias>

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

package dynamicswordskills.skills;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * 
 * ARMOR BREAK
 * Description: Unleash a powerful blow that ignores armor
 * Activation: Hold attack for (20 - level) ticks
 * Effect: Unleashes an attack that inflicts normal weapon damage but ignores armor
 * Exhaustion: 2.0F - (0.1F * level)
 * 
 * Using this skill performs an attack that ignores armor but otherwise deals exactly the
 * same damage as a normal attack with the given item would, including all bonuses from other
 * skills and enchantments.
 * 
 * Armor Break must be charged by holding the 'attack' key; once the charge reaches full,
 * the player will perform the Armor Break attack automatically.
 * 
 */
public class ArmorBreak extends SkillActive
{
	/** Set when triggered; set to 0 when target struck in onImpact() */
	private int activeTimer = 0;

	/** Current charge time */
	private int charge = 0;

	/** Flag to allow armor break to begin charging even if mouse is over a block */
	private boolean wasLockedOn;

	@SideOnly(Side.CLIENT)
	private KeyBinding attackKey;

	public ArmorBreak(String translationKey) {
		super(translationKey);
	}

	private ArmorBreak(ArmorBreak skill) {
		super(skill);
	}

	@Override
	public ArmorBreak newInstance() {
		return new ArmorBreak(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getChargeDisplay(getChargeTime(player)));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	protected boolean allowUserActivation() {
		return false;
	}

	@Override
	public boolean isActive() {
		return activeTimer > 0;
	}

	@Override
	protected float getExhaustion() {
		float base = 2.0F - (0.1F * level);
		if (Config.shouldArmorBreakBonusDamage()) {
			// The bonus damage makes the skill far more valuable, so it costs more to use as well.
			// Exhaustion is spent 4 points at a time, each time taking 1 point of saturation and,
			// once that runs out, 1 point of hunger - so a full bar (20 + 20) holds 160 points.
			// The base amount is deliberately large and the per-level part small, because the
			// intended budget is a nearly flat 18 uses at level 1 down to 15 at level 5, rather
			// than a value that scales with level.
			base += Config.getArmorBreakBonusDamageExtraExhaustion()
					+ Config.getArmorBreakBonusDamageExtraExhaustionPerLevel() * (level - 1);
		}
		return base;
	}

	/** Returns number of ticks required before attack will execute: 20 - level */
	private int getChargeTime(EntityPlayer player) {
		return 20 - level;
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isWeapon(player.getHeldItem());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			// Need to receive key release when not locked on to stop charging
			return charge > 0 && key == mc.gameSettings.keyBindAttack;
		}
		wasLockedOn = isLockedOn;
		return key == mc.gameSettings.keyBindAttack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		// Only begin charging if not mousing over a block or locked on, otherwise player cannot harvest blocks
		if (wasLockedOn || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			attackKey = key;
			// Start the charge on the key press itself rather than waiting for the next
			// update tick, as the key is no longer reported as held by then whenever the
			// player is using an item (e.g. blocking with a sword), which would otherwise
			// prevent Armor Break from ever charging while blocking
			beginCharging(player);
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void keyReleased(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == attackKey) {
			attackKey = null;
			charge = 0;
			DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
		}
	}

	@SideOnly(Side.CLIENT)
	private void initCharging(EntityPlayer player) {
		if (charge == 0 && attackKey != null && attackKey.getIsKeyPressed() && DSSPlayerInfo.get(player).canInteract()) {
			beginCharging(player);
		}
	}

	/**
	 * Begins charging the skill, provided the player is able to interact and is wielding
	 * a weapon. Safe to call multiple times; subsequent calls while already charging are ignored.
	 */
	@SideOnly(Side.CLIENT)
	private void beginCharging(EntityPlayer player) {
		if (charge == 0 && PlayerUtils.isWeapon(player.getHeldItem()) && DSSPlayerInfo.get(player).canInteract()) {
			charge = getChargeTime(player);
			// Unset the keybind state to prevent issues if the player mouses over a block while charging
			KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
		}
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		activeTimer = 4; // needs to be active for attack event to process correctly
		if (world.isRemote) { // only attack after server has been activated, i.e. client receives activation packet back
			attackKey = null;
			DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
			// Attack even while blocking, which is a valid way to charge this skill
			DSSClientEvents.handlePlayerAttack(Minecraft.getMinecraft(), true);
		}
		return true;
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		activeTimer = 0;
		charge = 0;
		DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			initCharging(player);
		}
		if (isActive()) {
			--activeTimer;
		} else if (charge > 0) {
			if (PlayerUtils.isWeapon(player.getHeldItem())) {
				int maxCharge = getChargeTime(player);
				if (charge < maxCharge - 1) {
					float f = 0.25F + 0.75F * ((float)(maxCharge - charge) / (float) maxCharge);
					DSSPlayerInfo.get(player).setArmSwingProgress(f, 0.0F);
				}
				--charge;
				if (charge == 0) {
					PacketDispatcher.sendToServer(new ActivateSkillPacket(this, true));
				}
			} else {
				DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
				charge = 0;
			}
		}
	}

	@Override
	public boolean onAttack(EntityPlayer player, EntityLivingBase entity, DamageSource source, float amount) {
		activeTimer = 0;
		entity.attackEntityFrom(DamageUtils.causeArmorBreakDamage(player), amount + getBonusDamage(amount));
		if (!player.worldObj.isRemote) { 
			PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_ARMORBREAK, 0.4F, 0.5F);
		}
		return true;
	}

	/**
	 * Extra damage granted on top of the normal attack damage.
	 *
	 * The charge costs about as long as two normal attacks, so the skill only pays off when one
	 * boosted hit beats those two hits. A flat bonus per level works for vanilla weapons, whose
	 * damage is low enough that a fixed amount matters, while a ratio-based bonus is needed for
	 * high-damage weapons from adventure or equipment mods, where a flat amount becomes noise.
	 * Whichever of the two is larger is used, so the skill stays relevant with either kind of gear.
	 */
	private float getBonusDamage(float amount) {
		if (!Config.shouldArmorBreakBonusDamage()) {
			return 0.0F;
		}
		float flat = Config.getArmorBreakBonusDamagePerLevel() * level;
		float ratio = Config.getArmorBreakBonusDamageRatioPerLevel() * level * amount;
		return Math.max(flat, ratio);
	}
}
