package org.sipfoundry.callcenter.hotdesking;

import java.util.Properties;

import org.sipfoundry.callcenter.hotdesking.exceptions.PhoneNotFoundException;
import org.sipfoundry.callcenter.hotdesking.exceptions.UserNotFoundException;

public class OfficeServiceImpl extends HotdeskingTriggerService {

	public static final String BEAN_ID = "officeService";
	
	private HotdeskingTaskWriter m_hotdeskingTaskWriter;
	
	public void setHotdeskingTaskWriter(HotdeskingTaskWriter hotdeskingTaskWriter) {
		this.m_hotdeskingTaskWriter = hotdeskingTaskWriter;
	}
	
	@Override
	public void login(String userId, String phoneId) throws UserNotFoundException, PhoneNotFoundException {
		String retrievedUserId = m_sipXdao.getUser(userId);
		if( retrievedUserId == null )
			throw new UserNotFoundException(userId);
		
		String contact = m_sipXdao.getContactByPhoneId(phoneId);
		if( contact == null ) 
			throw new PhoneNotFoundException(phoneId);
		
		String ipAddress = m_sipXdao.getIpFromContact(contact);
		Properties properties = m_hotdeskingTaskWriter.getPropertiesForLogin(retrievedUserId, phoneId, ipAddress);
		m_hotdeskingTaskWriter.write(properties);
	}

	@Override
	public void logout(String userId, String phoneId) throws UserNotFoundException, PhoneNotFoundException {
		String retrievedUserId = m_sipXdao.getUser(userId);
		if( retrievedUserId == null )
			throw new UserNotFoundException(userId);
		
		String contact = m_sipXdao.getContactByPhoneId(phoneId);
		if( contact == null ) 
			throw new PhoneNotFoundException(phoneId);
		
		String ipAddress = m_sipXdao.getIpFromContact(contact);
		Properties properties = m_hotdeskingTaskWriter.getPropertiesForLogout(retrievedUserId, phoneId, ipAddress);
		m_hotdeskingTaskWriter.write(properties);
	}
	
	
}
