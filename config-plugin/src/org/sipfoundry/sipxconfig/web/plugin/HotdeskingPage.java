package org.sipfoundry.sipxconfig.web.plugin;

import org.apache.tapestry.annotations.Bean;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.sipfoundry.sipxconfig.components.PageWithCallback;
import org.sipfoundry.sipxconfig.components.SipxValidationDelegate;
import org.sipfoundry.sipxconfig.dialplan.AutoAttendant;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.sipxhotdesking.SipXHotdesking;
import org.sipfoundry.sipxconfig.sipxhotdesking.SipXHotdeskingSettings;

public abstract class HotdeskingPage extends PageWithCallback implements PageBeginRenderListener {
	
	@InjectObject("spring:sipXHotdesking")
    public abstract SipXHotdesking getSipXHotdesking();
	
    public abstract SipXHotdeskingSettings getSettings();
    public abstract void setSettings(SipXHotdeskingSettings settings);
	
	@Bean()
    public abstract SipxValidationDelegate getValidator();
	
	@Override
	public void pageBeginRender(PageEvent arg0) {
		if (getSettings() == null) {
			setSettings(getSipXHotdesking().getSettings());
        }
	}

	
	public void apply() {
		if (getValidator().getHasErrors()) {
			return;
		}
		getSipXHotdesking().saveSettings(getSettings());
	}

}
