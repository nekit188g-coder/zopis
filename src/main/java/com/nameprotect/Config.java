package com.nameprotect;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class Config {
    public static boolean targetEsp = true;
    public static int mode = 0;          // 0..7 = Radar..Sardelki
    public static int size = 1;          // 0 = S, 1 = M, 2 = L
    public static boolean spin = true;
    public static int hitSound = 0;      // 0 = Off, 1 = Undertale, 2 = Bubble
    public static int volume = 2;        // 0 = 25%, 1 = 50%, 2 = 100%
    public static boolean killEffect = true;
    public static boolean killSound = true;
    public static boolean nameProtect = true;
    public static boolean watermark = true;
    public static boolean espIgnoreInvisible = true;   // TargetESP не показується на невидимках
    public static boolean targetHud = true;
    public static boolean hudArmor = true;
    public static boolean musicPlayer = true;
    public static boolean mpCover = true;              // обкладинка з iTunes Search API (онлайн)
    public static int wmX = -1, wmY = -1;              // позиція Watermark (-1 = по центру зверху)
    public static int mpX = -1, mpY = -1;              // позиція MusicPlayer (-1 = справа внизу)
    public static int hudX = -1;                       // -1 = стандартна позиція (як на скріні)
    public static int hudY = -1;

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nameprotect.properties");
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean b(Properties p, String k, boolean d) {
        return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d)));
    }

    private static int i(Properties p, String k, int d, int lo, int hi) {
        try {
            return clamp(Integer.parseInt(p.getProperty(k, String.valueOf(d))), lo, hi);
        } catch (Exception e) {
            return d;
        }
    }

    public static void load() {
        try {
            Path p = file();
            if (!Files.exists(p)) return;
            Properties pr = new Properties();
            try (InputStream in = Files.newInputStream(p)) {
                pr.load(in);
            }
            targetEsp = b(pr, "targetEsp", true);
            mode = i(pr, "mode", 0, 0, 7);
            size = i(pr, "size", 1, 0, 2);
            spin = b(pr, "spin", true);
            hitSound = i(pr, "hitSound", 0, 0, 2);
            volume = i(pr, "volume", 2, 0, 2);
            killEffect = b(pr, "killEffect", true);
            killSound = b(pr, "killSound", true);
            nameProtect = b(pr, "nameProtect", true);
            watermark = b(pr, "watermark", true);
            espIgnoreInvisible = b(pr, "espIgnoreInvisible", true);
            targetHud = b(pr, "targetHud", true);
            hudArmor = b(pr, "hudArmor", true);
            hudX = i(pr, "hudX", -1, -1, 4000);
            hudY = i(pr, "hudY", -1, -1, 4000);
            musicPlayer = b(pr, "musicPlayer", true);
            mpCover = b(pr, "mpCover", true);
            wmX = i(pr, "wmX", -1, -1, 4000);
            wmY = i(pr, "wmY", -1, -1, 4000);
            mpX = i(pr, "mpX", -1, -1, 4000);
            mpY = i(pr, "mpY", -1, -1, 4000);
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties pr = new Properties();
            pr.setProperty("targetEsp", String.valueOf(targetEsp));
            pr.setProperty("mode", String.valueOf(mode));
            pr.setProperty("size", String.valueOf(size));
            pr.setProperty("spin", String.valueOf(spin));
            pr.setProperty("hitSound", String.valueOf(hitSound));
            pr.setProperty("volume", String.valueOf(volume));
            pr.setProperty("killEffect", String.valueOf(killEffect));
            pr.setProperty("killSound", String.valueOf(killSound));
            pr.setProperty("nameProtect", String.valueOf(nameProtect));
            pr.setProperty("watermark", String.valueOf(watermark));
            pr.setProperty("espIgnoreInvisible", String.valueOf(espIgnoreInvisible));
            pr.setProperty("targetHud", String.valueOf(targetHud));
            pr.setProperty("hudArmor", String.valueOf(hudArmor));
            pr.setProperty("hudX", String.valueOf(hudX));
            pr.setProperty("hudY", String.valueOf(hudY));
            pr.setProperty("musicPlayer", String.valueOf(musicPlayer));
            pr.setProperty("mpCover", String.valueOf(mpCover));
            pr.setProperty("wmX", String.valueOf(wmX));
            pr.setProperty("wmY", String.valueOf(wmY));
            pr.setProperty("mpX", String.valueOf(mpX));
            pr.setProperty("mpY", String.valueOf(mpY));
            try (OutputStream out = Files.newOutputStream(file())) {
                pr.store(out, "NameProtect / LoLHelper");
            }
        } catch (Exception ignored) {
        }
    }
}
