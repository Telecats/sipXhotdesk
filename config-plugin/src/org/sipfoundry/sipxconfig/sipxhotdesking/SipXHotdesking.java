package org.sipfoundry.sipxconfig.sipxhotdesking;

import org.sipfoundry.sipxconfig.dialplan.DialingRuleProvider;
import org.sipfoundry.sipxconfig.feature.LocationFeature;

public interface SipXHotdesking extends DialingRuleProvider {
	
	public static final LocationFeature FEATURE = new LocationFeature("hotdesking");

	SipXHotdeskingSettings getSettings();
	void saveSettings(SipXHotdeskingSettings settings);
}
