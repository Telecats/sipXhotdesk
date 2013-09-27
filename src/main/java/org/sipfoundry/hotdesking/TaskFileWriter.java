package org.sipfoundry.hotdesking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class TaskFileWriter {
	
	static final Logger LOG = Logger.getLogger(TaskFileWriter.class);

	public static void createHotdeskingTask(HotdeskingTask newUserInformation, 
			String logoffUser, 
			boolean enableAutoLogoffOtherPhones
			) {
        LOG.info("HotdeskingCode::createHotdeskingTask Creating task for Hotdesking");
        String path = System.getProperty("tmp.dir");
        if (path == null) {
            LOG.fatal("HotdeskingCode::createHotdeskingTask Cannot get System Property tmp.dir!  Check jvm argument -Dtmp.dir=");
            return;
        }
        final File taskDir = new File(path + File.separator + "hotdesking");
        if (!taskDir.exists()) {
            // create dir for hotdesking tasks
            taskDir.mkdir();
        } else {
            if (!taskDir.canWrite()) {
                LOG.fatal("HotdeskingCode::createHotdeskingTask Cannot write to temp folder: "
                        + taskDir.getAbsolutePath());
                return;
            }
        }

        // create uniq file
        File taskFile = new File(taskDir, System.currentTimeMillis() + "");
        while (taskFile.exists()) {
            taskFile = new File(taskDir, System.currentTimeMillis() + "");
        }
        LOG.info("HotdeskingCode::createHotdeskingTask Created temp task file: " + taskFile.getAbsolutePath());
        // write task content
        final Properties asProperties = newUserInformation.getAsProperties();

        // add logoff user
        //String logoffUser = m_hotdeskingConfig.getLogoffUser();
        //boolean enableAutoLogoffOtherPhones = m_hotdeskingConfig.m_enableAutoLogoffOtherPhones;

        LOG.debug("HotdeskingCode::logoff user                             :" + logoffUser);
        LOG.debug("HotdeskingCode::auto logoff other phones                : " + enableAutoLogoffOtherPhones);
        asProperties.setProperty(Hotdesking.LOGOFF_USER, logoffUser); 
        asProperties.setProperty(Hotdesking.HOTDESKING_ENABLE_AUTO_LOGOFF_OTHER_PHONES, enableAutoLogoffOtherPhones + "");

        BufferedWriter out = null;
        try {
            final FileWriter fstream = new FileWriter(taskFile);
            out = new BufferedWriter(fstream);
            asProperties.store(out, "Hotdesking task file");
            out.flush();
            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            LOG.error(
                    "HotdeskingCode::createHotdeskingTask Cannot write to temp file: " + taskFile.getAbsolutePath()
                            + ": " + e.getMessage(), e);
            return;
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
        if (!renameTo) {
            LOG.error("HotdeskingCode::createHotdeskingTask Renaming task file fail!");
            return;
        }
        LOG.info("HotdeskingCode::createHotdeskingTask Successfully created task file: " + dest.getAbsolutePath());
    }
	
}
