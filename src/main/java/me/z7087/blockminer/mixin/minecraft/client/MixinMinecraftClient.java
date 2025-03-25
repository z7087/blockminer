package me.z7087.blockminer.mixin.minecraft.client;

import me.z7087.blockminer.BlockMinerMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow
    public ClientPlayerEntity player;

    @SuppressWarnings("SpellCheckingInspection")
    @Shadow
    public HitResult crosshairTarget;

    @Redirect(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
    private boolean beforeAttackBlock(ClientPlayerInteractionManager interactionManager, BlockPos pos, Direction direction) {
        if (BlockMinerMod.getInstance().taskManager.handleAttackBlock(pos)
                || BlockMinerMod.getInstance().blockBreakUtils.isModBreakingBlock())
            return true;
        return interactionManager.attackBlock(pos, direction);
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;"
                    //#if MC < 11900
                    //$$ + "Lnet/minecraft/client/world/ClientWorld;"
                    //#endif
                    + "Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"
    ), order = 9800, cancellable = true)
    private void beforeUseOnBlock(CallbackInfo ci) {
        if (this.player.getMainHandStack().isEmpty() && BlockMinerMod.getInstance().taskManager.handleUseOnBlock(((BlockHitResult) this.crosshairTarget).getBlockPos()))
            ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;attackCooldown:I", opcode = Opcodes.GETFIELD), cancellable = true)
    private void beforeBlockBreaking(CallbackInfo ci) {
        if (BlockMinerMod.getInstance().blockBreakUtils.isModBreakingBlock()) {
            ci.cancel();
        }
    }
}
