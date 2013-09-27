package org.sipfoundry.hotdesking;

import java.util.List;

import org.apache.log4j.Logger;
import org.sipfoundry.hotdesking.mongo.HotdeskingPermissionManager;
import org.sipfoundry.hotdesking.mongo.HotdeskingPhone;
import org.sipfoundry.hotdesking.mongo.HotdeskingPhoneManager;
import org.springframework.beans.factory.annotation.Required;

public class AutoLogoff {
	
	static final Logger LOG = Logger.getLogger(AutoLogoff.class);
	
	private HotdeskingPermissionManager m_hotdeskingPermissionManager;
	private HotdeskingPhoneManager m_hotdeskingPhoneManager;
	private String m_logoffUser;
	
	@Required
	public void setLogoffUser(String logoffUser) {
		this.m_logoffUser = logoffUser;
	}
	
	@Required
	public void setHotdeskingPermissionManager(HotdeskingPermissionManager hotdeskingPermissionManager) {
		this.m_hotdeskingPermissionManager = hotdeskingPermissionManager;
	}
	
	@Required
	public void setHotdeskingPhoneManager(HotdeskingPhoneManager hotdeskingPhoneManager) {
		this.m_hotdeskingPhoneManager = hotdeskingPhoneManager;
	}

	public void logoff() {
		List<HotdeskingUser> hotdeskingUsers = m_hotdeskingPermissionManager.getAutoLogoffHotdeskingUsers();
		for(HotdeskingUser hotdeskingUser: hotdeskingUsers) {
			LOG.debug("Selected for logoff: " + hotdeskingUser.getUserId());

			HotdeskingPhone phone = m_hotdeskingPhoneManager.getOnePhone(hotdeskingUser.getUserId(), hotdeskingUser.getRealm());
			if( phone != null ) {
				HotdeskingTask logoffTask = new HotdeskingTask();
				
				logoffTask.setPhoneSerial(phone.getMac());
				logoffTask.setSipContactHost(phone.getIp());
				logoffTask.setCurrentUserId(hotdeskingUser.getUserId());
				logoffTask.setHotdeskingWithEverywhereLogout(true);
				logoffTask.setHotdeskingWithoutLogout(false);
				logoffTask.setLogoutEverywhere(true);
				logoffTask.setLogoutOnThisPhone(true);
				logoffTask.setNewUserId(m_logoffUser);
				
				TaskFileWriter.createHotdeskingTask(logoffTask, m_logoffUser, true);
			}
		}
		
		
		
	}
}
