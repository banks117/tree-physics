package com.farcr.treephysics.mixin.tree_splitting;

import com.farcr.treephysics.api.util.FloodFillUtil;
import com.farcr.treephysics.api.util.TreeUtil;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire removes blocks without a break event, so split a fallen tree when it burns through a log. */
@Mixin(FireBlock.class)
public class FireBlockMixin {

    // NeoForge adds a Direction parameter to checkBurnOut, so the arguments are taken by type.
    @Inject(method = "checkBurnOut", at = @At("HEAD"))
    private void treephysics$rememberBurningLog(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos, @Share("burning") LocalRef<BlockState> burning) {
        // Vanilla may replace the log with air or fire, so remember what was there.
        BlockState state = level.getBlockState(pos);
        burning.set(TreeUtil.isLog(state) ? state : null);
    }

    @Inject(method = "checkBurnOut", at = @At("TAIL"))
    private void treephysics$splitBurntTree(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos, @Share("burning") LocalRef<BlockState> burning) {
        BlockState before = burning.get();
        // Same block still there: the fire didn't take it this tick.
        if(before == null || !(level instanceof ServerLevel serverLevel) || level.getBlockState(pos).is(before.getBlock())) return;
        // skipWorldSplit: only trees that already fell split on burn-through; standing trees stay put.
        FloodFillUtil.blockBroken(serverLevel, pos, before, null, true);
    }
}
