/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet;

import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.PingType;
import cn.rukkit.game.mod.Mod;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.command.GameCommand;

import java.util.ArrayList;

/**
 * 游戏协议发送抽象层。
 * <p>
 * 该抽象层用于屏蔽具体协议细节（字段顺序、压缩块结构、包类型常量），
 * 上层业务只需要按“业务语义”构建包，然后交由网络层发送。
 * </p>
 *
 * <h3>设计目标</h3>
 * <ul>
 *     <li>统一所有服务器->客户端的核心包构建入口</li>
 *     <li>将房间维度（{@link NetworkRoom}）与步进维度（step）两套接口统一表达</li>
 *     <li>允许未来按协议版本提供不同实现</li>
 * </ul>
 *
 * <h3>实现建议</h3>
 * <ul>
 *     <li>实现类应保持“无副作用”：只负责构造 {@link Packet}，不直接发送</li>
 *     <li>若底层序列化存在 {@code IOException}，建议在实现内转为运行时异常或统一错误类型</li>
 *     <li>不应在实现中直接读写全局状态（除默认资源兜底）</li>
 * </ul>
 *
 * <h3>状态约束（建议）</h3>
 * <ul>
 *     <li>{@code preRegister/chat/serverInfo}：连接建立/房间阶段</li>
 *     <li>{@code gameCommand/emptyCommand/gamePing/gameSummon/...}：仅游戏中阶段</li>
 *     <li>{@code sendSave/sendPullSave/syncCheckSum}：同步流程阶段</li>
 * </ul>
 *
 * @since 0.9.2
 */
public abstract class GamePacket {
    /**
     * 构建聊天包（服务器消息/玩家消息统一入口）。
     *
     * @param from 发送者名称（如 {@code SERVER} 或玩家名）
     * @param msg  消息正文
     * @param team 队伍标识，通常 {@code -1} 表示全局/系统消息
     * @return 聊天包
     */
    public abstract Packet chat(String from, String msg, int team);

    /**
     * 构建心跳包。
     *
     * @return 心跳包
     */
    public abstract Packet ping();

    /**
     * 构建预注册响应包（握手阶段）。
     *
     * @return 预注册包
     */
    public abstract Packet preRegister();

    /**
     * 构建踢出包。
     *
     * @param reason 踢出原因
     * @return 踢出包
     */
    public abstract Packet kick(String reason);

    /**
     * 构建“按 step”发送存档包。
     *
     * @param step       服务器当前步进
     * @param bArr       存档二进制数据
     * @param isPullSave 是否为拉档流程
     * @return 同步包（包含存档）
     */
    public abstract Packet sendSave(int step, byte[] bArr, boolean isPullSave);

    /**
     * 构建“按 step”拉档请求包。
     *
     * @param step 服务器当前步进
     * @return 同步包（拉档请求）
     */
    public abstract Packet sendPullSave(int step);

    /**
     * 构建校验同步请求包。
     *
     * @param step 服务器当前步进
     * @return 校验同步请求包
     */
    public abstract Packet syncCheckSum(int step);

    /**
     * 构建游戏指令广播包。
     *
     * @param step 服务器当前步进
     * @param cmd  游戏指令
     * @return 游戏指令包
     */
    public abstract Packet gameCommand(int step, GameCommand cmd);

    /**
     * 构建空指令包（用于维持 tick 推进）。
     *
     * @param step 服务器当前步进
     * @return 空游戏指令包
     */
    public abstract Packet emptyCommand(int step);

    /**
     * 构建游戏内地图 Ping 包。
     *
     * @param step  服务器当前步进
     * @param index 发起者槽位
     * @param type  Ping 类型
     * @param x     地图坐标 X
     * @param y     地图坐标 Y
     * @return 游戏 Ping 指令包
     */
    public abstract Packet gamePing(int step, int index, PingType type, float x, float y);

    /**
     * 构建召唤单位指令包。
     *
     * @param step 服务器当前步进
     * @param unit 单位标识（内置或自定义）
     * @param x    地图坐标 X
     * @param y    地图坐标 Y
     * @param team 队伍标识，{@code -1} 通常表示不强制队伍
     * @return 召唤单位指令包
     */
    public abstract Packet gameSummon(int step, String unit, float x, float y, int team);

    /**
     * 构建投降（surround）指令包。
     * <p>注意：当前历史命名为 {@code gameSurrender}，底层常见实现可能对应 {@code gameSurrounder}。</p>
     *
     * @param step  服务器当前步进
     * @param index 玩家槽位
     * @return 投降指令包
     */
    public abstract Packet gameSurrender(int step, int index);

    /**
     * 构建服务器提问包（问答系统）。
     *
     * @param qid      问题 ID
     * @param question 问题内容
     * @return 提问包
     */
    public abstract Packet packetQuestion(int qid, String question);

    /**
     * 构建返回房间包。
     *
     * @return 返回房间包
     */
    public abstract Packet packetReturnToBattleroom();

    /**
     * 构建完整房间配置同步包。
     *
     * @param config  房间配置
     * @param isAdmin 是否以房主视角下发
     * @param units   可用单位列表（含模组单位）
     * @return 房间信息包
     */
    public abstract Packet serverInfo(RoundConfig config, boolean isAdmin, ArrayList<Mod.ModUnit> units);

    /**
     * 构建“按房间”发送存档包（由房间内部换算 step）。
     *
     * @param room       房间上下文
     * @param bArr       存档二进制数据
     * @param isPullSave 是否为拉档流程
     * @return 同步包（包含存档）
     */
    public abstract Packet sendSave(NetworkRoom room, byte[] bArr, boolean isPullSave);

    /**
     * 构建“按房间”拉档请求包（由房间内部换算 step）。
     *
     * @param room 房间上下文
     * @return 同步包（拉档请求）
     */
    public abstract Packet sendPullSave(NetworkRoom room);
}
