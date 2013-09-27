package org.sipfoundry.callcenter.hotdesking.exceptions;

public class PhoneNotFoundException extends Exception {

	public PhoneNotFoundException(String phone) {
		super(String.format("The requested phone %s could not be found.", phone));
	}
}
