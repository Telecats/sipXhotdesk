package org.sipfoundry.sipxconfig.sipxhotdesking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.address.Address;
import org.sipfoundry.sipxconfig.admin.AdminContext;
import org.sipfoundry.sipxconfig.cfgmgt.ConfigManager;
import org.sipfoundry.sipxconfig.cfgmgt.ConfigProvider;
import org.sipfoundry.sipxconfig.cfgmgt.ConfigRequest;
import org.sipfoundry.sipxconfig.cfgmgt.ConfigUtils;
import org.sipfoundry.sipxconfig.cfgmgt.KeyValueConfiguration;
import org.sipfoundry.sipxconfig.cfgmgt.PostConfigListener;
import org.sipfoundry.sipxconfig.commserver.Location;
import org.sipfoundry.sipxconfig.dialplan.DialPlanContext;
import org.sipfoundry.sipxconfig.domain.Domain;
import org.sipfoundry.sipxconfig.feature.Feature;
import org.sipfoundry.sipxconfig.feature.FeatureManager;
import org.sipfoundry.sipxconfig.permission.Permission;
import org.sipfoundry.sipxconfig.permission.PermissionManager;
import org.sipfoundry.sipxconfig.registrar.Registrar;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.setting.SettingImpl;
import org.springframework.beans.factory.annotation.Required;

public class SipXHotdeskingConfig implements ConfigProvider, PostConfigListener { //AlarmProvider

	private SipXHotdesking m_sipXHotdesking;
	private PermissionManager m_permissionManager;
	
	public void setPermissionManager(PermissionManager permissionManager) {
		this.m_permissionManager = permissionManager;
	}

	@Override
	public void replicate(ConfigManager manager, ConfigRequest request)
			throws IOException {
		if (!request.applies(SipXHotdesking.FEATURE, AdminContext.FEATURE)) {
			return;
		}

		SipXHotdeskingSettings settings = m_sipXHotdesking.getSettings();

		FeatureManager featureManager = manager.getFeatureManager();
		Domain domain = manager.getDomainManager().getDomain();

		List<Location> registrarLocations = manager.getFeatureManager()
				.getLocationsForEnabledFeature(Registrar.FEATURE);

		for (Location location : registrarLocations) {
			File dir = manager.getLocationDataDirectory(location);

			ConfigUtils.enableCfengineClass(dir, "hotdesking.cfdat", true,
					"hotdesking");

			File f = new File(dir, "hotdesking.properties.part");
			Writer wtr = new FileWriter(f);
			try {
				write(wtr, settings, domain);
			} finally {
				IOUtils.closeQuietly(wtr);
			}
		}

	}

	void write(Writer wtr, SipXHotdeskingSettings settings, Domain domain) throws IOException {
		KeyValueConfiguration config = KeyValueConfiguration
				.equalsSeparated(wtr);
		config.writeSettings(settings.getSettings());
		//Do more stuff with the settings.
		
		//Permission name mapping:
		Permission hotdesking, hotdeskingWithoutPincode, hotdeskingAutoLogoff;
		hotdesking = m_permissionManager.getPermissionByLabel("hotdesking");
		hotdeskingWithoutPincode = m_permissionManager.getPermissionByLabel("hotdesking_without_pincode");
		hotdeskingAutoLogoff = m_permissionManager.getPermissionByLabel("hotdesking_auto_logoff");
		
		Setting hotdeskingSetting, hotdeskingWithoutPincodeSetting, hotdeskingAutoLogoffSetting;
		hotdeskingSetting = new SettingImpl();
		hotdeskingWithoutPincodeSetting = new SettingImpl();
		hotdeskingAutoLogoffSetting = new SettingImpl();
		
		hotdeskingSetting.setName("hotdesking");
		hotdeskingSetting.setValue(hotdesking.getName());
		
		hotdeskingWithoutPincodeSetting.setName("hotdesking_without_pincode");
		hotdeskingWithoutPincodeSetting.setValue(hotdeskingWithoutPincode.getName());
		
		hotdeskingAutoLogoffSetting.setName("hotdesking_auto_logoff");
		hotdeskingAutoLogoffSetting.setValue(hotdeskingAutoLogoff.getName());
		
		config.writeSetting("permission.", hotdeskingSetting);
		config.writeSetting("permission.", hotdeskingWithoutPincodeSetting);
		config.writeSetting("permission.", hotdeskingAutoLogoffSetting);
	}

	/*@Override
	public Collection<AlarmDefinition> getAvailableAlarms(
			AlarmServerManager manager) {
		if (!manager.getFeatureManager().isFeatureEnabled(SipXtcExample.FEATURE)) {
			return null;
		}
		String[] ids = new String[] { "SIPXEXAMPLE_FAILED_LOGIN" };

		return AlarmDefinition.asArray(ids);
	}*/

	@Required
	public void setSipXHotdesking(SipXHotdesking sipXHotdesking) {
		m_sipXHotdesking = sipXHotdesking;
	}
	
	
	

	@Override
	public void postReplicate(ConfigManager manager, ConfigRequest request)
			throws IOException {
		if(request.applies(Collections.singletonList((Feature)SipXHotdesking.FEATURE)))
			manager.configureEverywhere(DialPlanContext.FEATURE);
	}

}
