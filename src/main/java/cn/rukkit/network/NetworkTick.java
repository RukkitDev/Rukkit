/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find the license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

import java.util.concurrent.TimeUnit;

/** Shared timing constants for the game's network tick window. */
public final class NetworkTick {
    /** The original protocol advances ten simulation frames per TICK packet. */
    public static final int FRAMES_PER_WINDOW = 10;

    /**
     * Ten frames at the original 60 simulation frames per second. The
     * nanosecond period avoids accumulating millisecond rounding error.
     */
    public static final long WINDOW_PERIOD_NANOS = TimeUnit.SECONDS.toNanos(1) / 6;

    /** Human-readable approximation retained for existing room diagnostics. */
    public static final int WINDOW_PERIOD_MILLIS = 167;

    private NetworkTick() {
    }
}
