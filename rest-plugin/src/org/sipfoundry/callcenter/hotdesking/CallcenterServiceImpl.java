package org.sipfoundry.callcenter.hotdesking;

import java.rmi.RemoteException;

import nl.telecats.sipxecs.callcenter.callCenterService.CallCenterServiceException;
import nl.telecats.sipxecs.callcenter.callCenterService.CallCenterService_PortType;

import org.apache.log4j.Logger;
import org.sipfoundry.callcenter.hotdesking.exceptions.PhoneNotFoundException;

public class CallcenterServiceImpl extends HotdeskingTriggerService {

	public static final String BEAN_ID = "callcenterService";
	private static final Logger LOG = Logger.getLogger( CallcenterServiceImpl.class );
	
	private int m_pauseCode = 0;
	private CallCenterService_PortType m_callcenterServicePort;

	@Override
	public void login(String userId, String phoneId) throws PhoneNotFoundException, CallCenterServiceException {
		String contact = m_sipXdao.getContactByPhoneId(phoneId);
		
		if( contact == null ) 
			throw new PhoneNotFoundException(phoneId);
		
		String extension = m_sipXdao.getExtensionFromContact(contact);
		try {
			m_callcenterServicePort.login(userId, extension, m_pauseCode);
		} catch (CallCenterServiceException e) {
			throw e;
		} catch (RemoteException e) {
			throw new CallCenterServiceException(e.getMessage(), -1);
		}
	}

	@Override
	public void logout(String userId, String phoneId) throws PhoneNotFoundException, CallCenterServiceException {
		String contact = m_sipXdao.getContactByPhoneId(phoneId);
		
		if( contact == null ) 
			throw new PhoneNotFoundException(phoneId);
		
		String extension = m_sipXdao.getExtensionFromContact(contact);
		try {
			m_callcenterServicePort.logoff(userId, extension);
		} catch (CallCenterServiceException e) {
			throw e;
		} catch (RemoteException e) {
			throw new CallCenterServiceException(e.getMessage(), -1);
		}
	}

	public void setPauseCode(int pauseCode) {
		this.m_pauseCode = pauseCode;
	}

	public void setCallcenterServicePort(
			CallCenterService_PortType callcenterServicePort) {
		this.m_callcenterServicePort = callcenterServicePort;
	}
	
	
	
	
}
