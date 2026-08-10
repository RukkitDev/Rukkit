/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find the license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.room;

import cn.rukkit.network.command.GameCommand;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe FIFO queue for commands waiting for the next room tick.
 *
 * <p>The queue is swapped while holding the lock and drained outside the
 * critical section. This keeps command producers independent from packet
 * serialization while preserving the order in which commands were enqueued.
 * The queue owns the lifecycle of pending commands; callers must not mutate
 * the returned list after handing it to a packet builder.</p>
 */
public final class RoomCommandQueue {
    private ArrayDeque<GameCommand> pending = new ArrayDeque<>();

    /** Adds a command to the tail of the room FIFO. */
    public void addLast(GameCommand command) {
        Objects.requireNonNull(command, "command");
        synchronized (this) {
            pending.addLast(command);
        }
    }

    /**
     * Atomically takes all commands currently pending for a tick.
     * Commands added after the swap belong to the following tick.
     */
    public List<GameCommand> drain() {
        ArrayDeque<GameCommand> batch;
        synchronized (this) {
            if (pending.isEmpty()) {
                return List.of();
            }
            batch = pending;
            pending = new ArrayDeque<>();
        }
        return new ArrayList<>(batch);
    }

    /**
     * Puts a failed batch back at the head, retaining its original order.
     */
    public void prepend(List<GameCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        synchronized (this) {
            for (int i = commands.size() - 1; i >= 0; i--) {
                pending.addFirst(Objects.requireNonNull(commands.get(i), "command"));
            }
        }
    }

    /** Removes all commands that have not yet been dispatched. */
    public synchronized void clear() {
        pending.clear();
    }

    public synchronized boolean isEmpty() {
        return pending.isEmpty();
    }

    public synchronized int size() {
        return pending.size();
    }
}
