package dev.twitchmod.rules;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.tag.BlockTags;

/**
 * Tool law of the run.
 *
 * <p>Pickaxe → stone/ore/obsidian/nether/end blocks. Axe → wood. Shovel →
 * dirt/sand/gravel. Everything the tags call "hoe mineable" plus flowers,
 * crops and a thin snow layer is hand-legal. Player-placed blocks may be
 * removed with any convenient tool, but they still count as world damage.</p>
 */
public enum ToolClass {
    HAND_OK,
    AXE,
    PICKAXE,
    SHOVEL;

    public static ToolClass forBlock(BlockState state) {
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) return PICKAXE;
        if (state.isIn(BlockTags.AXE_MINEABLE)) return AXE;
        if (state.isIn(BlockTags.SHOVEL_MINEABLE)) return SHOVEL;
        return HAND_OK;
    }

    /** True when bare hands are legal for this block. */
    public static boolean handAllowed(BlockState state) {
        ToolClass t = forBlock(state);
        if (t == HAND_OK) return true;
        Block b = state.getBlock();
        // One-layer snow, foliage and plants stay hand-legal.
        if (b == Blocks.SNOW && state.contains(SnowBlock.LAYERS)) {
            return state.get(SnowBlock.LAYERS) <= 1;
        }
        return false;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return switch (this) {
            case AXE -> item instanceof AxeItem;
            case PICKAXE -> item instanceof PickaxeItem;
            case SHOVEL -> item instanceof ShovelItem;
            case HAND_OK -> true;
        };
    }

    public String key() {
        return switch (this) {
            case AXE -> "axe";
            case PICKAXE -> "pickaxe";
            case SHOVEL -> "shovel";
            case HAND_OK -> "hand";
        };
    }
}
