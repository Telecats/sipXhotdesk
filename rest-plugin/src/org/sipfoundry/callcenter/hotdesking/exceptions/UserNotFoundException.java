package org.sipfoundry.callcenter.hotdesking.exceptions;

public class UserNotFoundException extends Exception {

	public UserNotFoundException(String user) {
		super(String.format("The requested user %s could not be found.", user));
	}
}
