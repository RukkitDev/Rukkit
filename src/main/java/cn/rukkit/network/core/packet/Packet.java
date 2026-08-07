/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.packet;

/**
 * A length-prefixed game packet payload and its protocol type.
 *
 * <p>The wire format is a four-byte payload length, followed by a four-byte
 * packet type and the payload bytes. The class intentionally keeps the fields
 * mutable because packet builders in the existing network stack fill the
 * payload after constructing the packet.</p>
 */
public class Packet {
    /** Sentinel used when the framed protocol should not impose a payload limit. */
    public static final int NO_MAX_FRAME_LENGTH = Integer.MAX_VALUE;

    /** The first migration stage deliberately leaves the payload size unlimited. */
    public static final int DEFAULT_MAX_FRAME_LENGTH = NO_MAX_FRAME_LENGTH;

    public byte[] bytes;
    public int type;

    public Packet(int type) {
        this.type = type;
    }

    public Packet(int type, byte[] bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    public Packet() {
        this(0);
    }
}
