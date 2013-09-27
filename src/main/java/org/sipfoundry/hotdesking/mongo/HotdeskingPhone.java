package org.sipfoundry.hotdesking.mongo;

public class HotdeskingPhone {

	protected String m_mac;
	protected String m_ip;
	//protected String identity;
	
	public String getMac() {
		return m_mac;
	}
	public void setMac(String mac) {
		this.m_mac = mac;
	}
	public String getIp() {
		return m_ip;
	}
	public void setIp(String ip) {
		this.m_ip = ip;
	}
	
	
}
