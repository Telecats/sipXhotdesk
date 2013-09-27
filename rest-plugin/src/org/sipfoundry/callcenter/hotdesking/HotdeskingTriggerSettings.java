package org.sipfoundry.callcenter.hotdesking;

import java.io.File;

public class HotdeskingTriggerSettings {
	
	public static final String BEAN_ID = "hotdeskingTriggerSettings";
	public static final String HOTDESKING_PATH_SUFFIX = "hotdesking";

	private String m_adminAgent;
	private String m_sipxchangeDomainName;
	private String m_logoffUser;
	private String m_tempPath;
	

	public String getAdminAgent() {
		return m_adminAgent;
	}

	public void setAdminAgent(String adminAgent) {
		this.m_adminAgent = adminAgent;
	}

	public String getSipxchangeDomainName() {
		return m_sipxchangeDomainName;
	}

	public void setSipxchangeDomainName(String sipxchangeDomainName) {
		this.m_sipxchangeDomainName = sipxchangeDomainName;
	}

	public String getLogoffUser() {
		return m_logoffUser;
	}

	public void setLogoffUser(String logoffUser) {
		this.m_logoffUser = logoffUser;
	}

	public String getTempPath() {
		return m_tempPath;
	}

	public void setTempPath(String tempPath) {
		this.m_tempPath = tempPath;
	}
	
	public String getTaskPath() {
		return getTempPath() + File.separator + HOTDESKING_PATH_SUFFIX;
	}
	
	
	
}
