package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class PlayerESP extends Module {

    // --- Render Mode Settings ---
    private final Setting<Mode> mode = this.register(new Setting<>("Mode", Mode.Box));
    private final Setting<Boolean> showName = this.register(new Setting<>("ShowName", true));
    private final Setting<Boolean> showHealth = this.register(new Setting<>("ShowHealth", true));
    private final Setting<Boolean> showArmor = this.register(new Setting<>("ShowArmor", true));

    // --- Color Settings ---
    private final Setting<Color> playerColor = this.register(new Setting<>("PlayerColor", new Color(255, 255, 255, 80)));
    private final Setting<Color> friendColor = this.register(new Setting<>("FriendColor", new Color(0, 255, 100, 80)));
    private final Setting<Color> enemyColor = this.register(new Setting<>("EnemyColor", new Color(255, 0, 0, 80)));

    // --- Range Setting ---
    private final Setting<Float> range = this.register(new Setting<>("Range", 8.0f, 1.0f, 32.0f));

    public PlayerESP() {
        super("PlayerESP", "Highlights other players in the world.", Category.Render);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (nullCheck()) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity == mc.getCameraEntity()) continue;
            if (!(entity instanceof PlayerEntity)) continue;
            if (entity.distanceTo(mc.player) > range.getValue()) continue;

            PlayerEntity player = (PlayerEntity) entity;
            Color color = getColorForPlayer(player);

            switch (mode.getValue()) {
                case Box:
                    renderBox(player, color);
                    break;
                case Outline:
                    renderOutline(player, color);
                    break;
                case Skeleton:
                    renderSkeleton(player, color);
                    break;
                case Box2D:
                    render2DBox(player, color);
                    break;
            }

            // Render extra info like name, health, and armor
            if (showName.getValue() || showHealth.getValue() || showArmor.getValue()) {
                renderInfo(player, color);
            }
        }
    }

    private Color getColorForPlayer(PlayerEntity player) {
        if (OyVey.friendManager.isFriend(player)) {
            return friendColor.getValue();
        } else if (isEnemy(player)) {
            return enemyColor.getValue();
        }
        return playerColor.getValue();
    }

    // Simple enemy check: not a friend and not in the same team
    private boolean isEnemy(PlayerEntity player) {
        return !OyVey.friendManager.isFriend(player) && !mc.player.isAlliedTo(player);
    }

    private void renderBox(PlayerEntity player, Color color) {
        double x = player.getX() - mc.getRenderManager().getCameraEntity().getX();
        double y = player.getY() - mc.getRenderManager().getCameraEntity().getY();
        double z = player.getZ() - mc.getRenderManager().getCameraEntity().getZ();
        float width = player.getBbWidth();
        float height = player.getBbHeight();

        RenderUtils.prepareRender();

        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
        RenderUtils.drawFilledBox(x - width, y, z - width, x + width, y + height, z + width);

        RenderUtils.releaseRender();
    }

    private void renderOutline(PlayerEntity player, Color color) {
        double x = player.getX() - mc.getRenderManager().getCameraEntity().getX();
        double y = player.getY() - mc.getRenderManager().getCameraEntity().getY();
        double z = player.getZ() - mc.getRenderManager().getCameraEntity().getZ();
        float width = player.getBbWidth();
        float height = player.getBbHeight();

        RenderUtils.prepareRender();
        GL11.glLineWidth(2.0f);

        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 1.0f); // Full alpha for outline
        RenderUtils.drawOutlineBox(x - width, y, z - width, x + width, y + height, z + width);

        RenderUtils.releaseRender();
    }

    private void renderSkeleton(PlayerEntity player, Color color) {
        // This requires more complex GL calls to draw lines between body parts.
        // We'll use a simplified version here. A full implementation would use the player's model data.
        // For now, we'll just draw a basic stick figure.
        Vector3d pos = player.getPositionVec().subtract(mc.getRenderManager().getCameraEntity().getPositionVec());
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        RenderUtils.prepareRender();
        GL11.glLineWidth(2.0f);
        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 1.0f);

        // Head
        RenderUtils.drawCircle(x, y + player.getBbHeight() + 0.3, z, 0.3);

        // Body
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x, y + player.getBbHeight(), z);
        GL11.glVertex3d(x, y + player.getBbHeight() - 1.0, z);
        // Arms
        GL11.glVertex3d(x - 0.4, y + player.getBbHeight() - 0.8, z);
        GL11.glVertex3d(x + 0.4, y + player.getBbHeight() - 0.8, z);
        // Legs
        GL11.glVertex3d(x, y + player.getBbHeight() - 1.0, z);
        GL11.glVertex3d(x - 0.2, y, z);
        GL11.glVertex3d(x, y + player.getBbHeight() - 1.0, z);
        GL11.glVertex3d(x + 0.2, y, z);
        GL11.glEnd();

        RenderUtils.releaseRender();
    }

    private void render2DBox(PlayerEntity player, Color color) {
        // This projects the 3D box onto the 2D screen.
        // It's complex and requires the projection matrix. A simpler approach is to draw a box at the player's feet.
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Get screen coordinates
        Vector3d[] corners = get2DCorners(x, y, z, player.getBbWidth(), player.getBbHeight());
        if (corners == null) return; // Don't render if behind the camera

        RenderUtils.prepareRender();
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Draw over everything
        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);

        // Draw the filled 2D quad
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(corners[0].x, corners[0].y, 0);
        GL11.glVertex3d(corners[1].x, corners[1].y, 0);
        GL11.glVertex3d(corners[2].x, corners[2].y, 0);
        GL11.glVertex3d(corners[3].x, corners[3].y, 0);
        GL11.glEnd();

        RenderUtils.releaseRender();
    }

    private void renderInfo(PlayerEntity player, Color color) {
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() + 0.5;
        double z = player.getZ();

        Vector3d screenPos = projectToScreen(x, y, z);
        if (screenPos == null) return; // Don't render if behind the camera

        String text = player.getName().getString();
        if (showHealth.getValue()) {
            text += String.format(" (%.
