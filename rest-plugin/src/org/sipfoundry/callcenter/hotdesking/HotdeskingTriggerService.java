package org.sipfoundry.callcenter.hotdesking;

import nl.telecats.sipxecs.callcenter.callCenterService.CallCenterServiceException;

import org.sipfoundry.callcenter.hotdesking.exceptions.PhoneNotFoundException;
import org.sipfoundry.callcenter.hotdesking.exceptions.UserNotFoundException;

public abstract class HotdeskingTriggerService {
	
	protected SipXDao m_sipXdao;
	
	public void setSipXdao(SipXDao sipXdao) {
		this.m_sipXdao = sipXdao;
	}
	
	public abstract void login(String userId, String phoneId) throws UserNotFoundException, PhoneNotFoundException, CallCenterServiceException;
	public abstract void logout(String userId, String phoneId)  throws UserNotFoundException, PhoneNotFoundException, CallCenterServiceException;
	
	
}
