/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.game.map.OfficialMap;

/** Shared, side-effect-free parsing rules for map chat commands. */
final class MapCommandSupport {
    static final int PAGE_SIZE = 10;

    private MapCommandSupport() {
    }

    /** Returns a zero-based page index, or {@code -1} for an invalid page. */
    static int pageIndex(String[] args) {
        if (args == null || args.length == 0) {
            return 0;
        }
        try {
            int page = Integer.parseInt(args[0]);
            return page > 0 ? page - 1 : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static int pageEnd(int pageIndex, int size) {
        return Math.min(pageIndex * PAGE_SIZE + PAGE_SIZE, size);
    }

    /** Returns a valid zero-based index, or {@code -1} when the value is invalid. */
    static int mapIndex(String value, int size) {
        try {
            int index = Integer.parseInt(value);
            return index >= 0 && index < size ? index : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Extracts the text between the first and last single quote. */
    static String quotedValue(String value) {
        if (value == null || !value.startsWith("'")) {
            return null;
        }
        int end = value.lastIndexOf('\'');
        return end > 0 ? value.substring(1, end) : value.substring(1);
    }

    /** Matches both the user-facing map name and the resource name shown by {@code maps}. */
    static int officialMapIndex(String query) {
        if (query == null || query.isEmpty()) {
            return -1;
        }
        int size = Math.min(OfficialMap.maps.length, OfficialMap.mapsName.length);
        for (int i = 0; i < size; i++) {
            if (OfficialMap.mapsName[i].contains(query)
                    || OfficialMap.maps[i].contains(query)) {
                return i;
            }
        }
        return -1;
    }
}
