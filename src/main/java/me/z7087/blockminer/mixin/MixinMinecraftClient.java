package me.z7087.blockminer.mixin;

import me.z7087.blockminer.BlockMinerMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @SuppressWarnings("SpellCheckingInspection")
    @Shadow
    public HitResult crosshairTarget;

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"), cancellable = true)
    private void beforeAttackBlock(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() != null)
            return;
        if (BlockMinerMod.INSTANCE.taskManager.handleAttackBlock(((BlockHitResult) crosshairTarget).getBlockPos())) {
            cir.setReturnValue(true);
            return;
        }
        if (BlockMinerMod.INSTANCE.blockBreakUtils.isModBreakingBlock())
            cir.setReturnValue(true);
    }

    //#if MC >= 11900
    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult beforeUseOnBlock(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (hand == Hand.MAIN_HAND && player.getMainHandStack().isEmpty() && BlockMinerMod.INSTANCE.taskManager.handleUseOnBlock(hitResult.getBlockPos()))
            return ActionResult.FAIL;
        return interactionManager.interactBlock(player, hand, hitResult);
    }
    //#else
    //$$ @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    //$$ private ActionResult beforeUseOnBlock(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, net.minecraft.client.world.ClientWorld world, Hand hand, BlockHitResult hitResult) {
    //$$     if (hand == Hand.MAIN_HAND && player.getMainHandStack().isEmpty() && BlockMinerMod.INSTANCE.taskManager.handleUseOnBlock(hitResult.getBlockPos()))
    //$$         return ActionResult.FAIL;
    //$$     return interactionManager.interactBlock(player, world, hand, hitResult);
    //$$ }
    //#endif

    @Inject(method = "handleBlockBreaking", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;attackCooldown:I", opcode = Opcodes.GETFIELD), cancellable = true)
    private void beforeBlockBreaking(CallbackInfo ci) {
        if (BlockMinerMod.INSTANCE.blockBreakUtils.isModBreakingBlock()) {
            ci.cancel();
        }
    }
}
