/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit;

import cn.rukkit.game.unit.InternalUnit;
import cn.rukkit.network.GameInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class RukkitSaveReader {
    public static Logger log = LoggerFactory.getLogger("RukkitSaveReader");
    public static void main(String args[]) throws IOException {
        String saveName = "save.rwsave";
        if (args.length <= 1) {
            log.warn("No inputs given, reading save.rwsave in application folder...");
        } else {
            saveName = args[0];
        }
        // 文件处理
        FileInputStream fileInputStream = new FileInputStream(saveName);
        byte[] bytes = fileInputStream.readAllBytes();
        fileInputStream.close();

        GameInputStream inputStream = new GameInputStream(bytes);
        log.info("Header: {}", inputStream.readString());
        inputStream.readInt();
        int version = inputStream.readInt();
        log.info("Save (Protocol) Version: {}", version);
        if (version > 96) {
            log.error("Save version is from a newer version of Rusted Warfare!");
            throw new RuntimeException("Save version is from a newer version of Rusted Warfare!");
        }

        if (version >= 5) inputStream.readBoolean();

        if (version >= 23) inputStream.startBlock(true); // SaveCompression

        if (version >= 54) {
            inputStream.startBlock(false); // customUnitsBlock
            log.info("Custom Units Block");
            inputStream.endBlock();
        }

        if (version >= 56) {
            inputStream.startBlock(false); // gameSetup
            log.info("Game Setup Block");
            inputStream.endBlock();
        }

        log.info("MapPath: {}", inputStream.readString());
        if (version >= 72) {
            if (inputStream.readBoolean()) {
                log.info("Custom Map Found!");
                inputStream.readStreamBytes();
            }
        }

        log.info("GameTime: {}, Player lookat: ({}, {}, {}x zoom)", inputStream.readInt(), inputStream.readFloat(), inputStream.readFloat(), inputStream.readFloat());

        if (version >= 18) {
            log.info("GroupCount: {}", inputStream.readInt());
        }
        inputStream.readInt();
        if (version >= 19) {
            inputStream.readMark();
            log.info("Setup End");
        }

        // 沟槽的读取
        if (inputStream.readBoolean()) {
            int x = inputStream.readInt();
            int y = inputStream.readInt();
            for (int i = 0;i < x;i++) {
                for (int j = 0;j < y;j++) {
                    log.info("({}, {}) = {}", i, j, inputStream.readByte());
                }
            }
        }

        if (version >= 86) {
            log.info("needFogSetup: {}", inputStream.readBoolean());
            log.info("hasFog: {}", inputStream.readBoolean());
            log.info("losFog: {}", inputStream.readBoolean());
            log.info("revealedMap: {}", inputStream.readBoolean());
        }

        if (inputStream.readBoolean()) {
            log.info(" - Mission Engine -");
            inputStream.readBoolean();
            inputStream.readInt();
            inputStream.readInt();
            inputStream.readInt();
            inputStream.readInt();
            inputStream.readInt();
            inputStream.readFloat();
            inputStream.readFloat();
            inputStream.readFloat();
            inputStream.readBoolean();
            int type = inputStream.readInt();
            if (type >= 1) {
                int count = inputStream.readInt();
                for (int i = 0;i < count;i++) {
                    log.info(inputStream.readString());
                    inputStream.readBoolean();
                    if (type >= 2) {
                        inputStream.readInt();
                        inputStream.readInt();
                    }

                    if (type >= 3) inputStream.readBoolean();

                    if (type >= 4) inputStream.readInt();
                }
            }

            if (type >= 5) inputStream.readInt();
            if (type >= 6) inputStream.readBoolean();
        }

        if (version >= 19) {
            inputStream.readMark();
            log.info("Teams");
            if (version >= 36) log.info("Local Player Index: {}", inputStream.readInt());
        }

        int maxTeamIndex = 8;
        if (version >= 49) log.info("Max Team Index: {}", maxTeamIndex = inputStream.readInt());

        for (int i = 0;i < maxTeamIndex;i++) {
            log.info("# Player {} #", i);
            log.info("isAI: {}", inputStream.readBoolean());
            log.info("isNetworkedPlayer: {}", inputStream.readBoolean());
            boolean exist = inputStream.readBoolean();
            log.info("exist: {}", exist);
            if (exist) {
                if (version >= 2) {
                    // 读玩家
                    //玩家位置
                    inputStream.readByte();
                    //玩家资金(毫无作用)
                    inputStream.readInt();
                    //玩家队
                    inputStream.readInt();
                    // 名字
                    if (inputStream.readBoolean()) log.info("Name: {}", inputStream.readString());
                    inputStream.readBoolean();

                    if (version >= 14) {
                        //enc.stream.writeBoolean(true);
                        inputStream.readInt();
                        // time
                        inputStream.readLong();
                    }

                    if (version >= 34) {
                        //是否AI
                        inputStream.readBoolean();
                        //AI难度
                        inputStream.readInt();
                    }

                    if (version >= 50) {
                        //玩家队伍
                        inputStream.readInt();
                        inputStream.readByte();
                    }

                    if (version >= 52) {
                        //分享控制
                        inputStream.readBoolean();
                        //是否掉线
                        inputStream.readBoolean();
                    }

                    if (version >= 70) {
                        //是否投降
                        inputStream.readBoolean();
                        inputStream.readBoolean();
                        inputStream.readInt();
                    }

                    if (version >= 90) {
                        if (inputStream.readBoolean()) log.info("Forward Player: {}", inputStream.readString());
                        //是否房主
                        inputStream.readInt();
                    }

                    if (version >= 93) {
                        // 1.15新增
                        if (inputStream.readBoolean()) log.info("AI Difficulty Override: {}", inputStream.readInt());
                        if (inputStream.readBoolean()) log.info("startUnitOverride: {}", inputStream.readInt());
                        if (inputStream.readBoolean()) log.info("Unknown: {}", inputStream.readInt());
                        if (inputStream.readBoolean()) log.info("Player Color: {}", inputStream.readInt());
                        //color
                        log.info("Color: {}", inputStream.readInt());
                    }
                } else {
                    throw new RuntimeException("Old save not supported!");
                }
            }
            log.info("");
        }

        int unitCount = inputStream.readInt();
        log.info("Total Units: {}", unitCount);
        for (int i = 0;i< unitCount;i++) {
            String unitName;
            long unitId;
            byte type = inputStream.readByte();
            if (type == 1) {
                // Internal Unit
                unitName = InternalUnit.units[inputStream.readInt()];
                unitId = inputStream.readLong();
                log.info("[{}] InternalUnit@{}", unitId, unitName);
            } else if (type == 3) {
                // Custom Unit
                unitName = inputStream.readString();
                unitId = inputStream.readLong();
                log.info("[{}] CustomUnit@{}", unitId, unitName);
            } else if (type == 2) {
                // Unknown
                unitName = String.valueOf(inputStream.readByte());
                unitId = inputStream.readLong();
                log.info("[{}] UnknownUnit@{}", unitId, unitName);
            } else {
                log.warn("Unsupported Unit Type!");
            }
        }

        if (version >= 3) log.info("Next Unit Id: {}", inputStream.readLong());

        if (version >= 24) {
            inputStream.readInt();
            log.info("InputController - GroupCount: {}", inputStream.readInt());
        }

        // Interface Engine
        if (version >= 4) {
            // Part 1
            int count = inputStream.readInt();
            log.info("Count: {}", count);
            for (int i = 0;i < count;i++) {
                float f = inputStream.readFloat();
                long l = inputStream.readLong();
                int count2 = inputStream.readInt();
                log.info("{}, {}, count: {}", f, l, count2);
                for (int j = 0;j < count2;j++) {
                    log.info("{}", inputStream.readLong());
                }
                inputStream.readByte();
            }
            inputStream.readByte();
            // Part 2
            if (inputStream.readByte() >= 1) {
                log.info("ad: {}", inputStream.readInt());
            }
        }

        // Stats Engine
        if (version >= 57) {
            inputStream.startBlock(false);
            inputStream.readByte();
            int count = inputStream.readInt();
            for (int i = 0;i < count;i++) {
                log.info("# Stats #");
                log.info("");
            }
            inputStream.endBlock();
        }

        if (version >= 7) {

        }

    }
}
