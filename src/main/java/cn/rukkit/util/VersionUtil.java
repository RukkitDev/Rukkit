/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 版本管理工具类
 * 负责从资源文件中动态获取版本信息
 */
public class VersionUtil {
    private static final Logger log = LoggerFactory.getLogger(VersionUtil.class);
    private static final String VERSION_FILE = "rukkit-version.properties";
    
    private static Properties versionProperties = null;
    private static boolean propertiesLoaded = false;
    
    /**
     * 获取Rukkit版本号
     * @return 版本字符串，如果无法获取则返回默认值
     */
    public static String getVersion() {
        loadVersionProperties();
        if (versionProperties != null) {
            String version = versionProperties.getProperty("rukkit.version");
            if (version != null && !version.isEmpty()) {
                return version;
            }
        }
        log.warn("无法从资源文件获取版本信息，使用默认版本");
        return "UNKNOWN"; // 默认版本
    }
    
    /**
     * 获取构建时间戳
     * @return 构建时间戳，如果无法获取则返回空字符串
     */
    public static String getBuildTimestamp() {
        loadVersionProperties();
        if (versionProperties != null) {
            return versionProperties.getProperty("build.timestamp", "");
        }
        return "";
    }
    
    /**
     * 获取Git提交哈希
     * @return Git提交哈希，如果无法获取则返回空字符串
     */
    public static String getGitCommit() {
        loadVersionProperties();
        if (versionProperties != null) {
            return versionProperties.getProperty("git.commit", "");
        }
        return "";
    }
    
    /**
     * 获取完整的版本信息字符串
     * @return 完整版本信息
     */
    public static String getFullVersionInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rukkit ").append(getVersion());
        String timestamp = getBuildTimestamp();
        if (!timestamp.isEmpty()) {
            sb.append(" (built: ").append(timestamp).append(")");
        }
        String commit = getGitCommit();
        if (!commit.isEmpty()) {
            sb.append(" [").append(commit).append("]");
        }
        return sb.toString();
    }
    
    /**
     * 加载版本属性文件
     */
    private static synchronized void loadVersionProperties() {
        if (propertiesLoaded) {
            return;
        }
        
        try {
            InputStream inputStream = VersionUtil.class.getClassLoader()
                    .getResourceAsStream(VERSION_FILE);
            
            if (inputStream != null) {
                versionProperties = new Properties();
                versionProperties.load(inputStream);
                inputStream.close();
                log.debug("成功加载版本信息文件: {}", VERSION_FILE);
            } else {
                log.warn("未找到版本信息文件: {}", VERSION_FILE);
            }
        } catch (IOException e) {
            log.error("加载版本信息文件失败: {}", VERSION_FILE, e);
        } finally {
            propertiesLoaded = true;
        }
    }
    
    /**
     * 强制重新加载版本信息（主要用于测试）
     */
    public static synchronized void reloadVersionProperties() {
        propertiesLoaded = false;
        versionProperties = null;
        loadVersionProperties();
    }
}