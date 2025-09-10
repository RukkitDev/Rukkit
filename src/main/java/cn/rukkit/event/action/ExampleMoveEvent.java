package cn.rukkit.event.action;

import cn.rukkit.event.Cancellable;
import cn.rukkit.event.ListenerList;
import cn.rukkit.game.NetworkPlayer;

//这个是用来为VirtualWorld提供在游戏前期提供unit-Id offset的
//为什么Example 可以参阅GameCommand
//
//**这个Event{具有耦合性} 强烈建议不要使用**
//           ------------
public class ExampleMoveEvent extends ActionEvent implements Cancellable {
    private static ListenerList list = new ListenerList(ExampleMoveEvent.class);

    public static ListenerList getListenerList() {
        return list;
    }

    private NetworkPlayer player;
    private float targetX;
    private float targetY;
    private float startX;
    private float startY;
    private long actionUnitId;

    public static class moveingUnitInfo {
        public float sx;// start x
        public float sy;// start y
        public float ex;// end x
        public float ey;// end y
        public long actionUnitId;

        public moveingUnitInfo(float startX, float startY, float endX, float endY, long unitId) {
            this.sx = startX;
            this.sy = startY;
            this.ex = endX;
            this.ey = endY;
            this.actionUnitId = unitId;
        }
    }

    public ExampleMoveEvent(NetworkPlayer p, moveingUnitInfo mInfo) {
        this.player = p;
        this.startX = mInfo.sx;
        this.startY = mInfo.sy;
        this.targetX = mInfo.ey;
        this.targetY = mInfo.ey;
        this.actionUnitId = mInfo.actionUnitId;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public long getActionUnitId() {
        return this.actionUnitId;
    }

    public NetworkPlayer getPlayer() {
        return this.player;
    }
}
