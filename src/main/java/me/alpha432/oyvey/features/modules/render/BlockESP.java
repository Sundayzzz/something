package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockESP extends Module {

    // --- Settings ---
    private final Setting<Mode> mode = this.register(new Setting<>("Mode", Mode.Outline));
    private final Setting<Integer> radius = this.register(new Setting<>("Radius", 5, 1, 20));
    private final Setting<Boolean> chests = this.register(new Setting<>("Chests", true));
    private final Setting<Color> chestColor = this.register(new Setting<>("ChestColor", new Color(255, 0, 0, 100)));
    private final Setting<Boolean> ores = this.register(new Setting<>("Ores", true));
    private final Setting<Color> oreColor = this.register(new Setting<>("OreColor", new Color(0, 255, 0, 100)));
    private final Setting<Boolean> spawners = this.register(new Setting<>("Spawners", true));
    private final Setting<Color> spawnerColor = this.register(new Setting<>("SpawnerColor", new Color(255, 255, 0, 100)));

    // --- A list of all ore blocks for easy checking ---
    private static final Set<Block> ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE
    );

    public BlockESP() {
        super("BlockESP", "Highlights specific blocks in the world.", Category.Render);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (nullCheck()) return;

        // Use ConcurrentHashMap for thread safety if you were to add multi-threading later.
        // For a single thread loop, a regular HashSet is fine, but this is robust practice.
        Set<BlockPos> checkedBlocks = ConcurrentHashMap.newKeySet();

        // Iterate through blocks in the specified radius
        for (BlockPos pos : BlockPos.betweenClosedInclusive(
                mc.player.blockPosition().offset(-radius.getValue(), -radius.getValue(), -radius.getValue()),
                mc.player.blockPosition().offset(radius.getValue(), radius.getValue(), radius.getValue()))) {

            if (checkedBlocks.contains(pos)) continue; // Avoid re-checking blocks
            checkedBlocks.add(pos);

            Block block = mc.level.getBlockState(pos).getBlock();

            // Check if the block is one we want to render
            if (shouldRender(block)) {
                renderBlock(pos, block);
            }
        }
    }

    private boolean shouldRender(Block block) {
        if (chests.getValue() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST)) {
            return true;
        }
        if (ores.getValue() && ORE_BLOCKS.contains(block)) {
            return true;
        }
        if (spawners.getValue() && block == Blocks.SPAWNER) {
            return true;
        }
        // Add more block checks here if you expand the settings
        return false;
    }

    private void renderBlock(BlockPos pos, Block block) {
        // Get the bounding box for the block
        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
        if (shape.isEmpty()) {
            shape = VoxelShapes.block(); // Default to a full block if it has no shape (like air)
        }
        AxisAlignedBB bb = shape.bounds().move(pos);

        // Get the color for the specific block
        Color color = getColorForBlock(block);

        // Prepare for rendering
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        // Get the camera position to render from the player's view
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) camera = mc.player;
        Vector3d camPos = mc.gameRenderer.getMainCamera().getPosition();

        // Translate the rendering context to the block's position relative to the camera
        GL11.glTranslated(bb.minX - camPos.x, bb.minY - camPos.y, bb.minZ - camPos.z);

        // Set the color
        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);

        // Render based on the selected mode
        if (mode.getValue() == Mode.Outline) {
            RenderUtils.drawOutlineBox(bb.maxX - bb.minX, bb.maxY - bb.minY, bb.maxZ - bb.minZ, 1.5f);
        } else {
            RenderUtils.drawFilledBox(bb.maxX - bb.minX, bb.maxY - bb.minY, bb.maxZ - bb.minZ);
        }

        // Reset OpenGL state
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private Color getColorForBlock(Block block) {
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST) {
            return chestColor.getValue();
        }
        if (ORE_BLOCKS.contains(block)) {
            return oreColor.getValue();
        }
        if (block == Blocks.SPAWNER) {
            return spawnerColor.getValue();
        }
        return Color.WHITE; // Fallback color
    }

    public enum Mode {
        Fill, Outline
    }
}
