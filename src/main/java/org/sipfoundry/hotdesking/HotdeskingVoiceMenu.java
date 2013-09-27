//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.hotdesking;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.freeswitch.FreeSwitchEventSocketInterface;
import org.sipfoundry.commons.freeswitch.Localization;
import org.sipfoundry.commons.freeswitch.PromptList;
import org.sipfoundry.commons.freeswitch.Sleep;
import org.sipfoundry.hotdesking.mongo.HotdeskingPermissionManager.UserNotFoundException;

/**
 * Hotdesking voice menu handler
 * 
 * @author aliaksandr
 * 
 */
public class HotdeskingVoiceMenu {
    private static final int OPTION_LOGIN = 1;
    private static final int OPTION_LOGIN_AND_LOGOFF_OTHER_PHONES = 2;

    private static final int OPTION_LOGOUT = 1;
    private static final int OPTION_LOGOUT_EVERYWHERE = 2;
    private static final int OPTION_RELOG = 3;
    private static final int OPTION_RELOG_AND_LOGOFF_OTHER_PHONES = 4;
    
    private static final int OPTION_CONFIRM = 1;

    static final Logger LOG = Logger.getLogger(HotdeskingVoiceMenu.class);

    /**
     * Max count for error pin enter
     */
    private int maxErrors = 2;
    /**
     * User's hotdesking service thread
     */
    private Hotdesking m_hotdeskingService;
    /**
     * Localization support
     */
    private Localization m_loc;
    /**
     * Hotdesking configuration
     */
    // private SipXconfiguration m_config;
    /**
     * Freeswitch socket inteface
     */
    private FreeSwitchEventSocketInterface m_fses;

    public HotdeskingVoiceMenu(Hotdesking service) {
        m_hotdeskingService = service;
        m_loc = service.getLoc();
        // m_config = service.getConfig();
        m_fses = m_loc.getFreeSwitchEventSocketInterface();
    }

    /**
     * Start point for user-service interacting
     * 
     * @param isPhoneLogouted
     * @return
     */
    public HotdeskingTask enterUserInformation(boolean isPhoneLogouted, boolean enableRelogging,
            boolean enableLogoffOthers, boolean confirmation, boolean skipWelcome, boolean useDefaultPincode,
            String defaultPincode, String logoffUserId) {
        HotdeskingTask task;
        if (isPhoneLogouted) {
            task = enterUserInformationOnLogoutedPhone(enableLogoffOthers, skipWelcome, useDefaultPincode,
                    defaultPincode);
        } else {
            task = enterUserInformationOnLoggedinPhone(enableRelogging, enableLogoffOthers, skipWelcome,
                    useDefaultPincode, defaultPincode, logoffUserId);
        }

        if (confirmation && task != null) {
            return askForConfirmation(task);
        } else {
            return task;
        }
    }

    private HotdeskingTask askForConfirmation(HotdeskingTask task) {
        int collectMenuChoice = 0;
        if (task != null) {
            m_loc.play("HotdeskingCode_confirmation", "" + OPTION_CONFIRM);
            collectMenuChoice = collectMenuChoice("" + OPTION_CONFIRM);
        }

        if (task != null && collectMenuChoice == 1) {
            return task;
        } else {
            return null;
        }
    }

    /**
     * Collect entered information on logouted phone
     * 
     * @return
     */
    private HotdeskingTask enterUserInformationOnLogoutedPhone(boolean enableLogoffOthers, boolean skipWelcome,
            boolean useDefaultPincode, String defaultPincode) {
        
    	String options = "" + OPTION_LOGIN;
        if (enableLogoffOthers) {
            options = "" + OPTION_LOGIN + OPTION_LOGIN_AND_LOGOFF_OTHER_PHONES;
        }

        if (!skipWelcome)
            m_loc.play("HotdeskingCode_welcome", options);
        
        boolean autoLogoffOtherPhones = m_hotdeskingService.getConfig().getEnableAutoLogoffOtherPhones();
        String htdUserParam = m_hotdeskingService.getParams().get("htdUser");
        boolean useHtdParam = htdUserParam != null && !htdUserParam.isEmpty();
        
        // If the user pre-entered an user-id, skip text
        int voiceMenuChoiceOnLoggedOutPhone;
        if( useHtdParam ) {
        	if( enableLogoffOthers && autoLogoffOtherPhones ) {
        		voiceMenuChoiceOnLoggedOutPhone = OPTION_LOGIN_AND_LOGOFF_OTHER_PHONES;
        	} else {
        		voiceMenuChoiceOnLoggedOutPhone = OPTION_LOGIN;
        	}
        } else {
        	if( autoLogoffOtherPhones ) {
        		voiceMenuChoiceOnLoggedOutPhone = OPTION_LOGIN_AND_LOGOFF_OTHER_PHONES;
        	} else if( options.length() == 1){
        		voiceMenuChoiceOnLoggedOutPhone = OPTION_LOGIN;
        	} else {
        		voiceMenuChoiceOnLoggedOutPhone = voiceMenuOnLogoutedPhone(options);
        	}
        }

        final HotdeskingTask collectNewUserInfo = collectNewUserInfo(useDefaultPincode, defaultPincode);
        if (collectNewUserInfo == null)
            return null;

        switch (voiceMenuChoiceOnLoggedOutPhone) {
        case OPTION_LOGIN:
            collectNewUserInfo.setHotdeskingWithoutLogout(true);
            break;
        case OPTION_LOGIN_AND_LOGOFF_OTHER_PHONES:
            collectNewUserInfo.setHotdeskingWithEverywhereLogout(true);
            break;
        default:
            return null;
        }

        return collectNewUserInfo;
    }

    /**
     * Enter user's information
     * 
     * note: we do allow relogging when relog prompt is disabled
     * @return
     */
    private HotdeskingTask enterUserInformationOnLoggedinPhone(boolean enableRelogging, boolean enableLogoffOthers,
            boolean skipWelcome, boolean useDefaultPincode, String defaultPincode, String logoffUserId) {
    	
    	String htdUserParam = m_hotdeskingService.getParams().get("htdUser");
    	boolean hasUserParam = htdUserParam != null && !htdUserParam.isEmpty();
    	boolean autoLogoffOthers = m_hotdeskingService.getConfig().getEnableAutoLogoffOtherPhones();
    	boolean playPrompt = true;
    	
    	String options = "";
    	int selection = -1;
    	if( hasUserParam ) {
    		selection = !autoLogoffOthers ? OPTION_RELOG : OPTION_RELOG_AND_LOGOFF_OTHER_PHONES;
    		playPrompt = false;
    	} else {
    		options += OPTION_LOGOUT;
    		if (enableLogoffOthers) {
                options += OPTION_LOGOUT_EVERYWHERE;
            }
            if (enableRelogging) {
                options += OPTION_RELOG;
            }
            if (enableLogoffOthers && enableRelogging) {
                options += OPTION_RELOG_AND_LOGOFF_OTHER_PHONES;
            }
            
            if( options.length() == 1 ) {
            	selection = OPTION_LOGOUT;
            	playPrompt = false;
            }
    	}
    	
        LOG.debug("HotdeskingEnter::options:" + options);

        // Say welcome to hotdesking
        if (!skipWelcome)
            m_loc.play("HotdeskingCode_welcome", options);
        
        if( playPrompt ) {
        	selection = voiceMenuLoggedInPhone(options);
        }
        
        if( autoLogoffOthers ) {
        	if( selection == OPTION_LOGOUT )
            	selection = OPTION_LOGOUT_EVERYWHERE;
        	
        	if( selection == OPTION_RELOG )
        		selection = OPTION_RELOG_AND_LOGOFF_OTHER_PHONES;
        }

        LOG.debug("HotdeskingEnter::selectedChoice:" + selection);
        LOG.debug("HotdeskingEnter::options:" + options);

        HotdeskingTask collectNewUserInfo = new HotdeskingTask();

        switch (selection) {
        case OPTION_LOGOUT:
            collectNewUserInfo.setLogoutOnThisPhone(true);
            collectNewUserInfo.setNewUserId(logoffUserId);
            break;
        case OPTION_LOGOUT_EVERYWHERE:
            collectNewUserInfo.setLogoutEverywhere(true);
            collectNewUserInfo.setNewUserId(logoffUserId);
            break;
        case OPTION_RELOG:
            collectNewUserInfo = collectNewUserInfo(useDefaultPincode, defaultPincode);
            if (collectNewUserInfo == null)
                return null;
            collectNewUserInfo.setHotdeskingWithoutLogout(true);
            break;
        case OPTION_RELOG_AND_LOGOFF_OTHER_PHONES:
            collectNewUserInfo = collectNewUserInfo(useDefaultPincode, defaultPincode);
            if (collectNewUserInfo == null)
                return null;
            collectNewUserInfo.setHotdeskingWithEverywhereLogout(true);
            break;
        default:
            return null;
        }
        return collectNewUserInfo;
    }

    /**
     * Voice menu on loggined phone
     * 
     * @return
     */
    private int voiceMenuLoggedInPhone(String options) {
        String prompt = "";
        if (options.contains("" + OPTION_LOGOUT))
            m_loc.play("HotdeskingCode_welcome_logout", "0123456789#*");
        if (options.contains("" + OPTION_LOGOUT_EVERYWHERE))
            m_loc.play("HotdeskingCode_welcome_logout_everywhere", "0123456789#*");
        if (options.contains("" + OPTION_RELOG))
            m_loc.play("HotdeskingCode_welcome_switch_user", "0123456789#*");
        if (options.contains("" + OPTION_RELOG_AND_LOGOFF_OTHER_PHONES))
            m_loc.play("HotdeskingCode_welcome_switch_user_logout_everywhere", "0123456789#*");

        return collectMenuChoice(options);
    }

    /**
     * Voice menu on logged out phone
     * 
     * @return
     */
    private int voiceMenuOnLogoutedPhone(String options) {
        if (options.contains("" + OPTION_LOGIN))
            m_loc.play("HotdeskingCode_welcome_login", options);
        if (options.contains("" + OPTION_LOGIN_AND_LOGOFF_OTHER_PHONES))
            m_loc.play("HotdeskingCode_welcome_login_logout_everywhere", options);
        return collectMenuChoice(options);
    }

    /**
     * Collecting user's choice
     * 
     * @param countOfMenuOptions
     * @return
     */
    private int collectMenuChoice(String menuOptions) {
        LOG.debug("HotdeskingEnter::voiceMenu Starting getting user's choice on menu");
        String choiceStr = null;
        int choice = -1;

        // Timeout count not used
        int invalidCount = 0, maxInvalid = 3;

        PromptList menuPromptList = new PromptList(m_loc);
        boolean done = false, parseError;
        while (!done && invalidCount < maxInvalid) {
            final HotdeskingDigitCollect dc = new HotdeskingDigitCollect(m_loc);
            choiceStr = dc.collectDigit(menuPromptList, menuOptions);
            try {
                parseError = false;
                choice = Integer.parseInt(choiceStr);
            } catch (NumberFormatException e) {
                parseError = true;
            }
            if (choice < 0 || !menuOptions.contains("" + choice) || parseError) {
                new Sleep(m_fses, 1000).go();
                LOG.debug("HotdeskingVoiceMenu::collectMenuChoice User's choice invalid. (" + choiceStr + ")");
                invalidCount++;
            } else {
                done = true;
                LOG.debug("HotdeskingVoiceMenu::collectMenuChoice User's choice successfully entered");
            }
        }

        return choice;
    }

    /**
     * Collection new user's info
     * 
     * @return
     */
    private HotdeskingTask collectNewUserInfo(boolean useDefaultPincode, String defaultPincode) {
        String htdUserParam = m_hotdeskingService.getParams().get("htdUser");
        boolean isUserAsParam = (htdUserParam != null && !htdUserParam.isEmpty());

        HotdeskingUser newUser = null;
        if (isUserAsParam) {
            try {
                newUser = m_hotdeskingService.getHotdeskingUser(htdUserParam);
            } catch (UserNotFoundException ex) {
                LOG.warn("HotdeskingEnter::enterUserInformation User not found: " + htdUserParam, ex);
            }
        }
        // Backup plan (e.g. wrong user is given)
        if (newUser == null) {
            m_loc.play("HotdeskingCode_enter_user_id", "0123456789#*");
            newUser = getNewUser();
        }

        if (newUser == null) {
            LOG.debug("HotdeskingEnter::enterUserInformation New user id is wrong");
            m_hotdeskingService.failure_nochoice();
            return null;
        } else {
            LOG.debug("HotdeskingEnter::getNewUser New userId successfully entered");
        }

        // does the new user have permission for hotdesking ?
        if (!newUser.isHotdeskingPermission()) {
            LOG.info("HotdeskingEnter::getNewUser User is not allowed to do hotdesking");
            m_hotdeskingService.noPermissionError();
            
            return null;
        }

        // boolean skipPincode =
        // m_hotdeskingService.hasGivenUserPermissionToSkipPincode(newUser.getUserId());
        if (!newUser.isHotdeskingWithoutPincodePermission()) {
            if (useDefaultPincode) {
                if (newUser.getUserPincode() == null || !newUser.getUserPincode().equals(defaultPincode)) {
                    // playPincodeInvalidPrompt();
                    LOG.warn("HotdeskingEnter::enterUserInformation Default pincode not accepted for user: "
                            + newUser.getUserId() + "@" + newUser.getRealm());
                    useDefaultPincode = false;
                } else {
                    LOG.debug("HotdeskingEnter::enterUserInformation Default pincode accepted for user: "
                            + newUser.getUserId() + "@" + newUser.getRealm());
                }
            }
            if (!useDefaultPincode) {
                this.playPincodePrompt();
                boolean checkNewUserPincode = checkNewUserPincode(newUser);
                if (!checkNewUserPincode) {
                    LOG.debug("HotdeskingEnter::enterUserInformation Pincode for new user id is wrong");
                    m_hotdeskingService.failure_tooManyTries();
                    return null;
                } else {
                    LOG.debug("HotdeskingEnter::checkNewUserPincode New userId pincode successfully entered");
                }
            }
        }

        final HotdeskingTask task = new HotdeskingTask();
        task.setNewUserId(newUser.getUserId());
        return task;
    }

    private void playUserPrompt() {
        m_loc.play("HotdeskingCode_enter_user_id", "0123456789#*");
    }

    private void playUserInvalidPrompt() {
        m_loc.play("HotdeskingCode_invalid_user_id", "0123456789#*");
    }

    private void playPincodePrompt() {
        m_loc.play("HotdeskingCode_enter_user_pincode", "0123456789#*");
    }

    private void playPincodeInvalidPrompt() {
        m_loc.play("HotdeskingCode_invalid_user_pincode", "0123456789#*");
    }

    private void playPincodeInvalidTryAgainPrompt() {
        playPincodeInvalidPrompt();
        playPincodePrompt();
    }

    /**
     * Get the new user id to use.
     * 
     * @return
     */
    private HotdeskingUser getNewUser() {
        LOG.debug("HotdeskingEnter::getNewUser Starting getting new user id");
        int errorCount = 0;
        String newUserId = null;

        PromptList authcodePromptList = new PromptList(m_loc);

        HotdeskingUser newUser = null;

        boolean done = false;
        HotdeskingDigitCollect dc = new HotdeskingDigitCollect(m_loc);
        newUserId = dc.collectDtmf(authcodePromptList, 10);
        while (!done && errorCount < maxErrors) {

            try {
                newUser = m_hotdeskingService.getHotdeskingUser(newUserId);
            } catch (UserNotFoundException ex) {
                LOG.warn("User not found: " + newUserId, ex);
            }
            // The new user is invalid, play the invalid tone
            if (newUser == null) {
                playUserInvalidPrompt();
                newUserId = dc.collectDtmf(authcodePromptList, 10);
                // playUserPrompt();
                errorCount++;
            } else {
                done = true;
            }
        }

        return newUser;
    }

    /**
     * Check the pincode for new user.
     * 
     * @return
     */
    private boolean checkNewUserPincode(HotdeskingUser newUser) {
        LOG.debug("HotdeskingEnter::checkNewUserPincode Starting getting for new user pincode");
        int errorCount = 0;
        String newUserIdPincode = null;
        PromptList authcodePl = new PromptList(m_loc);

        boolean done = false, valid = false;
        HotdeskingDigitCollect dc = new HotdeskingDigitCollect(m_loc);
        newUserIdPincode = dc.collectDtmf(authcodePl, 10);
        while (!done && errorCount < maxErrors) {
            if (newUserIdPincode == null || !newUserIdPincode.equals(newUser.getUserPincode())) {
                playPincodeInvalidTryAgainPrompt();
                newUserIdPincode = dc.collectDtmf(authcodePl, 10);

                errorCount++;
            } else {
                done = true;
                valid = true;
            }
        }

        return valid;
    }
}
