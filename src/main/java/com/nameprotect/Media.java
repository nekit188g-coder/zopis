package com.nameprotect;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Що зараз грає у Windows. Без PowerShell: раз на секунду читаємо заголовки вікон
 * (Spotify "Артист - Назва", YouTube у браузері, VLC) через JNA. Обкладинка — пошук по iTunes Search API.
 */
public final class Media {
    public static volatile String title = "", artist = "", source = "";
    public static volatile boolean playing, has;

    /** Обкладинка, готова до завантаження в текстуру (PNG 128x128) + для якого треку вона. */
    static volatile byte[] pendingPng;
    static volatile String pendingKey = "";

    private static final Set<String> BROWSERS = Set.of("chrome.exe", "msedge.exe", "firefox.exe", "brave.exe",
            "opera.exe", "opera_gx.exe", "vivaldi.exe", "yandex.exe", "browser.exe");
    private static final Pattern YT = Pattern.compile("^(?:\\(\\d+\\)\\s*)?(.+?) - (YouTube Music|YouTube)\\b");
    private static final Pattern COVER_URL = Pattern.compile("\"artworkUrl100\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern COVER_ARTIST = Pattern.compile("\"artistName\"\\s*:\\s*\"([^\"]*)\"");

    private static final ExecutorService COVERS = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nameprotect-cover");
        t.setDaemon(true);
        return t;
    });

    private static boolean started;
    private static String lastKey = "";

    private Media() {}

    public static String key() {
        return artist + "|" + title;
    }

    public static void start() {
        if (started) return;
        started = true;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        Thread t = new Thread(Media::loop, "nameprotect-media");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            try {
                if (Config.musicPlayer) poll();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private static boolean spotifyIdle(String t) {
        return t.startsWith("Spotify") && !t.contains(" - ");
    }

    private static void poll() {
        List<String[]> wins = Win.windows();
        boolean spotifyRunning = false;
        String spotifyTrack = null, yt = null, ytSrc = "YouTube", vlc = null;
        for (String[] w : wins) {
            String exe = w[0], ti = w[1];
            if (exe.equals("spotify.exe")) {
                spotifyRunning = true;
                if (!spotifyIdle(ti) && ti.contains(" - ")) spotifyTrack = ti;
            } else if (BROWSERS.contains(exe)) {
                Matcher m = YT.matcher(ti);
                if (yt == null && m.find()) {
                    yt = m.group(1).trim();
                    ytSrc = m.group(2);
                }
            } else if (exe.equals("vlc.exe") && ti.endsWith(" - VLC media player")) {
                vlc = ti.substring(0, ti.length() - " - VLC media player".length());
            }
        }

        if (spotifyTrack != null) {
            int i = spotifyTrack.indexOf(" - ");
            set(spotifyTrack.substring(0, i).trim(), spotifyTrack.substring(i + 3).trim(), "Spotify", true);
        } else if (yt != null) {
            set(ytSrc, yt, ytSrc, true);
        } else if (vlc != null && !vlc.isEmpty()) {
            set("VLC", vlc, "VLC", true);
        } else if (spotifyRunning && has && source.equals("Spotify")) {
            playing = false;          // Spotify на паузі: лишаємо останній трек
        } else {
            has = false;
            playing = false;
        }
    }

    private static void set(String a, String t, String src, boolean play) {
        artist = a;
        title = t;
        source = src;
        playing = play;
        has = true;
        String k = key();
        if (k.equals(lastKey)) return;
        lastKey = k;
        pendingPng = null;
        if (Config.mpCover && (src.equals("Spotify") || src.equals("YouTube Music"))) {
            final String term = src.equals("Spotify") ? a + " " + t : t;
            final String artistName = src.equals("Spotify") ? a : "";
            COVERS.execute(() -> fetchCover(k, term, artistName));
        }
    }

    // ---------------------------------------------------------------- обкладинка
    private static void fetchCover(String key, String term, String artistName) {
        try {
            String api = "https://itunes.apple.com/search?media=music&entity=song&limit=1&term="
                    + URLEncoder.encode(term, StandardCharsets.UTF_8);
            String json = new String(download(api), StandardCharsets.UTF_8);
            Matcher mu = COVER_URL.matcher(json);
            if (!mu.find()) return;
            if (!artistName.isEmpty()) {
                Matcher ma = COVER_ARTIST.matcher(json);
                String first = artistName.split("[,&]")[0].trim().toLowerCase();
                if (ma.find() && !first.isEmpty() && !ma.group(1).toLowerCase().contains(first)
                        && !first.contains(ma.group(1).toLowerCase())) {
                    return;   // знайшлося не те — краще без обкладинки
                }
            }
            String img = mu.group(1).replace("100x100bb", "256x256bb");
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(download(img)));
            if (src == null) return;
            BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            var gr = out.createGraphics();
            gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gr.drawImage(src, 0, 0, 128, 128, null);
            gr.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bos);
            if (key.equals(lastKey)) {          // трек ще той самий
                pendingKey = key;
                pendingPng = bos.toByteArray();
            }
        } catch (Throwable ignored) {
        }
    }

    private static byte[] download(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "nameprotect-mod");
        try (InputStream in = c.getInputStream()) {
            return in.readAllBytes();
        }
    }

    // ---------------------------------------------------------------- керування (медіаклавіші)
    public static void playPause() { key(0xB3); }

    public static void next() { key(0xB0); }

    public static void prev() { key(0xB1); }

    private static void key(int vk) {
        try {
            Win.mediaKey(vk);
        } catch (Throwable ignored) {
        }
    }
}
