package me.alpha432.oyvey.util;

import org.lwjgl.opengl.GL11;

public class RenderUtils {

    public static void drawFilledBox(double x, double y, double z) {
        // Front Face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(x, 0, 0);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(0, y, 0);
        GL11.glEnd();

        // Back Face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(0, 0, z);
        GL11.glVertex3d(0, y, z);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, 0, z);
        GL11.glEnd();

        // Top Face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(0, y, 0);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(0, y, z);
        GL11.glEnd();

        // Bottom Face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.gl
