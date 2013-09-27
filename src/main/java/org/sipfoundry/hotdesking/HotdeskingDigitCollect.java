//
// Copyright (c) 2011 Telecats B.V. All rights reserved. Contributed to SIPfoundry and eZuce, Inc. under a Contributor Agreement.
// This library or application is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License (AGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// This library or application is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License (AGPL) for more details.
//
//////////////////////////////////////////////////////////////////////////////
package org.sipfoundry.hotdesking;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.freeswitch.Collect;
import org.sipfoundry.commons.freeswitch.FreeSwitchEventSocketInterface;
import org.sipfoundry.commons.freeswitch.Localization;
import org.sipfoundry.commons.freeswitch.Play;
import org.sipfoundry.commons.freeswitch.PromptList;

/**
 * User's input collector
 * 
 * @author aliaksandr
 * 
 */
public class HotdeskingDigitCollect {
    static final Logger LOG = Logger.getLogger("org.sipfoundry.sipxhotdesking");
    /**
     * Localization support
     */
    protected Localization m_loc;
    /**
     * Freeswitch socket interface
     */
    protected FreeSwitchEventSocketInterface m_fses;

    int m_maxDigits = 1;
    int m_invalidMax = 2;
    int m_timeoutMax = 2;
    int m_initialTimeout = 5000;
    int m_interDigitTimeout = 3000;
    int m_extraDigitTimeout = 1000;

    PromptList m_prePromptPl;

    boolean m_isStarCancel = false;
    String m_termChars = "";
    String m_digits = "";
    private static final String NO_DIGITS_COLLECTED = "";

    public HotdeskingDigitCollect(Localization loc) {
        m_loc = loc;
        m_fses = loc.getFreeSwitchEventSocketInterface();
    }

    /**
     * Collect several DTMF digits (0-9) from the user. "*" will cancel. "#" is a terminating character
     * 
     * @param menuPl
     * @param maxDigits
     * @return
     */
    public String collectDigits(PromptList menuPl, int maxDigits) {
        m_maxDigits = maxDigits;
        m_isStarCancel = true;
        m_termChars = "#*";
        return collect(menuPl, "0123456789");
    }

    /**
     * Collect a single DTMF digit (0-9) from the user. "*" will cancel
     * 
     * @param menuPl
     * @param validDigits
     * @return
     */
    public String collectDigit(PromptList menuPl, String validDigits) {
        m_maxDigits = 1;
        m_isStarCancel = true;
        m_termChars = "#*";
        return collect(menuPl, validDigits);
    }

    /**
     * Collect several DTMF keypresses (0-9,#,*) from the user.
     * 
     * @param menuPl
     * @param maxDigits
     * @return
     */
    public String collectDtmf(PromptList menuPl, int maxDigits) {
        m_maxDigits = maxDigits;
        m_isStarCancel = false;
        m_termChars = "#";
        return collect(menuPl, "0123456789#*");
    }

    private String updateValidDigits(String validDigits) {
        validDigits = validDigits == null ? "" : validDigits;
        if (m_isStarCancel && !validDigits.contains("*")) {
            validDigits += "*";
        }
        return validDigits;
    }

    private PromptList getPrompList(PromptList menuPromptList, boolean playPrePrompt) {
        PromptList promptList = m_loc.getPromptList();

        if (m_prePromptPl != null && playPrePrompt) {
            promptList.addPrompts(m_prePromptPl);
        }

        if (menuPromptList != null) {
            promptList.addPrompts(menuPromptList);
        }

        return promptList;
    }

    private Play getPlayPrompt(PromptList promptList, String validDigits) {
        Play play = new Play(m_fses, promptList);
        play.setDigitMask(validDigits);
        return play;
    }

    private void playTimeoutPromptIfNeeded() {
        m_loc.play("HotdeskingCode_fail_nochoice", "0123456789#*");
    }

    private void playInvalidPromptIfNeeded() {
        m_loc.play("HotdeskingCode_error_invalid", "0123456789#*");
    }

    private Collect getCollect() {
        Collect collect;
        if (m_maxDigits == 1) {
            collect = new Collect(m_fses, 1, m_initialTimeout, 0, 0);
        } else {
            collect = new Collect(m_fses, m_maxDigits, m_initialTimeout, m_interDigitTimeout, m_extraDigitTimeout);
        }

        collect.setTermChars(m_termChars);
        return collect;
    }

    protected String collect(PromptList menuPl, String validDigits) {
        return this.collect(menuPl, validDigits, true);
    }

    protected String collect(PromptList menuPl, String validDigits, boolean playPrePrompt) {
        validDigits = this.updateValidDigits(validDigits);

        int invalidCount = 0;
        int timeoutCount = 0;

        boolean done = false;
        m_digits = NO_DIGITS_COLLECTED;
        while (timeoutCount <= m_timeoutMax && invalidCount <= m_invalidMax && !done) {
            PromptList promptList = this.getPrompList(menuPl, playPrePrompt);
            playPrePrompt = false;

            Collect collect = getCollect();
            collect.go();
            String digits = collect.getDigits();

            Play play = this.getPlayPrompt(promptList, validDigits);
            play.go();

            LOG.info("DigitCollect::collect Collected digits=(" + m_fses.redact(digits) + ")");

            // Is there a digit? If not, raise timeout count
            if (digits.isEmpty()) {
                //playTimeoutPromptIfNeeded();
                timeoutCount++;
                LOG.info("DigitCollect::collect timeout (" + timeoutCount + " / " + m_timeoutMax + ")");
            }

            // There are digits, does the collection contains a cancel char?
            else if (m_isStarCancel && digits.contains("*")) {
                LOG.info("DigitCollect::cancelled by * key");
                digits = NO_DIGITS_COLLECTED;
                done = true;
            }

            // Is there a max digits constraint? But it doesn't contain the valid digit(s).
            else if (m_maxDigits == 1 && !validDigits.contains(digits)) {
                playInvalidPromptIfNeeded();
                invalidCount++;
                LOG.info("DigitCollect::collect Invalid entry (" + invalidCount + " / " + m_invalidMax + ")");
            }

            // Is there a max digits constraint? If so, are the digit(s) valid?
            else if (m_maxDigits == 1 && validDigits.contains(digits)) {
                m_digits = digits;
                done = true;
            }

            // There are digits, no cancellation, no max digits restriction. This is the last possibility.
            else {
                m_digits = digits;
                done = true;
            }
        }
        return m_digits;
    }

    public int getInitialTimeout() {
        return m_initialTimeout;
    }

    /**
     * Set the time to wait for the first digit (in mS)
     * 
     * @param initialTimeout_mS
     */
    public void setInitialTimeout(int initialTimeout_mS) {
        m_initialTimeout = initialTimeout_mS;
    }

    public int getInterDigitTimeout() {
        return m_interDigitTimeout;
    }

    /**
     * Set the time to wait for the second and subsequent digits (in mS)
     * 
     * @param interDigitTimeout_mS
     */
    public void setInterDigitTimeout(int interDigitTimeout_mS) {
        m_interDigitTimeout = interDigitTimeout_mS;
    }

    public int getExtraDigitTimeout() {
        return m_extraDigitTimeout;
    }

    /**
     * Set the time to wait for the return key (in mS)
     * 
     * @param extraDigitTimeout_mS
     */
    public void setExtraDigitTimeout(int extraDigitTimeout_mS) {
        m_extraDigitTimeout = extraDigitTimeout_mS;
    }

    public int getInvalidMax() {
        return m_invalidMax;
    }

    public void setInvalidMax(int invalidMax) {
        m_invalidMax = invalidMax;
    }

    public int getTimeoutMax() {
        return m_timeoutMax;
    }

    public void setTimeoutMax(int timeoutMax) {
        m_timeoutMax = timeoutMax;
    }

    public PromptList getPrePromptPl() {
        return m_prePromptPl;
    }

    public void setPrePromptPl(PromptList prePromptPl) {
        m_prePromptPl = prePromptPl;
    }

}
