package org.sipfoundry.hotdesking.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.mongo.MongoSpringTemplate;
import org.springframework.beans.factory.annotation.Required;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

public class HotdeskingPhoneManager {

	private MongoSpringTemplate m_registrarSpringTemplate;
	static final Logger LOG = Logger.getLogger(HotdeskingPhoneManager.class);
	private static Pattern addressPattern = Pattern.compile("^<sip:.*@([a-zA-Z0-9\\.]+):.*$");

	@Required
	public void setRegistrarSpringTemplate(
			MongoSpringTemplate registrarSpringTemplate) {
		this.m_registrarSpringTemplate = registrarSpringTemplate;
	}
	
	private DBCollection getRegistrarCollection() {
		return m_registrarSpringTemplate.getDb().getCollection("registrar");
	}
	
	public HotdeskingPhone getOnePhone(String userId, String realm) {
		DBObject query = QueryBuilder.start("identity").is(userId + "@" + realm).get();
		DBCursor phoneCursor = getRegistrarCollection().find(query);
		HotdeskingPhone phone = null;
		DBObject phoneObj;
		boolean done = false;
		while(phoneCursor.hasNext()) {
			phoneObj = phoneCursor.next();
			phone = new HotdeskingPhone();
			
			String mac = (String)phoneObj.get("instrument");
			String contact = (String)phoneObj.get("contact");
			String ip = "";
			Matcher m = addressPattern.matcher(contact);
			if(m.find()) {
				ip = m.group(1);
				done = true;
			}
			phone.setIp(ip);
			phone.setMac(mac);
			if(done)
				return phone;
		}
		return phone;
	}
	
	public List<HotdeskingPhone> getPhonesForUser(String userId, String realm) {
		DBObject query = QueryBuilder.start("identity").is(userId + "@" + realm).get();
		DBCursor phoneCursor = getRegistrarCollection().find(query);
		List<HotdeskingPhone> phones = new ArrayList<HotdeskingPhone>();
		HotdeskingPhone phone;
		DBObject phoneObj;
		boolean done;
		while(phoneCursor.hasNext()) {
			phoneObj = phoneCursor.next();
			phone = new HotdeskingPhone();
			
			String mac = (String)phoneObj.get("instrument");
			String contact = (String)phoneObj.get("contact");
			String ip = "";
			Matcher m = addressPattern.matcher(contact);
			if(m.find()) {
				ip = m.group(1);
			}
			phone.setIp(ip);
			phone.setMac(mac);
			phones.add(phone);
		}
		return phones;
	}
	
	public String getMac(String user, String host) {
		DBObject query = QueryBuilder.start("contact").regex(Pattern.compile("<sip:" + user + "@" + host + ".*>")).get();
		DBObject phoneObject = getRegistrarCollection().findOne(query);
		String mac = null;
		if(phoneObject != null) {
			mac = (String) phoneObject.get("instrument");
			
		}
			
		LOG.debug(String.format("Ran query %s on collection %s query with result %s", query, "registrar", phoneObject));
		LOG.info(String.format("Retrieved mac: %s for contact: <sip:%s@%s>", mac, user, host));
		return mac;
	}
}
