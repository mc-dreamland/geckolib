package software.bernie.geckolib.animatable;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

/**
 * The {@link GeoAnimatable} interface specific to {@link net.minecraft.world.entity.Entity Entities}
 * <p>
 * This also applies to Projectiles and other Entity subclasses
 * <p>
 * <b>NOTE:</b> This <u>cannot</u> be used for entities using the {@link software.bernie.geckolib.renderer.GeoReplacedEntityRenderer}
 * as you aren't extending {@code Entity}. Use {@link GeoReplacedEntity} instead.
 *
 * @see <a href="https://github.com/bernie-g/geckolib/wiki/Entity-Animations">GeckoLib Wiki - Entity Animations</a>
 */
public interface GeoEntity extends GeoAnimatable {
	/**
	 * Get server-synced animation data via its relevant {@link SerializableDataTicket}
	 * <p>
	 * Should only be used on the <u>client-side</u>
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param dataTicket The data ticket for the data to retrieve
	 * @return The synced data, or null if no data of that type has been synced
	 */
	@ApiStatus.NonExtendable
	@Nullable
	default <D> D getAnimData(SerializableDataTicket<D> dataTicket) {
		return getAnimatableInstanceCache().getManagerForId(((Entity)this).getId()).getData(dataTicket);
	}

	/**
	 * Saves an arbitrary syncable piece of data to this animatable's {@link AnimatableManager}
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param dataTicket The DataTicket to sync the data for
	 * @param data The data to sync
	 */
	@ApiStatus.NonExtendable
	default <D> void setAnimData(SerializableDataTicket<D> dataTicket, D data) {
		Entity entity = (Entity)this;

		if (entity.level().isClientSide()) {
			getAnimatableInstanceCache().getManagerForId(entity.getId()).setData(dataTicket, data);
		}
		else {
			GeckoLibServices.NETWORK.syncEntityAnimData(entity, false, dataTicket, data);
		}
	}

	/**
	 * Trigger an animation for this Entity, based on the controller name and animation name
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param controllerName The name of the controller name the animation belongs to, or null to do an inefficient lazy search
	 * @param animName The name of animation to trigger. This needs to have been registered with the controller via {@link AnimationController#triggerableAnim AnimationController.triggerableAnim}
	 */
	@ApiStatus.NonExtendable
	default void triggerAnim(@Nullable String controllerName, String animName) {
		Entity entity = (Entity)this;

		if (entity.level().isClientSide()) {
			getAnimatableInstanceCache().getManagerForId(entity.getId()).tryTriggerAnimation(controllerName, animName);
		}
		else {
			GeckoLibServices.NETWORK.triggerEntityAnim(entity, false, controllerName, animName);
		}
	}
	
	/**
	 * Returns the current age/tick of the animatable instance
	 * <p>
	 * By default this is just the animatable's age in ticks, but this method allows for non-ticking custom animatables to provide their own values
	 *
	 * @param entity The Entity representing this animatable
	 * @return The current tick/age of the animatable, for animation purposes
	 */
	@Override
	default double getTick(Object entity) {
		return ((Entity)entity).tickCount;
	}
}
