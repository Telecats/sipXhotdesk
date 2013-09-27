package org.sipfoundry.callcenter.hotdesking;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Filter;
import org.restlet.Route;
import org.restlet.Router;
import org.restlet.data.Request;
import org.sipfoundry.sipxrest.Plugin;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HotdeskingTriggerPlugin extends Plugin {
	
	private static final Logger LOG = Logger.getLogger(HotdeskingTriggerPlugin.class);
	private static final ClassPathXmlApplicationContext m_ctx;
	
	static {
		m_ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
	}

	@Override
	public void attachContext(Filter filter, Context context, Router router) {
		LOG.info("attaching context. Filter " + filter.toString() + ", Context " + context.toString() + ", Router "
                + router.toString());

		HotdeskingTriggerRestlet hotdeskingTriggerRestlet = m_ctx.getBean(HotdeskingTriggerRestlet.BEAN_ID, HotdeskingTriggerRestlet.class);
        filter.setNext(hotdeskingTriggerRestlet);
 
        String suffix = String.format("/{%s}/{%s}", new Object[] { HotdeskingTriggerParams.SERVICE,
                HotdeskingTriggerParams.METHOD });

        Route route = router.attach(this.getMetaInf().getUriPrefix() + suffix, filter);
        route.extractQuery(HotdeskingTriggerParams.USER_ID, HotdeskingTriggerParams.USER_ID, true);
        route.extractQuery(HotdeskingTriggerParams.PHONE_ID, HotdeskingTriggerParams.PHONE_ID, true);
	}

	@Override
	public String getAgent(Request request) {
		HotdeskingTriggerSettings hotdeskingTriggerSettings = m_ctx.getBean(HotdeskingTriggerSettings.BEAN_ID, HotdeskingTriggerSettings.class);
		return hotdeskingTriggerSettings.getAdminAgent();
	}

}
