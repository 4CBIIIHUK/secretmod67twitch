package dev.twitchmod.mixin;

import dev.twitchmod.client.cutscene.ClientCutscenePlayer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockView;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cutscene camera. Only overrides the view while a scene with a non-static camera
 * is running; HUD and hand rendering are untouched, so the strike counter stays
 * readable (accessibility rule).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void twitchmod$update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                  float tickDelta, CallbackInfo ci) {
        if (!ClientCutscenePlayer.controlsCamera() || focusedEntity == null) return;
        Vec3d center = focusedEntity.getLerpedPos(tickDelta)
                .add(0, focusedEntity.getHeight() * 0.85D, 0);
        double orbit = ClientCutscenePlayer.orbitRadians();
        double dist = ClientCutscenePlayer.distance();
        double height = ClientCutscenePlayer.heightOffset();
        Vec3d eye = center.add(Math.sin(orbit) * dist, height, Math.cos(orbit) * dist);
        setPos(eye);
        float[] angles = ClientCutscenePlayer.lookAngles(center, eye);
        setRotation(angles[0], angles[1]);
        ci.cancel();
    }
}
