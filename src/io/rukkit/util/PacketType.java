package io.rukkit.util;

public class PacketType
{
	//Server Commands
	public static final int PACKET_REGISTER_CONNECTION = 161;
	public static final int PACKET_TEAM_LIST = 115;
	public static final int PACKET_HEART_BEAT = 108;
	public static final int PACKET_SEND_CHAT = 141;
	public static final int PACKET_SERVER_INFO = 106;
	public static final int PACKET_START_GAME = 120;
	public static final int PACKET_QUESTION = 117;
	public static final int PACKET_QUESTION_RESPONCE = 118;
	
	//Client Commands
	public static final int PACKET_PREREGISTER_CONNECTION = 160;
	public static final int PACKET_HEART_BEAT_RESPONSE = 109;
	public static final int PACKET_ADD_CHAT = 140;
	public static final int PACKET_PLAYER_INFO = 110;
	public static final int PACKET_DISCONNECT = 111;
	public static final int PACKET_RANDY = 112;
	
	//Game Commands
	public static final int PACKET_ADD_GAMECOMMAND = 20;
	public static final int PACKET_TICK = 10;
	public static final int PACKET_SYNC_CHECKSUM = 35;
}
