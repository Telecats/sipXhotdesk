//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.provisioning.taskfile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Timer for checking hotdesking tasks
 * 
 * @author aliaksandr
 * 
 */
public class HotdeskingCheckTaskTimer implements
		ApplicationContextAware {
	private static final Log LOG = LogFactory
			.getLog(HotdeskingCheckTaskTimer.class);

	private ApplicationContext applicationContext;
	private String m_tmpDir;
	
	@Required
	public void setTmpDir(String tmpDir) {
		this.m_tmpDir = tmpDir;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void scanForHotdeskingTaskFile() {
		try {
		    LOG.debug("-----------------------------------------");
		    LOG.debug("Checking for hotdesking task file");
			final File f = new File(m_tmpDir);
			if (!f.exists() || !f.canWrite() || !f.isDirectory()) {
				LOG.error("Hotdesking timer doesnot have write access to task directory: "
						+ m_tmpDir);
				return;
			}

			final File[] listFiles = f.listFiles();
			if (listFiles.length>0) {
			    LOG.info("Found hotdesking task file(s):"+listFiles.length);
			} else {
			    LOG.debug("No hotdesking task file(s) to process");
			}
			int length = listFiles.length;
			for (int i = 0; i < length; i++) {
				final Properties properties = new Properties();
				final File file = listFiles[i];
				try {
					try {
						properties.load(new FileInputStream(file));
					} catch (Exception e) {
						LOG.error(
								"Hotdesking timer cannot load task property file: "
										+ file.getAbsolutePath() + " "
										+ e.getMessage(), e);
						continue;
					}
	                                LOG.debug("hotdesking properties loaded"+file.getAbsolutePath()+":\n"+properties);
	
					final HotdeskingApplicationEvent event = new HotdeskingApplicationEvent(
							properties);
					applicationContext.publishEvent(event);
				} finally {
					// 	cleanup
					boolean delete = file.delete();
					if (!delete) {
						LOG.error("Hotdesking timer cannot delete task file: "
								+ file.getAbsolutePath());
					}
				}
			}
		} catch (Throwable t) {
			LOG.error("Error executing HotdeskingTask:" + t.getMessage(), t);
		}
		LOG.debug("-----------------------------------------");
	}

}
