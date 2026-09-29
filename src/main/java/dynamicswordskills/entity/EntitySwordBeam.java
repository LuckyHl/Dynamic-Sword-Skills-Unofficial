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

package dynamicswordskills.entity;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.Skills;
import dynamicswordskills.skills.SwordBeam;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * Sword beam shot from Link's sword when at full health. Inflicts a portion of
 * the original sword's base damage to the first entity struck, less 20% for each
 * additional target thus struck.
 * 
 * If using the Master Sword, the beam will shoot through enemies, hitting all
 * entities in its direct path.
 *
 */
public class EntitySwordBeam extends EntityThrowable
{
	/** Damage that will be inflicted on impact */
	private float damage = 4.0F;

	/** Skill level of user; affects range */
	private int level = 1;

	/** Base number of ticks this entity can exist */
	private int lifespan = 12;

	/**
	 * Hard tick cap used only when the travel distance limit is disabled, so that an
	 * unobstructed beam cannot live forever. Sized so the beam never gets further than the
	 * server will keep tracking it (see {@link Config#canSwordBeamFlyForever()}).
	 */
	private static final int maxLifespan = 100;

	/**
	 * Half-width, in blocks, of the beam's visual sprite. The beam is rendered as a flat quad
	 * scaled by 1.5F with a half-width of 0.5, i.e. 0.75 blocks to either side of its path.
	 */
	private static final double BEAM_HALF_WIDTH = 0.75D;

	/** Half-height of the same sprite, used to bound the widened sweep vertically */
	private static final double BEAM_HALF_HEIGHT = 0.5D;

	public EntitySwordBeam(World world) {
		super(world);
	}

	public EntitySwordBeam(World world, EntityLivingBase entity) {
		super(world, entity);
	}

	public EntitySwordBeam(World world, double x, double y, double z) {
		super(world, x, y, z);
	}

	@Override
	public void entityInit() {
		setSize(0.5F, 0.5F);
	}

	/**
	 * Each level increases the distance the beam will travel
	 */
	public EntitySwordBeam setLevel(int level) {
		this.level = level;
		this.lifespan += level;
		// Velocity has to be recomputed here: EntityThrowable's constructor resolves
		// func_70182_d() before this method ever runs, so it always sees the default level
		// of 1 and the beam would otherwise keep the level 1 speed at every level.
		// Re-aiming along the existing motion preserves the direction the constructor
		// picked; passing 0 inaccuracy avoids rolling the spread a second time.
		this.setThrowableHeading(this.motionX, this.motionY, this.motionZ, this.func_70182_d(), 0.0F);
		return this;
	}

	/**
	 * Sets amount of damage that will be caused onImpact
	 */
	public EntitySwordBeam setDamage(float amount) {
		this.damage = amount;
		return this;
	}

	/** Entity's velocity factor, scaled by the configured speed multiplier */
	@Override
	protected float func_70182_d() {
		return (1.0F + (level * 0.15F)) * Config.getSwordBeamSpeedMultiplier();
	}

	@Override
	public float getGravityVelocity() {
		return 0.0F;
	}

	@Override
	public float getBrightness(float partialTick) {
		return 1.0F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float partialTick) {
		return 0xf000f0;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (Config.shouldSwordBeamIgnoreWaterDrag() && isInWater()) {
			// EntityThrowable#onUpdate multiplies the motion by an extra 0.8 at the end of the
			// tick when the projectile is in water. The move and the hit detection earlier in that
			// method already used the un-dragged velocity, so scaling the stored motion back up by
			// 0.99 / 0.8 leaves the beam with the regular air drag instead of the harsher water one.
			double f = 0.99D / 0.8D;
			motionX *= f;
			motionY *= f;
			motionZ *= f;
		}
		if (inGround || (!Config.canSwordBeamFlyForever() && ticksExisted > lifespan)) {
			setDead();
		}
		if (Config.canSwordBeamFlyForever() && ticksExisted > maxLifespan) {
			setDead();
		}
		if (!worldObj.isRemote && !this.isDead && Config.isWideSwordBeamHitbox()) {
			sweepForTargets();
		}
		for (int i = 0; i < 2; ++i) {
			worldObj.spawnParticle((i % 2 == 1 ? "magicCrit" : "crit"), posX, posY, posZ, motionX + rand.nextGaussian(), 0.01D, motionZ + rand.nextGaussian());
			worldObj.spawnParticle((i % 2 == 1 ? "magicCrit" : "crit"), posX, posY, posZ, -motionX + rand.nextGaussian(), 0.01D, -motionZ + rand.nextGaussian());
		}
	}

	/**
	 * Performs a widened version of the projectile's own ray trace and strikes the closest target
	 * found, if any. Vanilla projectile collision expands each candidate's bounding box by a fixed
	 * 0.3 blocks, which is far narrower than the flat, wide blade the beam is rendered as; targets
	 * that merely graze the sprite are therefore missed. Expanding by the beam's actual half-width
	 * instead makes the attack detection match what the player sees.
	 */
	private void sweepForTargets() {
		Vec3 start = Vec3.createVectorHelper(lastTickPosX, lastTickPosY, lastTickPosZ);
		Vec3 end = Vec3.createVectorHelper(posX, posY, posZ);
		if (start.squareDistanceTo(end) < 1.0E-7D) {
			return; // no movement this tick, the parent's ray trace already covered it
		}
		AxisAlignedBB bounds = boundingBox.addCoord(motionX, motionY, motionZ).expand(BEAM_HALF_WIDTH, BEAM_HALF_HEIGHT, BEAM_HALF_WIDTH);
		EntityLivingBase thrower = getThrower();
		EntityLivingBase closest = null;
		double closestDist = 0.0D;
		@SuppressWarnings("unchecked")
		List<Entity> targets = worldObj.getEntitiesWithinAABBExcludingEntity(this, bounds);
		for (Entity target : targets) {
			if (!(target instanceof EntityLivingBase) || !target.canBeCollidedWith()) {
				continue;
			}
			if (target == thrower && ticksExisted < 5) {
				continue; // do not hit the shooter in the first few ticks, matching vanilla behaviour
			}
			MovingObjectPosition mop = target.boundingBox.expand(BEAM_HALF_WIDTH, BEAM_HALF_HEIGHT, BEAM_HALF_WIDTH).calculateIntercept(start, end);
			if (mop != null) {
				double dist = start.distanceTo(mop.hitVec);
				if (closest == null || dist < closestDist) {
					closest = (EntityLivingBase) target;
					closestDist = dist;
				}
			}
		}
		if (closest != null) {
			onImpact(new MovingObjectPosition(closest));
		}
	}

	@Override
	protected void onImpact(MovingObjectPosition mop) {
		if (!worldObj.isRemote) {
			EntityPlayer player = (getThrower() instanceof EntityPlayer ? (EntityPlayer) getThrower() : null);
			SwordBeam skill = (player != null ? (SwordBeam) DSSPlayerInfo.get(player).getPlayerSkill(Skills.swordBeam) : null);
			if (mop.typeOfHit == MovingObjectType.ENTITY) {
				Entity entity = mop.entityHit;
				if (entity == player) { return; }
				if (player != null) {
					if (skill != null) {
						skill.onImpact(player, false);
					}
					if (entity.attackEntityFrom(DamageUtils.causeIndirectComboDamage(this, player).setProjectile(), damage)) {
						PlayerUtils.playSoundAtEntity(worldObj, entity, ModInfo.SOUND_HURT_FLESH, 0.4F, 0.5F);
					}
					damage *= 0.8F;
				}
				if (this.level < Skills.swordBeam.getMaxLevel()) {
					setDead();
				}
			} else {
				Block block = worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
				if (block.getMaterial().blocksMovement()) {
					if (player != null && skill != null) {
						skill.onImpact(player, true);
					}
					setDead();
				}
			}
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setFloat("damage", damage);
		compound.setInteger("level", level);
		compound.setInteger("lifespan", lifespan);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		damage = compound.getFloat("damage");
		level = compound.getInteger("level");
		lifespan = compound.getInteger("lifespan");
	}
}
