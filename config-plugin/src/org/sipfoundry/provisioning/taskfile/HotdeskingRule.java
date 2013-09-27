//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.provisioning.taskfile;

import java.util.Collections;
import java.util.List;

import org.sipfoundry.sipxconfig.dialplan.CallTag;
import org.sipfoundry.sipxconfig.dialplan.DialPattern;
import org.sipfoundry.sipxconfig.dialplan.InternalForwardRule;
import org.sipfoundry.sipxconfig.dialplan.config.FullTransform;

/**
 * Mapping rule for hotdesking
 * 
 * @author aliaksandr
 * 
 */
public class HotdeskingRule extends InternalForwardRule {

	private String m_hotdeskingPermissionName;
	
    public HotdeskingRule(String hostNameAndPort, String prefix, String language, String hotdeskingPermissionName) {
        super(new DialPattern(prefix, -1), new HotdeskingTransform(hostNameAndPort, language));
        setName("Hotdesking");
        setDescription("Hotdesking support - forwarding to hotdesking special user");
        setHotdeskingPermissionName(hotdeskingPermissionName);
    }
    
    public void setHotdeskingPermissionName(String hotdeskingPermissionName) {
    	this.m_hotdeskingPermissionName = hotdeskingPermissionName;
    }

    @Override
    public CallTag getCallTag() {
        //return CallTag.HTD;
    	return CallTag.CUST;
    }
    
    @Override
    public List<String> getPermissionNames() {
        return Collections.singletonList(m_hotdeskingPermissionName);
    }

    private static class HotdeskingTransform extends FullTransform {
        public HotdeskingTransform(String hostNameAndPort, String language) {
            setUser("HTD");
            setHost(hostNameAndPort);
            setUrlParams("command=hotdesking", "locale=" + language, "htdUser={vdigits}");
        }
    }

}
