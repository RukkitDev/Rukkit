package cn.rukkit.network;

import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.network.packet.Packet;

public class NetworkRoom {
    public PlayerManager playerManager;

    public RoundConfig config;
    public int stepRate = 10;
    public int currentStep = 0;
    public boolean isStarted = false;

    /**
     * Starts a room game.
     */
    public void startGame() {}

    /**
     * Broadcast a packet.
     * @param packet the packet wants to broadcast in this room.
     */
    public void broadcast(Packet packet) {}
}
