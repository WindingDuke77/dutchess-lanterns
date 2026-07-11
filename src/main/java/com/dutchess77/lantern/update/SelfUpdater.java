package com.dutchess77.lantern.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.zip.ZipFile;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/**
 * Self-update from GitHub releases, so nobody re-downloads jars by hand.
 * Runs once on a background thread: fetches releases/latest, and when it is
 * strictly newer than Lantern.VERSION downloads the jar next to the current
 * one as *.jar.update (Forge ignores the suffix). The swap happens at JVM
 * exit - Windows locks a loaded jar, so a detached helper script waits for
 * the lock to drop, replaces the jar and deletes itself; POSIX swaps
 * directly in the shutdown hook. Fail-soft: any error just logs and the
 * game runs on the current version.
 */
public final class SelfUpdater {

    private static final String API_LATEST =
        "https://api.github.com/repos/WindingDuke77/dutchess-lanterns/releases/latest";
    private static final int MAX_JAR_BYTES = 32 * 1024 * 1024;

    /** Set once a new jar is staged - the client notifier chats it to the player. */
    public static volatile String pendingVersion;

    private SelfUpdater() {
    }

    public static void start() {
        if (!LanternConfig.autoUpdate) {
            return;
        }
        Thread thread = new Thread(SelfUpdater::run, "lantern-self-update");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run() {
        try {
            ModContainer self = Loader.instance().getIndexedModList().get(Lantern.MODID);
            File currentJar = self == null ? null : self.getSource();
            if (currentJar == null || !currentJar.isFile() || !currentJar.getName().endsWith(".jar")) {
                return; // dev environment or unexpected layout
            }

            JsonObject release = fetchLatestRelease();
            String remoteVersion = release.get("tag_name").getAsString().replaceFirst("^v", "");
            if (!isNewer(remoteVersion, Lantern.VERSION)) {
                return;
            }
            JsonObject asset = findJarAsset(release);
            if (asset == null) {
                return;
            }

            File staged = new File(currentJar.getParentFile(), asset.get("name").getAsString() + ".update");
            download(asset.get("browser_download_url").getAsString(), staged);
            if (!looksLikeOurJar(staged)) {
                staged.delete();
                Lantern.LOGGER.warn("Self-update: downloaded jar failed validation, discarded");
                return;
            }

            File target = new File(currentJar.getParentFile(), asset.get("name").getAsString());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> swapOnExit(currentJar, staged, target)));
            pendingVersion = remoteVersion;
            Lantern.LOGGER.info("Self-update: {} staged, installs when the game closes", remoteVersion);
        } catch (Throwable t) {
            Lantern.LOGGER.info("Self-update check failed (staying on {}): {}", Lantern.VERSION, t.toString());
        }
    }

    private static JsonObject fetchLatestRelease() throws Exception {
        HttpURLConnection conn = open(API_LATEST);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } finally {
            conn.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        SSLSocketFactory trusted = osTrustedFactory();
        if (trusted != null && conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(trusted);
        }
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "dutchess-lanterns/" + Lantern.VERSION);
        return conn;
    }

    private static SSLSocketFactory cachedFactory;
    private static boolean factoryResolved;

    /**
     * The pack's bundled JRE (8u51) predates the cert roots GitHub's asset CDN
     * uses - its default trust store rejects the download (verified on
     * jre-legacy directly). On Windows, trust the OS root store instead, which
     * Windows keeps current. Null means "use the JVM default" (POSIX/dev).
     */
    private static synchronized SSLSocketFactory osTrustedFactory() {
        if (factoryResolved) {
            return cachedFactory;
        }
        factoryResolved = true;
        try {
            KeyStore roots = KeyStore.getInstance("Windows-ROOT");
            roots.load(null, null);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(roots);
            SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, tmf.getTrustManagers(), null);
            cachedFactory = ctx.getSocketFactory();
        } catch (Throwable t) {
            cachedFactory = null;
        }
        return cachedFactory;
    }

    private static JsonObject findJarAsset(JsonObject release) {
        JsonArray assets = release.getAsJsonArray("assets");
        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            if (asset.get("name").getAsString().matches("lantern-[\\d.]+\\.jar")) {
                return asset;
            }
        }
        return null;
    }

    /** Strictly-newer numeric compare so a rolled-back release never causes a downgrade loop. */
    static boolean isNewer(String remote, String local) {
        String[] r = remote.split("\\.");
        String[] l = local.split("\\.");
        for (int i = 0; i < Math.max(r.length, l.length); i++) {
            int ri = i < r.length ? Integer.parseInt(r[i]) : 0;
            int li = i < l.length ? Integer.parseInt(l[i]) : 0;
            if (ri != li) {
                return ri > li;
            }
        }
        return false;
    }

    private static void download(String url, File destination) throws Exception {
        // follow redirects by hand so each hop (github.com -> asset CDN) gets
        // the OS-trusted socket factory, not the JVM default
        HttpURLConnection conn = null;
        for (int hop = 0; hop < 5; hop++) {
            conn = open(url);
            conn.setInstanceFollowRedirects(false);
            int code = conn.getResponseCode();
            if (code / 100 == 3) {
                url = conn.getHeaderField("Location");
                conn.disconnect();
                continue;
            }
            if (code != 200) {
                throw new IllegalStateException("HTTP " + code + " downloading update");
            }
            break;
        }
        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_JAR_BYTES) {
                    throw new IllegalStateException("release asset larger than " + MAX_JAR_BYTES + " bytes");
                }
                out.write(buffer, 0, read);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static boolean looksLikeOurJar(File jar) {
        try (ZipFile zip = new ZipFile(jar)) {
            return zip.getEntry("mcmod.info") != null
                && zip.getEntry("com/dutchess77/lantern/Lantern.class") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void swapOnExit(File currentJar, File staged, File target) {
        try {
            if (!staged.isFile()) {
                return;
            }
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                spawnWindowsSwapHelper(currentJar, staged, target);
            } else {
                // POSIX lets us replace open files directly
                currentJar.delete();
                staged.renameTo(target);
            }
        } catch (Throwable ignored) {
            // never block JVM shutdown
        }
    }

    /** Detached batch script: waits for the JVM's jar lock to drop, swaps, self-deletes. */
    private static void spawnWindowsSwapHelper(File currentJar, File staged, File target) throws Exception {
        File script = File.createTempFile("lantern-update-", ".bat");
        try (PrintWriter out = new PrintWriter(script, "US-ASCII")) {
            out.println("@echo off");
            out.println("set tries=0");
            out.println(":wait");
            out.println("del \"" + currentJar.getAbsolutePath() + "\" 2>nul");
            out.println("if not exist \"" + currentJar.getAbsolutePath() + "\" goto swap");
            out.println("set /a tries+=1");
            out.println("if %tries% geq 60 goto cleanup");
            out.println("ping -n 2 127.0.0.1 >nul");
            out.println("goto wait");
            out.println(":swap");
            out.println("move /y \"" + staged.getAbsolutePath() + "\" \"" + target.getAbsolutePath() + "\" >nul");
            out.println(":cleanup");
            out.println("del \"%~f0\"");
        }
        new ProcessBuilder("cmd", "/c", "start", "lantern_update", "/min", script.getAbsolutePath()).start();
    }
}
