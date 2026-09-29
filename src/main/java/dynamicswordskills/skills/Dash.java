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

package dynamicswordskills.skills;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.api.IDashItem;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.DashImpactPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * The player charges into the target, inflicting damage and knocking the target back.
 * 
 * Activation: Press the attack key while moving forward and blocking
 * Range: 3 blocks plus 1 block per level
 * Damage: up to 2 plus 1 per level at max range
 * Knockback Strength: 0.4F per level plus an additional 0.15F per block traveled beyond the minimum (capped at 3.0F)
 * Exhaustion: Light [1.0F - (level * 0.05F)]
 * Special: Must be at least 2 blocks away from target when skill is activated to
 * 			inflict damage, minus 0.2F per level (down to 1 block at level 5)
 * Special: Effects that increase player speed increase the effective range, damage, and knockback.
 * 
 */
public class Dash extends SkillActive
{
	/** Player's base movement speed */
	public static final double BASE_MOVE = 0.10000000149011612D;

	/** True when Slam is used and while the player is in motion towards the target */
	private boolean isActive = false;

	/** Number of ticks since activation */
	private int activeTime;

	/** Trajectory based on player's last look vector while on the ground */
	private Vec3 trajectory;

	/** Player's starting position is used to determine actual distance traveled upon impact */
	private Vec3 initialPosition;

	/** Target acquired from ILockOnTarget skill; set to the entity hit upon impact */
	private Entity target;

	/** Impact timer used to make player immune to damage from struck target only, vs. setting hurtResistantTime */
	private int impactTime;

	public Dash(String translationKey) {
		super(translationKey);
	}

	private Dash(Dash skill) {
		super(skill);
	}

	@Override
	public Dash newInstance() {
		return new Dash(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		// Enchantment damage is measured against a generic target, as the tooltip has none:
		// players are UNDEFINED, so this counts sharpness but not Smite or Bane of Arthropods.
		desc.add(getDamageDisplay(getSkillDamage(getWeaponDamage(player, player)), false));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.knockback", String.format("%.1f", getKnockback())));
		desc.add(getRangeDisplay(getRange()));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.min_range", String.format("%.1f", getMinDistance())));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return isActive || impactTime > 0;
	}

	/** Maximum active time in case player is unable to move an appropriate amount of distance */
	protected int getMaxActiveTime() {
		return 14 + (2 * level);
	}

	/** Number of ticks the player will not be able to block or use an item after impact */
	private int getBlockCooldown() {
		return (30 - (2 * level));
	}

	@Override
	protected float getExhaustion() {
		float base = 1.0F - (0.05F * level);
		if (Config.shouldDashSpeedDamageBonus()) {
			// The speed bonus makes the skill stronger, so it costs more to use as well. Exhaustion
			// is spent 4 points at a time, taking 1 point of saturation first and 1 point of hunger
			// once saturation runs out, so a completely full bar holds 160 points. The defaults
			// leave level 1 at about 20 uses and level 5 at about 25 for the skill itself.
			//
			// Note that the dash also forces sprinting and moves the player with addVelocity, so
			// vanilla charges sprint exhaustion through EntityPlayer.addMovementStat at 0.1 per
			// block traveled (walking is 0.01). That adds roughly 0.4 to 0.8 per use depending on
			// level, so the counts actually observed in game are a little lower than the numbers
			// above: about 18 at level 1 and about 21 at level 5.
			base += Config.getDashSpeedDamageExtraExhaustion()
					+ Config.getDashSpeedDamageExtraExhaustionPerLevel() * (level - 1);
		}
		return base;
	}

	/** Flat damage the skill level provides, used when it beats the ratio-based amount */
	private float getFlatDamage() {
		return (2.0F + level);
	}

	/**
	 * Damage the skill itself contributes, on top of the player's weapon.
	 *
	 * A flat amount per level works for vanilla weapons, whose damage is low enough that a
	 * fixed amount matters, while a ratio-based amount is needed for high-damage weapons from
	 * adventure or equipment mods, where a flat amount becomes noise. Whichever is larger is
	 * used, so leveling the skill stays meaningful with either kind of gear.
	 */
	private float getSkillDamage(float weapon) {
		return Math.max(getFlatDamage(), Config.getDashSpeedDamageRatioPerLevel() * level * weapon);
	}

	/** Returns the player's attack damage attribute value, which excludes enchantment bonuses */
	private static float getBaseWeaponDamage(EntityPlayer player) {
		return (player == null ? 0.0F : (float)player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
	}

	/**
	 * Returns the weapon damage used by the dash, including enchantment bonuses.
	 *
	 * The dash is a point-blank skill: it leaves the player right next to the target, so the hit
	 * is almost always answered with one in return. Melee skills such as Armor Break apply their
	 * bonuses to the full vanilla attack damage, which includes enchantments, so the dash has to
	 * match that or a well-enchanted sword would be worth less here than anywhere else. The ranged
	 * skills (Leaping Blow, Sword Beam) deliberately stay on base damage only, since they can be
	 * used from a safe distance and do not carry the same risk.
	 *
	 * @param target the entity being hit, or null to skip the enchantment bonus
	 */
	private static float getWeaponDamage(EntityPlayer player, EntityLivingBase target) {
		float weapon = getBaseWeaponDamage(player);
		if (player != null && target != null) {
			weapon += EnchantmentHelper.getEnchantmentModifierLiving(player, target);
		}
		return weapon;
	}

	/** Returns base knockback strength, not accounting for distance traveled */
	private float getKnockback() {
		return 0.4F * level;
	}

	/** Range increases by 1 block per level */
	private double getRange() {
		return (3.0D + level);
	}

	/** Minimum distance the player must cover before the dash is effective */
	private double getMinDistance() {
		return 2.0D - (0.2D * level);
	}

	/** Movement speed while sprinting, used as the baseline for the speed damage bonus */
	private static final double SPRINT_SPEED = BASE_MOVE * 1.3D;

	/**
	 * Returns the factor applied to Dash damage and knockback based on the player's movement speed.
	 *
	 * Vanilla uses {@code 1 + (speed - BASE_MOVE)}, which makes speed bonuses nearly worthless:
	 * Swiftness II raises movement speed by 40 percent but only adds about 4 percent here, because
	 * the bonus is a small difference against a fixed base of 1. With the speed bonus enabled the
	 * difference is measured from the sprinting speed instead and multiplied, so Swiftness II
	 * becomes worth roughly 26 percent. Never drops below 1, so enabling it cannot make the skill
	 * weaker than vanilla.
	 */
	private double getSpeedFactor(EntityPlayer player) {
		double speed = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed).getAttributeValue();
		if (Config.shouldDashSpeedDamageBonus()) {
			return Math.max(1.0D, 1.0D + (speed - SPRINT_SPEED) * Config.getDashSpeedDamageBonusMultiplier());
		}
		return 1.0D + (speed - BASE_MOVE);
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && canHeldItemDash(player);
	}

	/**
	 * @return true if the currently held item allows dashing, either the player is blocking or an IDashItem in use
	 */
	private boolean canHeldItemDash(EntityPlayer player) {
		if (PlayerUtils.isBlocking(player)) {
			return true;
		}
		ItemStack stack = player.getHeldItem();
		if (stack != null && stack.getItem() instanceof IDashItem) {
			return player.isUsingItem();
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return player.onGround && canUse(player) && Minecraft.getMinecraft().gameSettings.keyBindForward.getIsKeyPressed();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			return false;
		}
		return key == mc.gameSettings.keyBindAttack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		return canExecute(player) && activate(player);
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		isActive = true;
		activeTime = 0;
		player.setSprinting(true);
		trajectory = player.getLookVec();
		initialPosition = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		initialPosition = null;
		impactTime = 0; // no longer active, target will be set to null from setNotDashing
		setNotDashing(player); // sets all remaining fields to 0 or null
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (impactTime > 0) {
			--impactTime;
			if (impactTime == 0) {
				target = null;
			}
		}
		// don't use isActive() method, as that also returns true after impact
		if (isActive) {
			player.setSprinting(true);
			// Only check for impact on the client, as the server is not reliable for this step
			// If a collision is detected, DashImpactPacket is sent to conclude the server-side
			if (!canHeldItemDash(player)) {
				if (!player.worldObj.isRemote) {
					deactivate(player);
				}
			} else if (player.worldObj.isRemote) {
				if (trajectory != null) {
					double bonus = 1.0D + (0.1D * level);
					double speed = bonus * player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed).getAttributeValue();
					if (player.isInWater() || player.handleLavaMovement()) {
						speed *= 0.15D;
					}
					if (player.onGround) { 
						trajectory = player.getLookVec();
					}
					player.addVelocity(trajectory.xCoord * speed, -0.02D, trajectory.zCoord * speed);
				}
				MovingObjectPosition mop = TargetUtils.checkForImpact(player.worldObj, player, player, 0.5D, false);
				if (mop != null || player.isCollidedHorizontally) {
					PacketDispatcher.sendToServer(new DashImpactPacket(player, mop));
					// Force player to stop blocking upon impact
					DSSPlayerInfo.get(player).setUseItemCooldown(getBlockCooldown());
					KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode(), false);
					impactTime = 5;
					if (mop != null && mop.typeOfHit == MovingObjectType.ENTITY) {
						target = mop.entityHit;
					}
					double d = (player.onGround ? 2.0D : 0.5D);
					double dy = (player.onGround ? 0.3D : -0.15D);
					player.setVelocity(-player.motionX * d, dy, -player.motionZ * d);
					setNotDashing(player);
				} else if (initialPosition == null || player.getDistance(initialPosition.xCoord, initialPosition.yCoord, initialPosition.zCoord) > getRange()) {
					player.addVelocity(-player.motionX * 0.5D, -0.02D, -player.motionZ * 0.5D);
					deactivate(player);
				} else if (!Minecraft.getMinecraft().gameSettings.keyBindForward.getIsKeyPressed()) {
					deactivate(player);
				}
			}
		}
		// Update active time if still active
		if (isActive) {
			++activeTime;
			if (activeTime > getMaxActiveTime()) {
				if (!player.worldObj.isRemote) {
					deactivate(player);
				}
			}
		}
	}

	/**
	 * Scans along the player's line of sight for the closest valid target, without relying on
	 * the player's motion values, which are not available server-side. Used as a fallback when
	 * the client reports a horizontal collision but no entity hit.
	 */
	private MovingObjectPosition getClosestTargetInRange(EntityPlayer player) {
		EntityLivingBase found = TargetUtils.acquireLookTarget(player, (int) getRange() + 1, 1.0D, true);
		return found == null ? null : new MovingObjectPosition(found);
	}

	/**
	 * Called on the server from {@link DashImpactPacket} to process the impact data from the client
	 * @param player	Player's motionX and motionZ have been set by the packet, so the values may be used
	 * @param mop	Null assumes a block was hit (none of the block data is needed, so it is not sent),
	 * 				or a valid MovingObjectPosition for the entity hit
	 */
	public void onImpact(World world, EntityPlayer player, MovingObjectPosition mop) {
		// The client reports an impact whenever it collides horizontally, even when no entity
		// was picked up by its ray trace; the fast dash movement can skip over a target's
		// hitbox between ticks. Fall back to a short-range scan so the dash still connects.
		if (mop == null || mop.typeOfHit != MovingObjectType.ENTITY) {
			mop = getClosestTargetInRange(player);
		}
		if (mop != null && mop.typeOfHit == MovingObjectType.ENTITY) {
			target = mop.entityHit;
			double distance = target.getDistance(initialPosition.xCoord, initialPosition.yCoord, initialPosition.zCoord);
			// Subtract hitbox modifier when comparing min distance to avoid hitting enemies right in front of the player
			double bbMod = (target.width / 2.0F) + (player.width / 2.0F);
			double speed = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed).getAttributeValue();
			double sf = getSpeedFactor(player);
			if (player.isInWater() || player.handleLavaMovement()) {
				sf *= 0.3D;
			}
			if (speed > 0.075D && (distance - bbMod) > getMinDistance() && distance < (getRange() + 1.0D) && player.getDistanceSqToEntity(target) < 6.0D) {
				// The dash leaves the player right next to the target, so a hit is almost always
				// answered with one in return; the skill has to beat a plain swing to be worth it.
				// Enchantments are included for the same reason: a plain swing gets them too.
				float weapon = getWeaponDamage(player, (target instanceof EntityLivingBase ? (EntityLivingBase) target : null));
				float dmg = (float)(sf * (getSkillDamage(weapon) + weapon) * distance / getRange());
				impactTime = 5; // time player will be immune to damage from the target entity
				target.attackEntityFrom(DamageSource.causePlayerDamage(player), dmg);
				if (target instanceof EntityLivingBase) {
					float db = 0.15F * (float)(distance - getMinDistance());
					float k = (float)sf * Math.min(db + getKnockback(), 3.0F);
					TargetUtils.knockTargetBack((EntityLivingBase) target, player, 0.5F * k);
				}
				if (target instanceof EntityPlayerMP && !player.worldObj.isRemote) {
					((EntityPlayerMP) target).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(target));
				}
			}
		}
		DSSPlayerInfo.get(player).setUseItemCooldown(getBlockCooldown());
		PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_SLAM, 0.4F, 0.5F);
		setNotDashing(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return isActive; // don't continue render tick updates after impact
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		player.setSprinting(true); // required for a smooth experience and sprinting particle effects
		return false; // this skill doesn't need to control the camera
	}

	@Override
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		if (impactTime > 0 && source.getEntity() == target) {
			return true;
		} else if (source.damageType.equals("mob") && source.getEntity() != null && player.getDistanceSqToEntity(source.getEntity()) < 6.0D) {
			return true; // stop stupid zombies from hitting player right before impact
		}
		return false;
	}

	/**
	 * After calling this method, {@link #isAnimating()} will always return false;
	 * {@link #isActive()} will return false if no entity was impacted, otherwise it
	 * will still be true for {@link #impactTime} ticks to prevent damage from the {@link #target}. 
	 */
	private void setNotDashing(EntityPlayer player) {
		isActive = false;
		player.setSprinting(false);
		trajectory = null;
		if (!isActive()) {
			target = null;
		}
	}
}
