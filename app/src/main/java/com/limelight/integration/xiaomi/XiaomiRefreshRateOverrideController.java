package com.limelight.integration.xiaomi;

import android.content.Context;
import android.os.Build;

import com.limelight.utils.concurrent.LatestTaskExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Applies Xiaomi's per-game refresh-rate override without changing whether
 * the application participates in Game Turbo.
 *
 * <p>The controller removes only this package from PowerKeeper's refresh-rate
 * game lists. Joyose classification and Game Turbo membership are separate
 * state and remain untouched, allowing the ordinary adaptive display policy
 * to choose the refresh rate.</p>
 */
public final class XiaomiRefreshRateOverrideController {
    private static final String POWERKEEPER_PACKAGE =
            "com.miui.powerkeeper";
    private static final String HIGH_FPS_ACTION =
            "com.xiaomi.joyose.HIGH_FPS_LIST";
    private static final String TOP_GAME_ACTION =
            "intent.action.TOP_GAME_LIST";
    private static final String POWERKEEPER_DUMP_COMMAND =
            "dumpsys activity service com.miui.powerkeeper";
    private static final long ROOT_COMMAND_TIMEOUT_SECONDS = 30;
    private static final Pattern SAFE_PACKAGE_NAME = Pattern.compile(
            "[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");
    private static final LatestTaskExecutor EXECUTOR =
            new LatestTaskExecutor("xiaomi-refresh-rate-override");

    public interface Callback {
        void onComplete(Result result);
    }

    public enum Result {
        APPLIED,
        ROOT_DENIED_OR_UNAVAILABLE,
        FAILED
    }

    private XiaomiRefreshRateOverrideController() {
    }

    public static boolean isSupportedDevice() {
        String identity = (String.valueOf(Build.MANUFACTURER) + " " +
                String.valueOf(Build.BRAND)).toLowerCase(Locale.US);
        return identity.contains("xiaomi") ||
                identity.contains("redmi") ||
                identity.contains("poco");
    }

    public static void apply(
            Context context,
            boolean suppressLimit,
            Callback callback) {
        Context applicationContext = context.getApplicationContext();
        Objects.requireNonNull(callback, "callback");
        EXECUTOR.execute(
                () -> callback.onComplete(applyBlocking(
                        applicationContext,
                        suppressLimit)),
                () -> callback.onComplete(Result.FAILED));
    }

    static Result applyBlocking(
            Context context,
            boolean suppressLimit) {
        if (!isSupportedDevice()) {
            return Result.FAILED;
        }
        String packageName = context.getPackageName();
        if (!SAFE_PACKAGE_NAME.matcher(packageName).matches()) {
            return Result.FAILED;
        }
        CommandResult query = runRootCommand(POWERKEEPER_DUMP_COMMAND);
        if (!query.succeeded()) {
            return Result.ROOT_DENIED_OR_UNAVAILABLE;
        }
        PowerKeeperState state = PowerKeeperState.parse(query.output);
        if (state == null) {
            return Result.FAILED;
        }
        String command = buildPolicyUpdateCommand(
                packageName,
                state,
                suppressLimit);
        CommandResult update = runRootCommand(command);
        return update.succeeded()
                ? Result.APPLIED
                : Result.ROOT_DENIED_OR_UNAVAILABLE;
    }

    static String buildPolicyUpdateCommand(
            String packageName,
            PowerKeeperState original,
            boolean suppressLimit) {
        if (!SAFE_PACKAGE_NAME.matcher(packageName).matches()) {
            throw new IllegalArgumentException("Unsafe package name");
        }
        LinkedHashMap<String, Integer> highFps =
                new LinkedHashMap<>(original.highFps);
        LinkedHashSet<String> defaultGames =
                new LinkedHashSet<>(original.defaultGames);
        highFps.remove(packageName);
        if (suppressLimit) {
            defaultGames.remove(packageName);
        }
        else {
            defaultGames.add(packageName);
        }

        String highFpsValue = joinHighFps(highFps);
        String defaultGamesValue = String.join(",", defaultGames);
        return "am broadcast --user current --receiver-foreground" +
                " -p " + POWERKEEPER_PACKAGE +
                " -a " + HIGH_FPS_ACTION +
                " --esal high_fps_list " + shellValue(highFpsValue) +
                " && am broadcast --user current --receiver-foreground" +
                " -p " + POWERKEEPER_PACKAGE +
                " -a " + TOP_GAME_ACTION +
                " --ez isAppend false" +
                " --es gameList " + shellValue(defaultGamesValue);
    }

    private static String joinHighFps(Map<String, Integer> values) {
        List<String> entries = new ArrayList<>(values.size());
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (SAFE_PACKAGE_NAME.matcher(entry.getKey()).matches() &&
                    entry.getValue() != null &&
                    entry.getValue() > 0 &&
                    entry.getValue() <= 1000) {
                entries.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        return String.join(",", entries);
    }

    private static String shellValue(String value) {
        if (value.isEmpty()) {
            return "''";
        }
        if (!value.matches("[A-Za-z0-9_.,:]+")) {
            throw new IllegalArgumentException("Unsafe vendor list value");
        }
        return value;
    }

    private static CommandResult runRootCommand(String command) {
        Process process = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            Process runningProcess = process;
            Thread reader = new Thread(
                    () -> copy(runningProcess.getInputStream(), output),
                    "xiaomi-refresh-rate-root-output");
            reader.setDaemon(true);
            reader.start();
            long deadlineNanos = System.nanoTime() +
                    ROOT_COMMAND_TIMEOUT_SECONDS * 1_000_000_000L;
            while (isRunning(process) &&
                    System.nanoTime() < deadlineNanos) {
                Thread.sleep(50);
            }
            if (isRunning(process)) {
                process.destroy();
                reader.join(1000);
                return new CommandResult(true, -1, output.toString(
                        StandardCharsets.UTF_8.name()));
            }
            reader.join(1000);
            return new CommandResult(
                    true,
                    process.exitValue(),
                    output.toString(StandardCharsets.UTF_8.name()));
        }
        catch (IOException error) {
            return new CommandResult(false, -1, "");
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return new CommandResult(process != null, -1, "");
        }
        finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static boolean isRunning(Process process) {
        try {
            process.exitValue();
            return false;
        }
        catch (IllegalThreadStateException ignored) {
            return true;
        }
    }

    private static void copy(
            InputStream input,
            ByteArrayOutputStream output) {
        byte[] buffer = new byte[8192];
        int count;
        try {
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
        }
        catch (IOException ignored) {
            // The process may be forcibly closed after the bounded timeout.
        }
    }

    static final class PowerKeeperState {
        private static final String HIGH_FPS_PREFIX =
                "fpsconfig mPrivGames=";
        private static final String DEFAULT_GAMES_PREFIX =
                "fpsconfig mDefaultGameApp(60):";
        final LinkedHashMap<String, Integer> highFps;
        final LinkedHashSet<String> defaultGames;

        PowerKeeperState(
                LinkedHashMap<String, Integer> highFps,
                LinkedHashSet<String> defaultGames) {
            this.highFps = highFps;
            this.defaultGames = defaultGames;
        }

        static PowerKeeperState parse(String dump) {
            String highFpsBody = findSetBody(dump, HIGH_FPS_PREFIX);
            String defaultGamesBody = findSetBody(
                    dump,
                    DEFAULT_GAMES_PREFIX);
            if (highFpsBody == null || defaultGamesBody == null) {
                return null;
            }
            LinkedHashMap<String, Integer> highFps =
                    new LinkedHashMap<>();
            if (!highFpsBody.trim().isEmpty()) {
                for (String item : highFpsBody.split(",")) {
                    String[] pair = item.trim().split("=", 2);
                    if (pair.length != 2 ||
                            !SAFE_PACKAGE_NAME.matcher(pair[0]).matches()) {
                        continue;
                    }
                    try {
                        highFps.put(
                                pair[0],
                                Integer.parseInt(pair[1]));
                    }
                    catch (NumberFormatException ignored) {
                    }
                }
            }
            LinkedHashSet<String> defaultGames =
                    new LinkedHashSet<>();
            if (!defaultGamesBody.trim().isEmpty()) {
                for (String item : defaultGamesBody.split(",")) {
                    String packageName = item.trim();
                    if (SAFE_PACKAGE_NAME.matcher(packageName).matches()) {
                        defaultGames.add(packageName);
                    }
                }
            }
            return new PowerKeeperState(highFps, defaultGames);
        }

        private static String findSetBody(
                String dump,
                String prefix) {
            int prefixIndex = dump.indexOf(prefix);
            if (prefixIndex < 0) {
                return null;
            }
            int open = dump.indexOf('{', prefixIndex + prefix.length());
            int close = open < 0 ? -1 : dump.indexOf('}', open + 1);
            return open < 0 || close < 0
                    ? null
                    : dump.substring(open + 1, close);
        }
    }

    private static final class CommandResult {
        final boolean started;
        final int exitCode;
        final String output;

        CommandResult(boolean started, int exitCode, String output) {
            this.started = started;
            this.exitCode = exitCode;
            this.output = output;
        }

        boolean succeeded() {
            return started && exitCode == 0;
        }
    }
}
