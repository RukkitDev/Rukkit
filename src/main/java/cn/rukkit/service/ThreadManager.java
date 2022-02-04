/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.service;
import java.util.concurrent.*;

public class ThreadManager
{
	ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(8);
	
	/**
	* init a thread manager.
	* @param poolCount threadPool thread count.
	* @return ScheduledFuture
	*/
	public ThreadManager(int poolCount){
		executorService = new ScheduledThreadPoolExecutor(poolCount);
	}
	
	/**
	* Schedule a task.
	* @param runnable Runnable task.
	* @param initialDelay after ~ms task will be scheduled.
	* @return ScheduledFuture
	*/
	public ScheduledFuture schedule(Runnable runnable, int initialDelay) {
		return executorService.schedule(runnable, initialDelay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedule a task.
	 * @param runnable Runnable task.
	 * @param initialDelay after ~ms task will be scheduled.
	 * @param delay every ~ms task will be scheduled.
	 * @return ScheduledFuture
	 */
	public ScheduledFuture schedule(Runnable runnable, int initialDelay, int delay) {
		ScheduledFuture t = executorService.scheduleWithFixedDelay(runnable, initialDelay, delay,TimeUnit.MILLISECONDS);
		return t;
	}
	
	/**
	 * Schedule a task without schedule.
	 * @param runnable Runnable task.
	 * @return Future
	 */
	public Future submit(Runnable runnable) {
		return executorService.submit(runnable);
	}
	
	public void shutdown() {
		executorService.shutdown();
	}
	
	public void shutdownTask(ScheduledFuture future) {
		future.cancel(true);
	}
	
	public long getTaskCount () {
		return executorService.getTaskCount();
	}
	
	public int getActiveThreadCount() {
		return executorService.getActiveCount();
	}
}
