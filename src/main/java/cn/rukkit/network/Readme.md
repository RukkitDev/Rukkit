ServerPacketHandler
├── NetworkRoom (分为两种类型) (共存)
│   ├── MenuRoom (用作菜单房间)
│   │   ├── Room(Non-Room) 一房间一个玩家 无需多余的组件
│   └── ServerRoom (实际游戏房间)
│        ├── Connection
│        └── ServerRoom
RelayPacketHandler
└── RelayRoom


关于sync
ServerRoom
    1 每300 Tick 向所有玩家发一次30包[ServerRoom提供可信list]
    2 玩家返回校验内容31包 服务器记录内容到NetworkPlayerlist[不可信list]并刷新lastSyncTick
    2-1 对比所有玩家lastSyncTick
        只有一个玩家 -> 对比nm呢 它2^999金币我都不管
        否则
        只有两个玩家{
            相同 tick
            直接对比chksumList
            ret
            不同 tick
            直接对比chksumList
            无 则随机选取样本同步tick
            ret
            有异常则提示玩家（对比了其实也没用）
        }
        玩家>=3
            全部相同 - > 进入3
            取tick方差 波动较小{
                ret
            }else{
                所有人都不相同 拉到吧 ret
                2 1 -> 选 2
                2 2 1 -> 从随机2中选取
                2 2 -> 参考只有两个玩家的操作
            }
    3 细致对比chksumlist
        全部相同 great 什么都不用干 ret
        全部不同-> 拉倒吧 这个要靠对比save了//扑街rukkitdev没写完
        2 2 随机选择一边同步
        2 1 从众同步
    4 great
        将选取好的记录为ServerRoomList
        获取save 对比save(fuck)
    5 pullsave perfect!


        
