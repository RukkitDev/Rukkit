/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.game;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CheckSumList {
    private ArrayList<ChecksumItem> checksumItems = new ArrayList<>();
    public class ChecksumItem {
        String description;
        public long checkData = 0;
        public long prefix = 0;
        public ChecksumItem(String description) {
            this.description = description;
        }

        public ChecksumItem(String description, long prefix) {
            this(description);
            this.prefix = prefix;
        }

        public String getDescription() {
            return description;
        }

        public long getCheckData() {
            return checkData;
        }

        public void setCheckData(long data) {
            this.checkData = data;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ChecksumItem item) {
                if (Math.abs(item.getCheckData() - getCheckData()) <= prefix) return true;
                else return false;
            } else {
                return false;
            }
        }
    }

    public CheckSumList() {
        checksumItems.add(new ChecksumItem("Unit Pos", 100000));
        checksumItems.add(new ChecksumItem("Unit Dir", 100000));
        checksumItems.add(new ChecksumItem("Unit Hp", 10000));
        checksumItems.add(new ChecksumItem("Unit Id", 5));
        checksumItems.add(new ChecksumItem("Waypoints", 5));
        checksumItems.add(new ChecksumItem("Waypoints Pos", 5));
        checksumItems.add(new ChecksumItem("Team Credits", 500));
        checksumItems.add(new ChecksumItem("UnitPaths", 5));
        checksumItems.add(new ChecksumItem("Unit Count", 10));
        checksumItems.add(new ChecksumItem("Team Info", 10));
        checksumItems.add(new ChecksumItem("Team 1 Credits", 500));
        checksumItems.add(new ChecksumItem("Team 2 Credits", 500));
        checksumItems.add(new ChecksumItem("Team 3 Credits", 500));
        checksumItems.add(new ChecksumItem("Command center2", 1000));
        checksumItems.add(new ChecksumItem("Command center3", 1000));
    }

    public ArrayList<ChecksumItem> getCheckList() {
        return checksumItems;
    }

    public void addCheckItem(ChecksumItem checksumItem) {
        checksumItems.add(checksumItem);
    }

    public ChecksumItem get(int index) {
        return checksumItems.get(index);
    }

    public boolean checkData(CheckSumList list) {
        int desyncCount = 0;
        for (int i = 0;i < checksumItems.size();i++) {
            if(!(checksumItems.get(i).equals(list.get(i)))) {
                LoggerFactory.getLogger("Checksum").warn("{} {} != {}", checksumItems.get(i).getDescription(),
                        checksumItems.get(i).getCheckData(), list.get(i).getCheckData());
                desyncCount++;
            }
        }
        return desyncCount == 0;
    }
}
