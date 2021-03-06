package me.jellysquid.mods.phosphor.mixin.chunk.light;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedBlockState;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedChunkLightProvider;
import me.jellysquid.mods.phosphor.common.util.cache.LightEngineBlockAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkLightProvider.class)
public class MixinChunkLightProvider<M extends ChunkToNibbleArrayMap<M>, S extends LightStorage<M>> implements ExtendedChunkLightProvider {
    @Shadow
    @Final
    protected BlockPos.Mutable reusableBlockPos;

    @Shadow
    @Final
    protected ChunkProvider chunkProvider;

    private LightEngineBlockAccess blockAccess;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(ChunkProvider provider, LightType lightType, S storage, CallbackInfo ci) {
        this.blockAccess = new LightEngineBlockAccess(provider);
    }

    @Inject(method = "clearChunkCache", at = @At("RETURN"))
    private void onCleanup(CallbackInfo ci) {
        // This callback may be executed from the constructor above, and the object won't be initialized then
        if (this.blockAccess != null) {
            this.blockAccess.reset();
        }
    }

    // [VanillaCopy] method_20479
    @Override
    public BlockState getBlockStateForLighting(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return Blocks.AIR.getDefaultState();
        }

        return this.blockAccess.getBlockState(x, y, z);
    }

    // [VanillaCopy] method_20479
    @Override
    public int getSubtractedLight(BlockState state, int x, int y, int z) {
        ExtendedBlockState estate = ((ExtendedBlockState) state);

        if (estate.hasCachedLightOpacity()) {
            return estate.getCachedLightOpacity();
        } else {
            return estate.getDynamicLightOpacity(this.chunkProvider.getWorld(), this.reusableBlockPos.set(x, y, z));
        }
    }

    // [VanillaCopy] method_20479
    @Override
    public VoxelShape getOpaqueShape(BlockState state, int x, int y, int z, Direction dir) {
        if (state == null) {
            return VoxelShapes.empty();
        }

        ExtendedBlockState estate = ((ExtendedBlockState) state);

        VoxelShape shape = estate.getCachedExtrudedFace(dir);

        if (shape != null) {
            return shape;
        }

        return estate.getDynamicExtrudedFace(this.chunkProvider.getWorld(), this.reusableBlockPos.set(x, y, z), dir);
    }

    // [VanillaCopy] method_20479
    @Override
    public VoxelShape getOpaqueShape(int x, int y, int z, Direction dir) {
        if (y < 0 || y >= 256) {
            return VoxelShapes.empty();
        }

        BlockState state = this.blockAccess.getBlockState(x, y, z);

        if (state == null) {
            return VoxelShapes.fullCube();
        }

        return this.getOpaqueShape(state, x, y, z, dir);
    }
}
