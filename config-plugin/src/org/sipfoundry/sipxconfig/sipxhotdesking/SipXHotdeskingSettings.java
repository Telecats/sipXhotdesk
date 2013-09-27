package org.sipfoundry.sipxconfig.sipxhotdesking;

import java.util.Collection;
import java.util.Collections;

import org.sipfoundry.sipxconfig.cfgmgt.DeployConfigOnEdit;
import org.sipfoundry.sipxconfig.feature.Feature;
import org.sipfoundry.sipxconfig.setting.PersistableSettings;
import org.sipfoundry.sipxconfig.setting.Setting;

public class SipXHotdeskingSettings extends PersistableSettings implements DeployConfigOnEdit {

	public static final String BEAN_ID = "hotdeskingSettings";
	public static final String SETTING_HOTDESKING_EXTENSION = "hotdesking/hotdesking.extension";
	public static final String SETTING_FREESWITCH_PORT = "hotdesking/freeswitch.eventSocketPort";
	public static final String HOTDESKING_USER_NAME = "~~in~HD";
	
	@Override
	public Collection<Feature> getAffectedFeaturesOnChange() {
		return Collections.singleton((Feature) SipXHotdesking.FEATURE);//Also Registrar.FEATURE??
	}

	@Override
	public String getBeanId() {
		return BEAN_ID;
	}

	@Override
    public Setting loadSettings() {
        return getModelFilesContext().loadModelFile("sipxhotdesking/sipxhotdesking.xml");
    }
	
	

}
