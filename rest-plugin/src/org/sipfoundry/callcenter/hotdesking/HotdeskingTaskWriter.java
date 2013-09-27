package org.sipfoundry.callcenter.hotdesking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class HotdeskingTaskWriter {

    private static Logger logger = Logger.getLogger(HotdeskingTaskWriter.class);
    private HotdeskingTriggerSettings m_hotdeskingTriggerSettings;
    
    public HotdeskingTriggerSettings getHotdeskingTriggerSettings() {
		return m_hotdeskingTriggerSettings;
	}

	public void setHotdeskingTriggerSettings(
			HotdeskingTriggerSettings hotdeskingTriggerSettings) {
		this.m_hotdeskingTriggerSettings = hotdeskingTriggerSettings;
	}

	

    public Properties getPropertiesForLogin(String userId, String phoneId, String ipAddress) {    	

        String domain = m_hotdeskingTriggerSettings.getSipxchangeDomainName();
        String logoffUser = m_hotdeskingTriggerSettings.getLogoffUser();

        Properties properties = new Properties();
        properties.setProperty("newUser", userId);
        properties.setProperty("callingUser", logoffUser + "@" + domain);
        properties.setProperty("hotdeskingWithEverywhereLogout", "true");
        properties.setProperty("phoneSerial", phoneId);
        properties.setProperty("sipContactHost", ipAddress);
        properties.setProperty("hotdesking.logoffUser", logoffUser);
        properties.setProperty("hotdesking.enable_auto_logoff_other_phones", "true");

        return properties;
    }

    public Properties getPropertiesForLogout(String userId, String phoneId, String ipAddress) {
    	
    	String domain = m_hotdeskingTriggerSettings.getSipxchangeDomainName();
        String logoffUser = m_hotdeskingTriggerSettings.getLogoffUser();

        Properties properties = new Properties();
        properties.setProperty("callingUser", userId + "@" + domain);
        properties.setProperty("logoutOnThisPhone", "true");
        properties.setProperty("phoneSerial", phoneId);
        properties.setProperty("sipContactHost", ipAddress);
        properties.setProperty("hotdesking.logoffUser", logoffUser);
        properties.setProperty("hotdesking.enable_auto_logoff_other_phones", "true");

        return properties;
    }

    public boolean write(Properties properties) {
        final File taskDir = new File(m_hotdeskingTriggerSettings.getTaskPath());
        if (!taskDir.exists()) {
            taskDir.mkdir();
        } else {
            if (!taskDir.canWrite()) {
                logger.fatal("Temp dir not writable");
                return false;
            }
        }

        logger.info("Creating properties in: " + taskDir.getAbsolutePath());
        File taskFile = new File(taskDir, System.currentTimeMillis() + "");
        while (taskFile.exists()) {
            taskFile = new File(taskDir, System.currentTimeMillis() + "");
        }
        BufferedWriter out = null;
        try {
            final FileWriter fstream = new FileWriter(taskFile);
            out = new BufferedWriter(fstream);
            logger.info("Writing properties to tmp file: " + taskFile.getAbsolutePath());
            properties.store(out, "Hotdesking task file");
            out.flush();
            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            logger.error(
                    "HotdeskingCode::createHotdeskingTask Cannot write to temp file: " + taskFile.getAbsolutePath()
                            + ": " + e.getMessage(), e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        // rename file
        final File dest = new File(taskDir, taskFile.getName() + ".hotdesking");
        boolean renameTo = taskFile.renameTo(dest);
        logger.info("Renaming properties to file: " + dest.getAbsolutePath());
        if (!renameTo) {
            logger.error("HotdeskingCode::createHotdeskingTask Renaming task file fail!");
            return false;
        }
        logger.info("HotdeskingCode::createHotdeskingTask Successfully created task file: " + dest.getAbsolutePath());
        return true;
    }

}
