package com.nameprotect;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.minecraft.class_243;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Проекція світових координат на екран (NDC). */
public final class Proj {
    private static Matrix4fc view, proj;
    private static class_243 cam = class_243.field_1353;
    private static boolean hasTr;

    private Proj() {}

    public static void begin(WorldExtractionContext ctx) {
        view = ctx.viewMatrix();
        proj = ctx.cullProjectionMatrix();
        cam = ctx.camera().method_71156();
        hasTr = Math.abs(view.m30()) + Math.abs(view.m31()) + Math.abs(view.m32()) > 1.0e-3f;
    }

    private static Vector4f clip(double x, double y, double z) {
        Vector4f v = hasTr
                ? new Vector4f((float) x, (float) y, (float) z, 1f)
                : new Vector4f((float) (x - cam.field_1352), (float) (y - cam.field_1351), (float) (z - cam.field_1350), 1f);
        view.transform(v);
        proj.transform(v);
        return v;
    }

    /** Точка -> NDC. out = {x, y}. */
    public static boolean project(double x, double y, double z, float[] out) {
        if (view == null) return false;
        Vector4f v = clip(x, y, z);
        if (v.w <= 0.001f) return false;
        out[0] = v.x / v.w;
        out[1] = v.y / v.w;
        return true;
    }

    /** Відрізок -> NDC з обрізкою по ближній площині. out = {x1, y1, x2, y2}. */
    public static boolean segment(double x1, double y1, double z1, double x2, double y2, double z2, float[] out) {
        if (view == null) return false;
        Vector4f a = clip(x1, y1, z1);
        Vector4f b = clip(x2, y2, z2);
        final float eps = 0.05f;
        if (a.w <= eps && b.w <= eps) return false;
        if (a.w <= eps) {
            float t = (eps - a.w) / (b.w - a.w);
            a.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t, eps);
        } else if (b.w <= eps) {
            float t = (eps - b.w) / (a.w - b.w);
            b.set(b.x + (a.x - b.x) * t, b.y + (a.y - b.y) * t, b.z + (a.z - b.z) * t, eps);
        }
        out[0] = a.x / a.w;
        out[1] = a.y / a.w;
        out[2] = b.x / b.w;
        out[3] = b.y / b.w;
        return true;
    }
}
