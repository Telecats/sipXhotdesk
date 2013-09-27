package org.sipfoundry.hotdesking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.sipfoundry.commons.log4j.SipFoundryLayout;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class HotdeskingServer {
	
	static final Logger LOG = Logger.getLogger("org.sipfoundry.hotdesking");
    private int m_eventSocketPort;
    private String m_logLevel;
    private String m_logFile;
	
	protected abstract HotdeskingHandler getHotdeskingHandler();

	public void runServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(m_eventSocketPort);
            for (;;) {
            	LOG.info("HotdeskingProcess received incoming request");
                Socket client = serverSocket.accept();
                HotdeskingHandler hotdesking = getHotdeskingHandler();
                hotdesking.setClient(client);
                Thread thread = new Thread(hotdesking);
                thread.start();
            }
        } catch (IOException ex) {
            System.out.println("FAILED TO START HOTDESKING SERVER" + ex);
            ex.printStackTrace();
            System.exit(1);
        }
    }
	
	public static void main(String[] args) {
		initSystemProperties();
		
    	ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {
    			"classpath:/org/sipfoundry/hotdesking/mongo/mongo.beans.xml",
        		"classpath:/org/sipfoundry/hotdesking/hotdesking.beans.xml"});
    	
    	HotdeskingServer hotdeskingProcess = (HotdeskingServer) context.getBean("hotdeskingServer");
    	hotdeskingProcess.runServer();
	}
	
	public void init() {
		Properties props = new Properties();
        props.setProperty("log4j.rootLogger", "warn, file");
        props.setProperty("log4j.logger.org.sipfoundry.hotdesking", SipFoundryLayout.mapSipFoundry2log4j(m_logLevel).toString());
        props.setProperty("log4j.appender.file", "org.sipfoundry.commons.log4j.SipFoundryAppender");
        props.setProperty("log4j.appender.file.File", m_logFile);
        props.setProperty("log4j.appender.file.layout", "org.sipfoundry.commons.log4j.SipFoundryLayout");
        props.setProperty("log4j.appender.file.layout.facility", "sipXhotdesking");
        PropertyConfigurator.configure(props);
	}
	
	private static void initSystemProperties() {
        String path = System.getProperty("conf.dir");
        if (path == null) {
            System.err.println("Cannot get System Property conf.dir!  Check jvm argument -Dconf.dir=") ;
            System.exit(1);
        }
        
        // Setup SSL properties so we can talk to HTTPS servers
        String keyStore = System.getProperty("javax.net.ssl.keyStore");
        if (keyStore == null) {
            // Take an educated guess as to where it should be
            keyStore = path+"/ssl/ssl.keystore";
            System.setProperty("javax.net.ssl.keyStore", keyStore);
            System.setProperty("javax.net.ssl.keyStorePassword", "changeit"); // Real security!
        }
        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        if (trustStore == null) {
            // Take an educated guess as to where it should be
            trustStore = path+"/ssl/authorities.jks";
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit"); // Real security!
        }
    }
	
	public void setEventSocketPort(int port) {
        m_eventSocketPort = port;
    }

    public void setLogLevel(String logLevel) {
        m_logLevel = logLevel;
    }

    public void setLogFile(String logFile) {
        m_logFile = logFile;
    }
}
