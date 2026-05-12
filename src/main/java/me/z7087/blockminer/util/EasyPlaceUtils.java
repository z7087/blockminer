package me.z7087.blockminer.util;

import me.z7087.blockminer.BlockMinerMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public final class EasyPlaceUtils {
    private EasyPlaceUtils() {}

    // generate easyplace in litematica

    public static ActionResult placePistonCarpetExtra(
            ClientPlayerInteractionManager interactionManager,
            ClientPlayerEntity player,
            ClientWorld ignoredWorld,
            Hand hand,
            BlockHitResult hitResult,
            Direction pistonFace
    ) {
        {
            final Vec3d hitVec = hitResult.getPos();
            hitResult = new BlockHitResult(
                    new Vec3d(
                            hitVec.getX() + ((pistonFace.getIndex() * 2) + 2),
                            hitVec.getY(),
                            hitVec.getZ()
                    ),
                    hitResult.getSide(),
                    hitResult.getBlockPos(),
                    hitResult.isInsideBlock()
                    //#if MC >= 12102
                    , hitResult.isAgainstWorldBorder()
                    //#endif
            );
        }
        final BlockHitResult finalHitResult = hitResult;
        return fakeRotation4EasyPlacePistonDuring(
                player,
                pistonFace,
                () -> BlockUtils.interactBlock(interactionManager, player, ignoredWorld, hand, finalHitResult)
        );
    }

    public static ActionResult placePistonV3(
            ClientPlayerInteractionManager interactionManager,
            ClientPlayerEntity player,
            ClientWorld ignoredWorld,
            Hand hand,
            BlockHitResult hitResult,
            Direction pistonFace
    ) {
        // same currently :/
        {
            final Vec3d hitVec = hitResult.getPos();
            hitResult = new BlockHitResult(
                    new Vec3d(
                            hitVec.getX() + ((pistonFace.getIndex() << 1) + 2),
                            hitVec.getY(),
                            hitVec.getZ()
                    ),
                    hitResult.getSide(),
                    hitResult.getBlockPos(),
                    hitResult.isInsideBlock()
                    //#if MC >= 12102
                    , hitResult.isAgainstWorldBorder()
                    //#endif
            );
        }
        final BlockHitResult finalHitResult = hitResult;
        return fakeRotation4EasyPlacePistonDuring(
                player,
                pistonFace,
                () -> BlockUtils.interactBlock(interactionManager, player, ignoredWorld, hand, finalHitResult)
        );
    }

    private static <T> T fakeRotation4EasyPlacePistonDuring(
            ClientPlayerEntity player,
            Direction pistonFace,
            Supplier<T> supplier
    ) {
        float yaw = 0F, pitch = 0F;
        switch (pistonFace) {
            case UP: {
                pitch = 90F;
                break;
            }
            case DOWN: {
                pitch = -90F;
                break;
            }
            case SOUTH: {
                yaw = 180F;
                break;
            }
            case WEST: {
                yaw = -90F;
                break;
            }
            /*
            case NORTH: {
                yaw = 0F;
                break;
            }
            */
            case EAST: {
                yaw = 90F;
                break;
            }
        }
        return BlockMinerMod.getInstance().getRotationUtils().fakeClientSideRotationDuring(player, yaw, pitch, supplier);
    }
}
