package cn.rukkit.network.core.packet;

public class PacketType{
    public static final int REGISTER_CONNECTION = 161;//A1
	public static final int TEAM_LIST = 115;//73
	public static final int HEART_BEAT = 108;//6C
	public static final int SEND_CHAT = 141;//8D
	public static final int SERVER_INFO = 106;//6A
	public static final int START_GAME = 120;//78
	public static final int QUESTION = 117;//75
	public static final int QUESTION_RESPONCE = 118;//76
	public static final int KICK = 150;
	public static final int RETURN_TO_BATTLEROOM = 122;

	//Client Commands
	public static final int PREREGISTER_CONNECTION = 160;//A0
	public static final int HEART_BEAT_RESPONSE = 109;//6D 心跳包应答
	public static final int ADD_CHAT = 140;//8C
	public static final int PLAYER_INFO = 110;//6E hps: REGISTER_PLAYER
	public static final int DISCONNECT = 111;//6F
	public static final int READY = 112;//70 原RANDY改READY RW-HPS 给出的是ACCEPT_START_GAME


	//Game Commands
	public static final int ADD_GAMECOMMAND = 20;//14
	public static final int TICK = 10;//0A
	public static final int SYNC_CHECKSUM = 30;//1E
	public static final int SYNC_CHECKSUM_RESPONCE = 31;//1F
    public static final int SYNC = 35;//23

	//Relay Commands
	public static final int RELAY_117 = 117;
    public static final int RELAY_118_117_RETURN = 118;
    public static final int RELAY_POW = 151;
    public static final int RELAY_POW_RECEIVE = 152;

    public static final int RELAY_VERSION_INFO = 163;//A3
    public static final int RELAY_BECOME_SERVER = 170;//让c端变"s"端
    public static final int FORWARD_CLIENT_ADD = 172;
    public static final int FORWARD_CLIENT_REMOVE = 173;
    public static final int PACKET_FORWARD_CLIENT_FROM = 174;
    public static final int PACKET_FORWARD_CLIENT_TO = 175;
    public static final int PACKET_FORWARD_CLIENT_TO_REPEATED = 176;
    public static final int PACKET_RECONNECT_TO = 178;
}