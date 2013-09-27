//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.hotdesking;

import java.util.Properties;

/**
 * Hotdesking task
 */
public class HotdeskingTask {
	public static final String CALLING_USER = "callingUser";
	public static final String NEW_USER = "newUser";

	/**
	 * Calling phone serial
	 */
	public static final String CALLING_PHONE_SERIAL = "phoneSerial";
	/**
	 * Just logout on calling phone
	 */
	public static final String LOGOUT_ON_THIS_PHONE = "logoutOnThisPhone";
	/**
	 * Logout everywhere (on all phones).
	 */
	public static final String LOGOUT_EVERYWHERE = "logoutEverywhere";
	/**
	 * Logout on all other phones and login only on calling phone
	 */
	public static final String HOTDESKING_LOGOUT_EVERYWHERE = "hotdeskingWithEverywhereLogout";
	/**
	 * Login on calling phone. Other phones with given user will stay online.
	 */
	public static final String HOTDESKING_WITHOUT_LOGOUT = "hotdeskingWithoutLogout";
	private static final String SIP_CONTACT_HOST = "sipContactHost";

	/**
	 * Current user. This is current user, binded to dialing phone (binded to
	 * Line)
	 */
	private String currentUserId;
	/**
	 * New user, that should be binded to phone/Line
	 */
	private String newUserId;

	/**
	 * Phone serial
	 */
	private String phoneSerial;

	private boolean logoutOnThisPhone;
	private boolean logoutEverywhere;
	private boolean hotdeskingWithEverywhereLogout;
	private boolean hotdeskingWithoutLogout;
	private String sipContactHost;

	@Override
	public String toString() {
		return "HotdeskingTask [currentUserId=" + currentUserId
				+ ", newUserId=" + newUserId + ", phoneSerial=" + phoneSerial
				+ ", logoutOnThisPhone=" + logoutOnThisPhone
				+ ", logoutEverywhere=" + logoutEverywhere
				+ ", hotdeskingWithEverywhereLogout="
				+ hotdeskingWithEverywhereLogout + ", hotdeskingWithoutLogout="
				+ hotdeskingWithoutLogout + "]";
	}

	/**
	 * Get as Properties object
	 * 
	 * @return
	 */
	public Properties getAsProperties() {
		final Properties taskPropertiies = new Properties();
		taskPropertiies.setProperty(CALLING_PHONE_SERIAL, phoneSerial);
		if (logoutOnThisPhone) {
			taskPropertiies.setProperty(LOGOUT_ON_THIS_PHONE, "true");
		} else if (logoutEverywhere) {
			taskPropertiies.setProperty(LOGOUT_EVERYWHERE, "true");
		} else if (hotdeskingWithoutLogout) {
			taskPropertiies.setProperty(HOTDESKING_WITHOUT_LOGOUT, "true");
		} else if (hotdeskingWithEverywhereLogout) {
			taskPropertiies.setProperty(HOTDESKING_LOGOUT_EVERYWHERE, "true");
		}
		if (currentUserId != null) {
			taskPropertiies.setProperty(CALLING_USER, currentUserId);
		}
		if (newUserId != null) {
			taskPropertiies.setProperty(NEW_USER, newUserId);
		}
		if (sipContactHost != null) {
			taskPropertiies.setProperty(SIP_CONTACT_HOST, sipContactHost);
		}
		return taskPropertiies;
	}

	public String getCurrentUserId() {
		return currentUserId;
	}

	public void setCurrentUserId(String currentUserId) {
		this.currentUserId = currentUserId;
	}

	public String getNewUserId() {
		return newUserId;
	}

	public void setNewUserId(String newUserId) {
		this.newUserId = newUserId;
	}

	public String getPhoneSerial() {
		return phoneSerial;
	}

	public void setPhoneSerial(String phoneSerial) {
		this.phoneSerial = phoneSerial;
	}

	public boolean isLogoutOnThisPhone() {
		return logoutOnThisPhone;
	}

	public void setLogoutOnThisPhone(boolean logoutOnThisPhone) {
		this.logoutOnThisPhone = logoutOnThisPhone;
	}

	public boolean isLogoutEverywhere() {
		return logoutEverywhere;
	}

	public void setLogoutEverywhere(boolean logoutEverywhere) {
		this.logoutEverywhere = logoutEverywhere;
	}

	public boolean isHotdeskingWithEverywhereLogout() {
		return hotdeskingWithEverywhereLogout;
	}

	public void setHotdeskingWithEverywhereLogout(
			boolean hotdeskingWithEverywhereLogout) {
		this.hotdeskingWithEverywhereLogout = hotdeskingWithEverywhereLogout;
	}

	public boolean isHotdeskingWithoutLogout() {
		return hotdeskingWithoutLogout;
	}

	public void setHotdeskingWithoutLogout(boolean hotdeskingWithoutLogout) {
		this.hotdeskingWithoutLogout = hotdeskingWithoutLogout;
	}

	public void setSipContactHost(String sipContactHost) {
		this.sipContactHost = sipContactHost;		
	}
	
	public String getSipContactHost() {
		return this.sipContactHost;
	}
}
