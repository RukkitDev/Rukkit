/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */

package cn.rukkit.game;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.rukkit.Rukkit;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.action.ExampleMoveEvent;
import cn.rukkit.event.action.MoveEvent;
import cn.rukkit.game.unit.Unit;
import cn.rukkit.network.room.ServerRoom;

public class VirtualWorld implements EventListener {
    /*
     * 借Event 完成实时同步一切
     */
    private static final Logger log = LoggerFactory.getLogger(VirtualWorld.class);
    public List<Unit> units = new ArrayList<>();
    // (start)计算可信unit id offset临时解决方案
    private boolean lockOffset = false;
    private long tmpOffset = -1;
    private int trustThresholdOffsetForUnitid = 1;
    private boolean hasTrustOffset = true;
    // (end)
    private ServerRoom room;
    // public int unparser = 0;//固定前几个id不计入解析

    public VirtualWorld(ServerRoom room) {
        Rukkit.getPluginManager().registerEventListener(this);
        this.room = room;
        if (room.playerManager.getPlayerCount() > 1) {
            trustThresholdOffsetForUnitid = room.playerManager.getPlayerCount() / 2;
        }
        // TODO: 完成自定义utils解析
    }

    // 已废弃
    public void unitMove(int id, float sx, float sy, float ex, float ey) {
        Unit curr = units.get(id);
        if (curr.pixelX == sx && curr.pixelY == sy) {// 应该有一个合理阈值 而不是严格一致
            // TODO: 终止已有的move事件(如果有)
            // TODO: 添加tick线程处理每tick的坐标变更
        }
    }

    public void moveEventHandler(MoveEvent event) {
        if (lockOffset) {
            for (long id : event.getActionUnitIds()) {
                Unit unit = Unit.getById(id, units);
                if (unit != null) {
                    log.info("unit " + unit.id + " name[" + unit.name + "]");
                }
            }
        }
        //debug using
        // event.getPlayer().getConnection().doChecksum();
        // List<NetworkPlayer> validPlayers = new ArrayList<>();
        // validPlayers.add(event.getPlayer());
        // event.getPlayer().getRoom().syncGame(validPlayers);
    }

    public void exampleMoveEventHandler(ExampleMoveEvent event) {
        if (!lockOffset) {
            // 不要试图理解这个代码在干什么
            for (Unit unit : units) {
                if (unit.isMapUnit) {
                    // log.info("{} {} {}
                    // {}",unit.pixelX,event.getStartX(),unit.pixelY,event.getStartY());
                    if (unit.pixelX == event.getStartX() && unit.pixelY == event.getStartY()) {
                        long offset = event.getActionUnitId() - unit.id;
                        if (offset > 0) {
                        }
                        if (offset == 0) {
                            lockOffset = true;
                            break;
                        }
                        if (tmpOffset == -1) {
                            tmpOffset = offset;
                            trustThresholdOffsetForUnitid--;
                        } else if (tmpOffset == offset) {
                            trustThresholdOffsetForUnitid--;
                        } else {
                            hasTrustOffset = false;
                        }
                        if (trustThresholdOffsetForUnitid <= 0) {
                            for (Unit unit2 : units) {
                                if (unit2.isMapUnit) {
                                    unit2.id = unit2.id + offset;
                                    unit2.isMapUnit = false;
                                }
                            }
                            lockOffset = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    // ===================================//
    // =========== EVENT REAL ============//
    // ===================================//
    // Event分发向正确的World
    @EventHandler
    public void onMove(MoveEvent event) {
        event.getPlayer().getRoom().world.moveEventHandler(event);
    }

    @EventHandler
    public void onExampleMove(ExampleMoveEvent event) {
        event.getPlayer().getRoom().world.exampleMoveEventHandler(event);
    }
}
