//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.hotdesking;

import static java.lang.String.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.freeswitch.FreeSwitchEventSocketInterface;
import org.sipfoundry.commons.freeswitch.Hangup;
import org.sipfoundry.commons.freeswitch.Localization;
import org.sipfoundry.commons.freeswitch.Sleep;
import org.sipfoundry.hotdesking.mongo.HotdeskingPermissionManager;
import org.sipfoundry.hotdesking.mongo.HotdeskingPermissionManager.UserNotFoundException;
import org.sipfoundry.hotdesking.mongo.HotdeskingPhone;
import org.sipfoundry.hotdesking.mongo.HotdeskingPhoneManager;

/**
 * User's service thread. Starts for every user.
 * 
 * @author aliaksandr
 * 
 */
public class Hotdesking {

    static final Logger LOG = Logger.getLogger(Hotdesking.class);

    public static final String LOGOFF_USER = "hotdesking.logoffUser";
    public static final String HOTDESKING_ENABLE_AUTO_LOGOFF_OTHER_PHONES = "hotdesking.enable_auto_logoff_other_phones";

//    private static final String ENABLE_SKIP_WELCOME = "hotdesking.enableWelcomeSkipping";
//    private static final String ENABLE_SKIP_PINCODE = "hotdesking.enablePincodeSkipping";
//    private static final String DEFAULT_PINCODE_IF_SKIPPING_PINCODE = "hotdesking.defaultPincodeIfSkippingPincode";

    // Global store for resource bundles keyed by locale
    private static final String RESOURCE_NAME = "org.sipfoundry.hotdesking.HotdeskingCode";

    private static final String ALARM_USER_PHONES_REST_SERCICE_RETURNED_NO_200_RESPONSE = "USER_PHONES_REST_SERCICE_RETURNED_NO_200_RESPONSE";

    private static final String ALARM_ERROR_CALLING_USER_PHONES_REST_SERVICE = "ERROR_CALLING_USER_PHONES_REST_SERVICE";

    /**
     * Hotdesking configuration
     */
    private HotdeskingConfiguration m_hotdeskingConfig;
    /**
     * Localization support
     */
    private Localization m_loc;
    /**
     * Freeswitch socket interface
     */
    private FreeSwitchEventSocketInterface m_fses;
    /**
     * SipX configuration: permissions; credetials; registrations
     */
    //private SipXconfiguration m_config;
    /**
     * Locale
     */
    private String m_localeString;
    /**
     * The parameters from the sip URI
     */
    private Hashtable<String, String> m_parameters;
    //private DomainConfiguration domainConfig = new DomainConfiguration(System.getProperty("conf.dir")
    //        + "/domain-config");
    
    private HotdeskingPermissionManager m_hotdeskingPermissionManager;
    private HotdeskingPhoneManager m_hotdeskingPhoneManager;

    /**
     * Create an HotdeskingCode object.
     * 
     * @param hotdeskingcodeConfig top level configuration stuff
     * @param fses The FreeSwitchEventSocket with the call already answered
     * @param parameters The parameters from the sip URI (to determine locale and which action to
     *        run)
     */
    public Hotdesking(HotdeskingConfiguration hotdeskingConfig, FreeSwitchEventSocketInterface fses,
            Hashtable<String, String> parameters, HotdeskingPermissionManager hotdeskingPermissionManager, HotdeskingPhoneManager hotdeskingPhoneManager) {
        LOG.debug("HotdeskingCode init params");

        this.m_hotdeskingConfig = hotdeskingConfig;
        this.m_fses = fses;
        this.m_parameters = parameters;
        this.m_hotdeskingPermissionManager = hotdeskingPermissionManager;
        this.m_hotdeskingPhoneManager = hotdeskingPhoneManager;

        // Look for "locale" parameter
        m_localeString = m_parameters.get("locale");
        if (m_localeString == null) {
            // Okay, try "lang" instead
            m_localeString = m_parameters.get("lang");
        }
        if (m_localeString == null) {
            // Default to english
            m_localeString = "en";
        }
    }
    
    public HotdeskingConfiguration getConfig() {
    	return m_hotdeskingConfig;
    }

    /**
     * Load all the needed configuration.
     * 
     */
    void loadConfig() {
        LOG.debug("HotdeskingCode::loadConfig Load the hotdesking configuration");
        LOG.info("hotdesking language:" + m_localeString);
        m_loc = new Localization(RESOURCE_NAME, m_localeString, m_fses.getConfig(), m_fses);
        //m_config = SipXconfiguration.update(true);
    }

    /**
     * Run Hotdesking for each call.
     * 
     */
    public void run() throws Throwable {
        LOG.debug("HotdeskingCode::run Run Hotdesking");

        if (m_loc == null) {
            loadConfig();
        }
        
        String userId = m_fses.getVariable("variable_sip_from_uri");
        HotdeskingUser callingUser;
        
        try {
        	callingUser = m_hotdeskingPermissionManager.getHotdeskingUser(userId);
        } catch(UserNotFoundException ex) {
        	LOG.info("The calling user " + userId + " is not found in the database");
        	noPermissionError();
        	return;
        }

        if (!callingUser.isHotdeskingPermission()) {
            LOG.info("The calling user " + callingUser + " is not allowed to use hotdesking");
            noPermissionError();
            return;
        }
        
        String phoneSerial = m_hotdeskingPhoneManager.getMac(callingUser.getUserPart(), m_fses.getVariable("variable_sip_contact_host"));
        if (phoneSerial == null) {
            LOG.error("HotdeskingCode::run Unable resolve calling phone serial (MAC)");
            unknownMacAddressError();
            return;
        }

        String sipContactHost = m_fses.getVariable("variable_sip_contact_host");
//        String enableReloggingString = m_hotdeskingConfig.getEnableRelogging();
//        String enablePromptLogoffOtherPhonesString = m_hotdeskingConfig.getEnablePromptLogoffOtherPhones();
//        String enableConfirmationString = m_hotdeskingConfig.getEnableConfirmation();
//        String enableWelcomeSkippingString = m_hotdeskingConfig.getEnableWelcomeSkipping();

        // This are the options that sipXecs can set in hotdesking configuration.
        // This is NOT the user/group permission!!
//        String useDefaultPincodeString = m_hotdeskingConfig.getEnablePincodeSkipping();
//        String defaultPincodeIfSkippingPincode = m_hotdeskingConfig.getDefaultPincodeIfSkipping();

//        LOG.debug("HotdeskingCode::callingUser                             : " + callingUser);
//        LOG.debug("HotdeskingCode::phoneSerial                             : " + phoneSerial);
//        LOG.debug("HotdeskingCode::variableSipContactHost                  : " + sipContactHost);
//        LOG.debug("HotdeskingCode::run enableReloggingString               : " + enableReloggingString);
//        LOG.debug("HotdeskingCode::run enablePromptLogoffOtherPhonesString : " + enablePromptLogoffOtherPhonesString);
//          LOG.debug("HotdeskingCode::run enableConfirmationString            : " + enableConfirmationString);
//          LOG.debug("HotdeskingCode::run enableWelcomeSkippingString         : " + enableWelcomeSkippingString);
//        LOG.debug("HotdeskingCode::run useDefaultPincodeString             : " + useDefaultPincodeString);
//        LOG.debug("HotdeskingCode::run defaultPincodeIfSkippingPincode     : " + defaultPincodeIfSkippingPincode);
//        boolean enableRelogging = Boolean.parseBoolean(enableReloggingString);
//        boolean enablePromptLogoffOtherPhones = Boolean.parseBoolean(enablePromptLogoffOtherPhonesString);
//        boolean enableConfirmation = Boolean.parseBoolean(enableConfirmationString);
//        boolean enableWelcomeSkipping = Boolean.parseBoolean(enableWelcomeSkippingString);
//        boolean useDefaultPincode = Boolean.parseBoolean(useDefaultPincodeString);

//        LOG.debug("HotdeskingCode::run enableRelogging                     : " + enableRelogging);
//        LOG.debug("HotdeskingCode::run enablePromptLogoffOtherPhones       : " + enablePromptLogoffOtherPhones);
//        LOG.debug("HotdeskingCode::run enableConfirmation                  : " + enableConfirmation);
//        LOG.debug("HotdeskingCode::run enableWelcomeSkipping               : " + enableWelcomeSkipping);
//        LOG.debug("HotdeskingCode::run useDefaultPincode               : " + useDefaultPincode);

        // Wait a bit so audio doesn't start too fast
        Sleep s = new Sleep(m_fses, 1000);
        s.go();
        // Play the feature start tone.
        m_fses.setRedactDTMF(false);
        m_fses.trimDtmfQueue(""); // Flush the DTMF queue

        // Enter to user's voice menu
        final HotdeskingTask hotdeskingTask = getUserInformation(callingUser.getUserId(), m_hotdeskingConfig.m_enableRelogging,
        		m_hotdeskingConfig.m_enablePromptLogoffOtherPhones, m_hotdeskingConfig.m_enableConfirmation, m_hotdeskingConfig.m_enableWelcomeSkipping, m_hotdeskingConfig.m_enablePincodeSkipping,
        		m_hotdeskingConfig.m_defaultPincodeIfSkipping);
        if (hotdeskingTask != null) {
            hotdeskingTask.setSipContactHost(sipContactHost);
            hotdeskingTask.setPhoneSerial(phoneSerial);
            LOG.info("HotdeskingCode::run New user information entered         = " + hotdeskingTask);
            hotdeskingTask.setCurrentUserId(callingUser.getUserId());

            // if new user doesn't have the hotdesking permission, it is only allowed to login
            // once (meaning login on max 1 phone)
            HotdeskingUser newHotdeskingUser;
            try {
            	newHotdeskingUser = m_hotdeskingPermissionManager.getHotdeskingUser(hotdeskingTask.getNewUserId());
            } catch(UserNotFoundException ex) {
            	newHotdeskingUser = new HotdeskingUser();
            	LOG.info("HotdeskingCode:: NEW user not found: "+hotdeskingTask.getNewUserId(), ex);
            }
            boolean newUserHasHotdeskingPermission = newHotdeskingUser.isHotdeskingPermission();
            
            LOG.info("NEW user has permission: " + newUserHasHotdeskingPermission);
            
            if (!newUserHasHotdeskingPermission) {
                if (isLoggedInOnPhone(hotdeskingTask.getNewUserId())) {
            
                LOG.info("HotdeskingCode::The new user " + hotdeskingTask.getNewUserId()
                        + " is not allowed to use hotdesking and is already logged in.");
                }
                
                noPermissionError();
                return;
            }

            //createHotdeskingTask(hotdeskingTask);
            TaskFileWriter.createHotdeskingTask(hotdeskingTask, m_hotdeskingConfig.m_logoffUser, m_hotdeskingConfig.m_enableAutoLogoffOtherPhones);

            if (hotdeskingTask.isLogoutEverywhere() || hotdeskingTask.isLogoutOnThisPhone()) {
                LOG.debug("HotdeskingCode::promppt logoff success");
                m_loc.play("HotdeskingCode_hotdesking_success_logoff", "");
            } else {
                LOG.debug("HotdeskingCode::promppt login success");
                m_loc.play("HotdeskingCode_hotdesking_success_login", "");
            }
        } else {
            LOG.warn("HotdeskingCode::no New user information available");
        }
        LOG.info("HotdeskingCode::run Ending HotdeskingCode");
    }

    private boolean isLoggedInOnPhone(String userName) {
    	HotdeskingPhone phone = m_hotdeskingPhoneManager.getOnePhone(userName, m_hotdeskingConfig.getRealm());
        return phone != null;
    }

    public static void raiseAlarm(String id, Object... args) {
        LOG.error("ALARM_HOTDESKING_" + format(id, args));
    }

    /**
     * Resolving phone serial by ip
     * 
     * @return
     */
//    private String resolvePhoneSerial() {
//        String phoneHost = m_fses.getVariable("variable_sip_contact_host");
//        if (phoneHost == null) {
//            LOG.error("HotdeskingCode::resolvePhoneSerial Unable resolve contact host (phone ip)");
//            return null;
//        }
//        final String phoneSerial = m_config.getPhoneSerial(phoneHost);
//        return phoneSerial;
//    }

    /**
     * Creating task for hotdesking config update
     * 
     * @param newUserInformation
     */
    private void createHotdeskingTask(HotdeskingTask newUserInformation) {
        LOG.info("HotdeskingCode::createHotdeskingTask Creating task for Hotdesking");
        String path = System.getProperty("tmp.dir");
        if (path == null) {
            LOG.fatal("HotdeskingCode::createHotdeskingTask Cannot get System Property tmp.dir!  Check jvm argument -Dtmp.dir=");
            return;
        }
        final File taskDir = new File(path + File.separator + "hotdesking");
        if (!taskDir.exists()) {
            // create dir for hotdesking tasks
            taskDir.mkdir();
        } else {
            if (!taskDir.canWrite()) {
                LOG.fatal("HotdeskingCode::createHotdeskingTask Cannot write to temp folder: "
                        + taskDir.getAbsolutePath());
                return;
            }
        }

        // create uniq file
        File taskFile = new File(taskDir, System.currentTimeMillis() + "");
        while (taskFile.exists()) {
            taskFile = new File(taskDir, System.currentTimeMillis() + "");
        }
        LOG.info("HotdeskingCode::createHotdeskingTask Created temp task file: " + taskFile.getAbsolutePath());
        // write task content
        final Properties asProperties = newUserInformation.getAsProperties();

        // add logoff user
        String logoffUser = m_hotdeskingConfig.getLogoffUser();
        boolean enableAutoLogoffOtherPhones = m_hotdeskingConfig.m_enableAutoLogoffOtherPhones;

        LOG.debug("HotdeskingCode::logoff user                             :" + logoffUser);
        LOG.debug("HotdeskingCode::auto logoff other phones                : " + enableAutoLogoffOtherPhones);
        asProperties.setProperty(LOGOFF_USER, logoffUser);
        asProperties.setProperty(HOTDESKING_ENABLE_AUTO_LOGOFF_OTHER_PHONES, enableAutoLogoffOtherPhones + "");

        BufferedWriter out = null;
        try {
            final FileWriter fstream = new FileWriter(taskFile);
            out = new BufferedWriter(fstream);
            asProperties.store(out, "Hotdesking task file");
            out.flush();
            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            LOG.error(
                    "HotdeskingCode::createHotdeskingTask Cannot write to temp file: " + taskFile.getAbsolutePath()
                            + ": " + e.getMessage(), e);
            return;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        // rename file
        final File dest = new File(taskDir, taskFile.getName() + ".hotdesking");
        boolean renameTo = taskFile.renameTo(dest);
        if (!renameTo) {
            LOG.error("HotdeskingCode::createHotdeskingTask Renaming task file fail!");
            return;
        }
        LOG.info("HotdeskingCode::createHotdeskingTask Successfully created task file: " + dest.getAbsolutePath());
    }

    /**
     * Do the prompting for new user information.
     * 
     */
    private HotdeskingTask getUserInformation(String callerUser, boolean enableRelogging,
            boolean enablePromptLogoffOthers, boolean enableConfirmation, boolean skipWelcome,
            boolean useDefaultPincode, String defaultPincode) {
        LOG.debug("HotdeskingCode::getUserInformation Do the prompting for user's information");
        final HotdeskingVoiceMenu hotdeskingEnter = new HotdeskingVoiceMenu(this);

        // determine if we are in logoff state
        String logoffUserId = m_hotdeskingConfig.getLogoffUser();
        if (logoffUserId == null || logoffUserId.isEmpty()) {
            LOG.warn("No logoff user configured, please configure it using sipXconfig webUI, features->hotdesking");
        }
        boolean isloggedOff = callerUser.startsWith(logoffUserId);

        final HotdeskingTask enterUserInformation = hotdeskingEnter.enterUserInformation(isloggedOff,
                enableRelogging, enablePromptLogoffOthers, enableConfirmation, skipWelcome, useDefaultPincode,
                defaultPincode, logoffUserId);

        return enterUserInformation;

    }

    /**
     * Process failure
     */
    public void failure_nochoice() {
        LOG.debug("HotdeskingCode:: no choice made. hangup");
        m_loc.play("HotdeskingCode_fail_nochoice", "");
        new Hangup(m_fses).go();
    }
    
    public void failure_invalidchoice() {
    	LOG.debug("HotdeskingCode:: Invalid choice, hangup");
        m_loc.play("HotdeskingCode_error_invalid", "");
        new Hangup(m_fses).go();
    }
        
    public void noPermissionError() {
        LOG.debug("HotdeskingCode::no permission Play an error hang up tone");
        m_loc.play("HotdeskingCode_no_permission_hang_up", "");
        new Hangup(m_fses).go();
    }

    public void unknownMacAddressError() {
        LOG.debug("HotdeskingCode::unknown mac address Play an error hang up tone");
        m_loc.play("HotdeskingCode_unknown_macaddress_hang_up", "");
        new Hangup(m_fses).go();
    }
    
    public void failure_tooManyTries() {
        LOG.debug("HotdeskingCode::User failed too many times to enter proper input, hangup");
        m_loc.play("HotdeskingCode_too_many_tries", "");
        new Hangup(m_fses).go();
    }
    

//    public SipXconfiguration getConfig() {
//        return m_config;
//    }
//
//    public void setConfig(SipXconfiguration config) {
//        m_config = config;
//    }

    public void setLocalization(Localization localization) {
        m_loc = localization;
    }

    public Localization getLoc() {
        return (m_loc);
    }

    /**
     * Play error message
     * 
     * @param errPrompt
     * @param vars
     */
    public void playError(String errPrompt, String... vars) {
        LOG.debug("HotdeskingCode::playError Play error");
        m_loc.play("error_beep", "");
        m_fses.trimDtmfQueue("");
        m_loc.play(errPrompt, "0123456789*#", vars);
    }

    public Hashtable<String, String> getParams() {
        return m_parameters;
    }

	public HotdeskingUser getHotdeskingUser(String htdUserParam) throws UserNotFoundException {
		return m_hotdeskingPermissionManager.getHotdeskingUser(htdUserParam);
	}
}
