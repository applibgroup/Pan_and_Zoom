package xyz.zpayh.hdimageview.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class Build {
    private static native int native_get_int(String key, int def);
    public static final int SDK_INT = get_SDK_INT(
            "ro.build.version.sdk", 0);
    private static final boolean TRACK_KEY_ACCESS = false;

    private static int get_SDK_INT(@NotNull String key, int def) {
        if (TRACK_KEY_ACCESS) onKeyAccess(key);
        return native_get_int(key, def);
    }

    private static final HashMap<String, MutableInt> sRoReads =
            TRACK_KEY_ACCESS ? new HashMap<>() : null;

    private static void onKeyAccess(String key) {
        if (!TRACK_KEY_ACCESS) return;

        if (key != null && key.startsWith("ro.")) {
            synchronized (sRoReads) {
                MutableInt numReads = sRoReads.getOrDefault(key, null);
                if (numReads == null) {
                    numReads = new MutableInt(0);
                    sRoReads.put(key, numReads);
                }
                numReads.value++;
                if (numReads.value > 3) {
//                    Log.d(TAG, "Repeated read (count=" + numReads.value
//                                    + ") of a read-only system property '" + key + "'",
//                            new Exception());
                }
            }
        }
    }
}
