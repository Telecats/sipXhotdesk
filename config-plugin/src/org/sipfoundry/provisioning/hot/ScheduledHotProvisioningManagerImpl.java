//////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////

//package org.sipfoundry.sipxconfig.device;
package org.sipfoundry.provisioning.hot;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.device.Device;
import org.sipfoundry.sipxconfig.device.DeviceSource;
import org.sipfoundry.sipxconfig.device.RestartException;
import org.sipfoundry.sipxconfig.job.JobContext;
import org.springframework.beans.factory.annotation.Required;

public class ScheduledHotProvisioningManagerImpl implements HotProvisioningManager {
    private static final Log LOG = LogFactory.getLog(ScheduledHotProvisioningManagerImpl.class);

    private static final int DEFAULT_THROTTLE_INTERVAL = 1000;

    private JobContext m_jobContext;

    private DeviceSource m_deviceSource;

    private ScheduledExecutorService m_executorService;

    /**
     * The number of millis by which we are going to delay provisining a device if more than one
     * device is restarted. We are delaying provisioning of the device. The default value is 1 second,
     * but it can be changes by modifying sipxconfig.properties file. Set to 0 to remove the
     * throttle.
     *
     */
    private int m_throttleInterval = DEFAULT_THROTTLE_INTERVAL;

    /**
     * Initial wait interval.
     */
    private int m_sleepInterval;

    public void hotProvision(Collection<Integer> deviceIds, Date scheduleTime, HashMap<String,String> props) {
        long delay = calculateDelay(scheduleTime);
        for (Integer id : deviceIds) {
            Device device = m_deviceSource.loadDevice(id);
            if (device.getModel().isRestartSupported()) {
                HotProvisionTask task = new HotProvisionTask(id, props);
                m_executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
                delay += m_throttleInterval;
            }
        }
    }

    long calculateDelay(Date scheduleTime) {
        if (scheduleTime == null) {
            return m_sleepInterval;
        }
        return scheduleTime.getTime() - System.currentTimeMillis() + m_sleepInterval;
    }

    public void hotProvision(Integer deviceId, Date scheduleTime, HashMap<String,String> props) {
        Device device = m_deviceSource.loadDevice(deviceId);
        if (device.getModel().isRestartSupported()) {
            HotProvisionTask task = new HotProvisionTask(deviceId, props);
            long delay = calculateDelay(scheduleTime);
            LOG.debug("schedule hotprovisining task, delay:"+delay);
            m_executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
        } else {
            LOG.warn("skipping hotprovisioning, phone doesn't support restart");
        }
    }

    @Required
    public void setJobContext(JobContext jobContext) {
        m_jobContext = jobContext;
    }

    @Required
    public void setDeviceSource(DeviceSource deviceSource) {
        m_deviceSource = deviceSource;
    }

    @Required
    public void setExecutorService(ScheduledExecutorService executorService) {
        m_executorService = executorService;
    }

    public void setThrottleInterval(int throttleInterval) {
        m_throttleInterval = throttleInterval;
    }

    public void setSleepInterval(int sleepInterval) {
        m_sleepInterval = sleepInterval;
    }

    private class HotProvisionTask implements Runnable {
        private final int m_deviceId;
		private HashMap<String, String> m_hotProvProps;

        public HotProvisionTask(int deviceId, HashMap<String, String> hotProvProps) {
            m_deviceId = deviceId;
            this.m_hotProvProps = hotProvProps;
        }

        public void run() {
            Device device = m_deviceSource.loadDevice(m_deviceId);
            String jobName = "HotProvision: " + device.getNiceName();
            LOG.debug("Running HotProvisionTask, jobName:"+jobName+"...");
            Serializable jobId = m_jobContext.schedule(jobName);
            try {
                m_jobContext.start(jobId);
                boolean hotProvisionable = (device instanceof HotProvisionable); 
                if (hotProvisionable) {
                    ((HotProvisionable)device).performHotProvisioning(m_hotProvProps);
                    m_jobContext.success(jobId);
                    LOG.debug("performing hotprovisioning...done");
                } else {
                    //TODO_ RD check modelId is usefull in logging
                    LOG.error("device not hotprovisionable, Bailing out !!! device:"+device.getModelId());
                }
            } catch (RestartException e) {
                m_jobContext.failure(jobId, null, e);
            } catch (RuntimeException e) {
                m_jobContext.failure(jobId, null, e);
                // do not throw error, job queue will stop running.
                // error gets logged to job error table and sipxconfig.log
                LOG.error(e);
            }
            LOG.debug("Running HotProvisionTask, jobName:"+jobName+"...done");
        }
    }
}
