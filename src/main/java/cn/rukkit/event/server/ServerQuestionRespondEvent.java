package cn.rukkit.event.server;

import cn.rukkit.event.ListenerList;
import cn.rukkit.game.NetworkPlayer;

public class ServerQuestionRespondEvent extends ServerEvent{
    private static ListenerList list = new ListenerList(ServerQuestionRespondEvent.class);

    public static ListenerList getListenerList() {
        return list;
    }

    public ServerQuestionRespondEvent(NetworkPlayer player, int qid, String respondMessage) {
        this.player = player;
        this.qid = qid;
        this.respondMessage = respondMessage;
    }

    public NetworkPlayer getPlayer() {
        return player;
    }

    public int getQid() {
        return qid;
    }

    public String getRespondMessage() {
        return respondMessage;
    }

    private NetworkPlayer player;

    private int qid;

    private String respondMessage;

}
