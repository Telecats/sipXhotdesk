package org.sipfoundry.hotdesking;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.freeswitch.Answer;
import org.sipfoundry.commons.freeswitch.DisconnectException;
import org.sipfoundry.commons.freeswitch.FreeSwitchEventSocketInterface;
import org.sipfoundry.commons.freeswitch.Hangup;
import org.sipfoundry.hotdesking.mongo.HotdeskingPermissionManager;
import org.sipfoundry.hotdesking.mongo.HotdeskingPhoneManager;

public class HotdeskingHandler implements Runnable {

    static final Logger LOG = Logger.getLogger("org.sipfoundry.hotdesking");
    private Socket m_client;
    private static final String SIP_H_DIVERSION = "variable_sip_h_diversion";

    private FreeSwitchEventSocketInterface m_fsEventSocket;
    private HotdeskingConfiguration m_hotdeskingConfig;
    private HotdeskingPermissionManager m_hotdeskingPermissionManager;
    private HotdeskingPhoneManager m_hotdeskingPhoneManager;

	/**
     * Determine what to do based on the SIP request.
     * 
     */
    public void run() {   	
    	LOG.debug("SipXhotdesking::run Starting SipXhotdesking thread with client " + m_client);
        try {
            if (m_fsEventSocket.connect(m_client, null)) {

                LOG.debug("SipXhotdesking socket connection, event received");
                String sipReqParams = m_fsEventSocket.getVariable("variable_sip_req_params");
                // Create a table of parameters to pass in
                Hashtable<String, String> parameters = new Hashtable<String, String>();

                if (sipReqParams != null) {
                    // Split parameter fields (separated by semicolons)
                    String[] params = sipReqParams.split(";");
                    for (String param : params) {
                        // Split key value pairs (separated by optional equal sign)
                        String[] kvs = param.split("=", 2);
                        if (kvs.length == 2) {
                            parameters.put(kvs[0], kvs[1]);
                        } else {
                            parameters.put(kvs[0], "");
                        }
                    }
                }

                LOG.info(String.format("SipXhotdesking::run Accepting call-id %s from %s to %s",
                		m_fsEventSocket.getVariable("variable_sip_call_id"),
                		m_fsEventSocket.getVariable("variable_sip_from_uri"),
                		m_fsEventSocket.getVariable("variable_sip_req_uri")));

                // Answer the call.
                m_fsEventSocket.invoke(new Answer(m_fsEventSocket));

                String action = parameters.get("command");
                if (action == null) {
                    LOG.warn("Cannot determine which application to run as the action parameter is missing.");
                } else if (action.equals("disa")) {
                    LOG.warn("Should start disa app");
                    parseHeader(m_fsEventSocket, parameters);
                } else if (action.equals("hotdesking")) {
                    LOG.info("Start Hotdesking process");
                    // Run the Hotdesking Code app.
                    Hotdesking app = new Hotdesking(m_hotdeskingConfig, m_fsEventSocket,
                            parameters, m_hotdeskingPermissionManager, m_hotdeskingPhoneManager);
                    app.run();
                } else {
                    // Nothing else to run...
                    LOG.warn("Cannot determine which application to run from command=" + action);
                }
            }
        } catch (DisconnectException e) {
            LOG.info("SipXhotdesking::run Far end hungup.");
        } catch (Throwable t) {
            LOG.error("SipXhotdesking::run", t);
        } finally {
            try {
            	m_fsEventSocket.invoke(new Hangup(m_fsEventSocket));
            	m_fsEventSocket.close();
            } catch (IOException e) {
                // Nothing to do, no where to go home...
                LOG.warn("SipXhotdesking::Could not close socket after the client disconnected.", e);
            }
        }

        LOG.debug("SipXhotdesking::run Ending SipXhotdesking thread with client " + m_client);
    }
	
	/**
     * Parse headers. header looks like:
     * variable_sip_h_diversion=<tel:3948809>;reason=no-answer;counter=1;screen=no;privacy=off
     */
    private void parseHeader(FreeSwitchEventSocketInterface fses, Hashtable<String, String> parameters) {

        String divHeader = fses.getVariable(SIP_H_DIVERSION);

        if (divHeader != null) {
            divHeader = divHeader.toLowerCase();
            String[] subParms = divHeader.split(";");

            int index = divHeader.indexOf("tel:");

            if (index >= 0) {
                divHeader = divHeader.substring(index + 4);
                index = divHeader.indexOf(">");
                if (index > 0) {
                    divHeader = divHeader.substring(0, index);

                    parameters.put("action", "deposit");
                    parameters.put("origCalledNumber", divHeader);

                    // now look for call forward reason
                    for (String param : subParms) {
                        if (param.startsWith("reason=")) {
                            param = param.substring("reason=".length());
                            param.trim();
                            parameters.put("call-forward-reason", param);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public void setClient(Socket client) {
		this.m_client = client;
	}
	
	public void setFsEventSocket(
			FreeSwitchEventSocketInterface fsEventSocket) {
		this.m_fsEventSocket = fsEventSocket;
	}

	public void setHotdeskingConfig(
			HotdeskingConfiguration hotdeskingConfig) {
		this.m_hotdeskingConfig = hotdeskingConfig;
	}

	public void setHotdeskingPermissionManager(
			HotdeskingPermissionManager hotdeskingPermissionManager) {
		this.m_hotdeskingPermissionManager = hotdeskingPermissionManager;
	}
	
	public void setHotdeskingPhoneManager(
			HotdeskingPhoneManager hotdeskingPhoneManager) {
		this.m_hotdeskingPhoneManager = hotdeskingPhoneManager;
	}
}
