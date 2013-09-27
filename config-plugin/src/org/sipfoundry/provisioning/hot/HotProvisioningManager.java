/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
//package org.sipfoundry.sipxconfig.device;
package org.sipfoundry.provisioning.hot;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

public interface HotProvisioningManager {

	/**
	 * Hotdesking property, the sipContactHost (ip)
	 */
	public static final String SIP_CONTACT_HOST_PROP = "sipContactHost";
	
    /**
     * Restart devices.
     *
     * @param deviceIds collection of phone ids to be restarted
     * @param scheduleTime time at which device should be restarted, null for ASAP
     */
    public void hotProvision(Collection<Integer> deviceIds, Date scheduleTime, HashMap<String,String> props);

    public void hotProvision(Integer deviceId, Date scheduleTime, HashMap<String,String> props);
}
