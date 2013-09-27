package org.sipfoundry.callcenter.hotdesking;

public class SipXDaoImpl extends SipXDao {

	@Override
	public String getIpByPhoneId(String phoneId) {
		String contact = getContactByPhoneId(phoneId);
		String ip = getIpFromContact(contact);
		return ip;
	}

	@Override
	public String getLastExtensionByPhoneId(String phoneId) {
		String contact = getContactByPhoneId(phoneId);
		String extension = getExtensionFromContact(contact);
		return extension;
	}

}
