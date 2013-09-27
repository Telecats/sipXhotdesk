//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.hotdesking;


/**
 * Holds the configuration data needed for Hotdesking.
 * 
 */
public class HotdeskingUser {
    /**
     * User Id
     */
    private String userId;
    /**
     * User pincode
     */
    private String userPincode;

    private String realm;
    
    private boolean hotdeskingPermission = false;
	boolean hotdeskingWithoutPincodePermission = false;
	boolean autoLogoffPermission = false;

    public HotdeskingUser() {
        super();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String authname) {
        userId = authname;
    }

    public String getUserPincode() {
        return userPincode;
    }

    public void setUserPincode(String authpassword) {
        userPincode = authpassword;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

	public boolean isHotdeskingPermission() {
		return hotdeskingPermission;
	}

	public void setHotdeskingPermission(boolean hotdeskingPermission) {
		this.hotdeskingPermission = hotdeskingPermission;
	}

	public boolean isHotdeskingWithoutPincodePermission() {
		return hotdeskingWithoutPincodePermission;
	}

	public void setHotdeskingWithoutPincodePermission(
			boolean hotdeskingWithoutPincodePermission) {
		this.hotdeskingWithoutPincodePermission = hotdeskingWithoutPincodePermission;
	}

	public boolean isAutoLogoffPermission() {
		return autoLogoffPermission;
	}

	public void setAutoLogoffPermission(boolean autoLogoffPermission) {
		this.autoLogoffPermission = autoLogoffPermission;
	}
	
	public String getUserPart() {
		return userId.split("@")[0];
	}
    
    

}
