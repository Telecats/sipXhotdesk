package org.sipfoundry.hotdesking;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.freeswitch.FreeSwitchConfigurationInterface;

public class HotdeskingConfiguration implements FreeSwitchConfigurationInterface {

	protected String m_logLevel;
	protected String m_logFile;
	protected int m_eventSocketPort;
	protected String m_dataDirectory;
	protected String m_promptsDirectory;
	protected String m_docDirectory;
	protected String m_sipxchangeDomainName;
	protected String m_realm;
	protected String m_logoffUser;
	protected boolean m_enableRelogging;
	protected boolean m_enablePromptLogoffOtherPhones;
	protected boolean m_enableAutoLogoffOtherPhones;
	protected boolean m_enableConfirmation;
	protected boolean m_enableWelcomeSkipping;
	protected boolean m_enablePincodeSkipping;
	protected String m_defaultPincodeIfSkipping;
	protected String m_autoLogoffTime;
//	protected String m_userPhonesRestUrl;
//	protected String m_supervisorHostRpcPort;
//	protected String m_supervisorHost;
	protected String m_sharedSecret;
	
	private static final Logger LOG = Logger.getLogger("org.sipfoundry.sipxhotdesking");
	
	public String getLogLevel() {
		return m_logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.m_logLevel = logLevel;
	}
	public String getLogFile() {
		return m_logFile;
	}
	public void setLogFile(String logFile) {
		this.m_logFile = logFile;
	}
	public int getEventSocketPort() {
		return m_eventSocketPort;
	}
	public void setEventSocketPort(int eventSocketPort) {
		this.m_eventSocketPort = eventSocketPort;
	}
	public String getDataDirectory() {
		return m_dataDirectory;
	}
	public void setDataDirectory(String dataDirectory) {
		this.m_dataDirectory = dataDirectory;
	}
	public String getPromptsDirectory() {
		return m_promptsDirectory;
	}
	public void setPromptsDirectory(String promptsDirectory) {
		this.m_promptsDirectory = promptsDirectory;
	}
	public String getDocDirectory() {
		return m_docDirectory;
	}
	public void setDocDirectory(String docDirectory) {
		this.m_docDirectory = docDirectory;
	}
	public String getSipxchangeDomainName() {
		return m_sipxchangeDomainName;
	}
	public void setSipxchangeDomainName(String sipxchangeDomainName) {
		this.m_sipxchangeDomainName = sipxchangeDomainName;
	}
	public String getRealm() {
		return m_realm;
	}
	public void setRealm(String realm) {
		this.m_realm = realm;
	}
	public String getLogoffUser() {
		return m_logoffUser;
	}
	public void setLogoffUser(String logoffUser) {
		this.m_logoffUser = logoffUser;
	}
	public boolean getEnableRelogging() {
		return m_enableRelogging;
	}
	public void setEnableRelogging(boolean enableRelogging) {
		this.m_enableRelogging = enableRelogging;
	}
	public boolean getEnablePromptLogoffOtherPhones() {
		return m_enablePromptLogoffOtherPhones;
	}
	public void setEnablePromptLogoffOtherPhones(
			boolean enablePromptLogoffOtherPhones) {
		this.m_enablePromptLogoffOtherPhones = enablePromptLogoffOtherPhones;
	}
	public boolean getEnableAutoLogoffOtherPhones() {
		return m_enableAutoLogoffOtherPhones;
	}
	public void setEnableAutoLogoffOtherPhones(boolean enableAutoLogoffOtherPhones) {
		this.m_enableAutoLogoffOtherPhones = enableAutoLogoffOtherPhones;
	}
	public boolean getEnableConfirmation() {
		return m_enableConfirmation;
	}
	public void setEnableConfirmation(boolean enableConfirmation) {
		this.m_enableConfirmation = enableConfirmation;
	}
	public boolean getEnableWelcomeSkipping() {
		return m_enableWelcomeSkipping;
	}
	public void setEnableWelcomeSkipping(boolean enableWelcomeSkipping) {
		this.m_enableWelcomeSkipping = enableWelcomeSkipping;
	}
	public boolean getEnablePincodeSkipping() {
		return m_enablePincodeSkipping;
	}
	public void setEnablePincodeSkipping(boolean enablePincodeSkipping) {
		this.m_enablePincodeSkipping = enablePincodeSkipping;
	}
	public String getDefaultPincodeIfSkipping() {
		return m_defaultPincodeIfSkipping;
	}
	public void setDefaultPincodeIfSkipping(String defaultPincodeIfSkipping) {
		this.m_defaultPincodeIfSkipping = defaultPincodeIfSkipping;
	}
//	public String getUserPhonesRestUrl() {
//		return m_userPhonesRestUrl;
//	}
//	public void setUserPhonesRestUrl(String userPhonesRestUrl) {
//		this.m_userPhonesRestUrl = userPhonesRestUrl;
//	}
//	public String getSupervisorHostRpcPort() {
//		return m_supervisorHostRpcPort;
//	}
//	public void setSupervisorHostRpcPort(String supervisorHostRpcPort) {
//		this.m_supervisorHostRpcPort = supervisorHostRpcPort;
//	}
//	public String getSupervisorHost() {
//		return m_supervisorHost;
//	}
//	public void setSupervisorHost(String supervisorHost) {
//		this.m_supervisorHost = supervisorHost;
//	}
	public String getSharedSecret() {
		return m_sharedSecret;
	}
	public void setSharedSecret(String sharedSecret) {
		this.m_sharedSecret = sharedSecret;
	}
	
	public String getAutoLogoffTime() {
		return m_autoLogoffTime;
	}
	public void setAutoLogoffTime(String autoLogoffTime) {
		this.m_autoLogoffTime = autoLogoffTime;
	}
	public String getLogoffCron() {
		//m_autoLogoffTime is of type HH:MM
		//Cron is of type 0 MM HH * * *
		Integer hours = Integer.parseInt(m_autoLogoffTime.split(":")[0]);
		Integer minutes = Integer.parseInt(m_autoLogoffTime.split(":")[1]);
		String cron = String.format("0 %d %d * * ?", minutes, hours);
		return cron;
	}
	
	@Override
	public Logger getLogger() {
		return LOG;
	}
}
