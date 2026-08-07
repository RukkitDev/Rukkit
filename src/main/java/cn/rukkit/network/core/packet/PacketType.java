package cn.rukkit.network.core.packet;

/** Packet type identifiers used by the game protocol. */
public final class PacketType {
    private PacketType() {
    }

    // Server commands
    public static final int REGISTER_CONNECTION = 161;
    public static final int TEAM_LIST = 115;
    public static final int HEART_BEAT = 108;
    public static final int SEND_CHAT = 141;
    public static final int SERVER_INFO = 106;
    public static final int START_GAME = 120;
    public static final int QUESTION = 117;
    public static final int QUESTION_RESPONCE = 118;
    public static final int QUESTION_RESPONSE = QUESTION_RESPONCE;
    public static final int KICK = 150;
    public static final int RETURN_TO_BATTLEROOM = 122;

    // Client commands
    public static final int PREREGISTER_CONNECTION = 160;
    public static final int HEART_BEAT_RESPONSE = 109;
    public static final int ADD_CHAT = 140;
    public static final int PLAYER_INFO = 110;
    public static final int DISCONNECT = 111;
    public static final int READY = 112;

    // Game commands
    public static final int ADD_GAMECOMMAND = 20;
    public static final int TICK = 10;
    public static final int SYNC_CHECKSUM = 30;
    public static final int SYNC_CHECKSUM_RESPONCE = 31;
    public static final int SYNC_CHECKSUM_RESPONSE = SYNC_CHECKSUM_RESPONCE;
    public static final int SYNC = 35;

    // Relay commands
    public static final int RELAY_117 = 117;
    public static final int RELAY_118_117_RETURN = 118;
    public static final int RELAY_POW = 151;
    public static final int RELAY_POW_RECEIVE = 152;
    public static final int RELAY_VERSION_INFO = 163;
    public static final int RELAY_BECOME_SERVER = 170;
    public static final int FORWARD_CLIENT_ADD = 172;
    public static final int FORWARD_CLIENT_REMOVE = 173;
    public static final int PACKET_FORWARD_CLIENT_FROM = 174;
    public static final int PACKET_FORWARD_CLIENT_TO = 175;
    public static final int PACKET_FORWARD_CLIENT_TO_REPEATED = 176;
    public static final int PACKET_RECONNECT_TO = 178;
}
