//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.provisioning.taskfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.common.SipxHibernateDaoSupport;
import org.sipfoundry.sipxconfig.commserver.Location;
import org.sipfoundry.sipxconfig.feature.FeatureManager;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchAction;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchCondition;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchExtension;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchFeature;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.sipxhotdesking.SipXHotdesking;
import org.sipfoundry.sipxconfig.sipxhotdesking.SipXHotdeskingSettings;
import org.springframework.beans.factory.annotation.Required;
/**
 * Extension provider for hotdesking
 * @author aliaksandr
 *
 */
/**
 * Extension provider for hotdesking
 * @author aliaksandr
 *
 */
public class HotdeskingFreeswitchExtensionProviderImpl extends SipxHibernateDaoSupport<HotdeskingFreeswitchExtensionProvider> implements
		HotdeskingFreeswitchExtensionProvider {

    
    private static final Log LOG = LogFactory.getLog(HotdeskingFreeswitchExtensionProviderImpl.class);
    
	private boolean m_enabled = true;
    
	private SipXHotdesking m_sipXHotdesking;
    private FeatureManager m_featureManager;
    
    @Required
	public void setSipXHotdesking(
			SipXHotdesking sipXHotdesking) {
		this.m_sipXHotdesking = sipXHotdesking;
	}

    @Required
	public void setFeatureManager(FeatureManager featureManager) {
		m_featureManager = featureManager;
	}

	public boolean isEnabled() {
		return m_enabled;
	}

	public void setEnabled(boolean enabled) {
		m_enabled = enabled;
	}

	@Override
	public List<? extends FreeswitchExtension> getFreeswitchExtensions() {
		if (!m_enabled) {
			return new ArrayList<FreeswitchExtension>();
		}

		final List<FreeswitchExtension> exts = new ArrayList<FreeswitchExtension>();
		final FreeswitchExtension ext = new FreeswitchExtension() {
		    @Override
		    protected Setting loadSettings() {
		        return null;
		    }
		};
		ext.setName("HTD");
		exts.add(ext);

		final Set<FreeswitchCondition> conditions = new HashSet<FreeswitchCondition>();
		final FreeswitchCondition hotdeskingCondition = new FreeswitchCondition();
		hotdeskingCondition.setField("destination_number");
		hotdeskingCondition.setExpression("^HTD$");
		conditions.add(hotdeskingCondition);
		ext.setConditions(conditions);

		final Set<FreeswitchAction> actions = new HashSet<FreeswitchAction>();
		final FreeswitchAction hotdeskingAction = new FreeswitchAction();
		actions.add(hotdeskingAction);
		hotdeskingCondition.setActions(actions);

		hotdeskingAction.setApplication("socket");
		List<Location> addresses = m_featureManager.getLocationsForEnabledFeature(FreeswitchFeature.FEATURE);
		if(addresses.isEmpty()) {
			LOG.error("Couldn't determine address of the server, needed to initialize the HotdeskingFreeswitchExtension.");
		    return null;
		}
		String address = addresses.get(0).getAddress();
		String port = m_sipXHotdesking.getSettings().getSettingValue(SipXHotdeskingSettings.SETTING_FREESWITCH_PORT);
		hotdeskingAction.setData(address + ":" + port + " async full");
        return exts;
	}


}
