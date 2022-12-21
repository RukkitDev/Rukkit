/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Service
{
    /**
     * Service internal logger.
     */
    public Logger log = LoggerFactory.getLogger(getClass());

    /**
    * On service start.
     */
    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onRegister();
}