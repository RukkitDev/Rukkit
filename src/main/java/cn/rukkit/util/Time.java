package cn.rukkit.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Time {
    /** Efficiency improvement under high concurrency */
    private static final CurrentTimeMillisClock INSTANCE = new CurrentTimeMillisClock();

    private Time() {} // Private constructor to prevent instantiation

    /**
     * Get the system time in nanoseconds
     *
     * @return The current value of the system timer in nanoseconds.
     */
    public static long nanos() {
        return System.nanoTime();
    }

    /**
     * @return The difference, in milliseconds, between the current time and midnight on January 1, 1970.
     */
    public static long millis() {
        return System.currentTimeMillis();
    }

    public static long second() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * @return The difference, in milliseconds, between the current time and midnight on January 1, 1970.
     */
    public static long concurrentMillis() {
        return INSTANCE.now;
    }

    /**
     * @return The difference, in seconds, between the current time and midnight on January 1, 1970.
     */
    public static int concurrentSecond() {
        return (int) (INSTANCE.now / 1000);
    }

    /**
     * Gets the number of nanoseconds elapsed since the last
     *
     * @param prevTime - must be nanoseconds
     * @return - Elapsed time in nanoseconds since prevTime
     */
    public static long getTimeSinceNanos(long prevTime) {
        return nanos() - prevTime;
    }

    /**
     * Get the number of milliseconds elapsed since the last
     *
     * @param prevTime - must be milliseconds
     * @return - Elapsed time in milliseconds since prevTime
     */
    public static long getTimeSinceMillis(long prevTime) {
        return millis() - prevTime;
    }

    public static long getTimeFutureMillis(long addTime) {
        return millis() + addTime;
    }

    public static int getTimeSinceSecond(int prevTime) {
        return concurrentSecond() - prevTime;
    }

    public static int getTimeFutureSecond(int addTime) {
        return concurrentSecond() + addTime;
    }

    /**
     * Get JDK current time
     */
    public static long getUtcMillis() {
        // 获取JDK当前时间
        Calendar cal = Calendar.getInstance();
        // 取得时间偏移量
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        // 取得夏令时差
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        // 从本地时间里扣除这些差量，即可以取得UTC时间
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return cal.getTimeInMillis();
    }

    public static String getMilliFormat(int fot) {
        return format(concurrentMillis(), fot);
    }

    public static String getUtcMilliFormat(int fot) {
        return format(getUtcMillis(), fot);
    }

    public static String format(long gmt, int fot) {
        String[] ft = {
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "dd-MM-yyyy HH:mm:ss",
            "MM-dd-yyyy HH:mm:ss",
            "yyyy-MM-dd_HH-mm-ss",
            "HH:mm:ss",
        };
        return new SimpleDateFormat(ft[fot]).format(new Date(gmt));
    }

    private static class CurrentTimeMillisClock {
        private volatile long now;

        public CurrentTimeMillisClock() {
            now = System.currentTimeMillis();
            scheduleTick();
        }

        @SuppressWarnings("resource")
        private void scheduleTick() {
            new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "current-time-millis");
                thread.setDaemon(true);
                return thread;
            }).scheduleAtFixedRate(() -> now = System.currentTimeMillis(), 100, 100, TimeUnit.MILLISECONDS);
        }
    }
}