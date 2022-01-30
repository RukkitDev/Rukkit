/*
 *  All Rights Reserved.
 *  FileName: InternalRukkitPlugin.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.plugin.internal;
import cn.rukkit.plugin.*;

abstract class InternalRukkitPlugin extends RukkitPlugin
{
	public InternalRukkitPlugin() {
		loadConfig();
	}
	
	public abstract void loadConfig();
}
