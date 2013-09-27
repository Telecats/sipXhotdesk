package org.sipfoundry.callcenter.hotdesking;

import static org.sipfoundry.commons.mongo.MongoConstants.ALIAS;
import static org.sipfoundry.commons.mongo.MongoConstants.ALIASES;
import static org.sipfoundry.commons.mongo.MongoConstants.ALIAS_ID;
import static org.sipfoundry.commons.mongo.MongoConstants.ID;
import static org.sipfoundry.commons.mongo.MongoConstants.RELATION;
import static org.sipfoundry.commons.mongo.MongoConstants.UID;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sipfoundry.commons.mongo.MongoSpringTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

public abstract class SipXDao {
	
	public static final String BEAN_ID = "sipXdao";
	public static Pattern addressPattern = Pattern.compile("^<sip:(.*)@([a-zA-Z0-9\\.]+):.*$");
	
	public abstract String getIpByPhoneId(String phoneId);
	public abstract String getLastExtensionByPhoneId(String phoneId);
	

	protected MongoSpringTemplate m_registrar;
	protected MongoSpringTemplate m_imdb;
	
	public void setRegistrar(MongoSpringTemplate registrar) {
		this.m_registrar = registrar;
	}
	
	public void setImdb(MongoSpringTemplate imdb) {
		this.m_imdb = imdb;
	}
	
	private DBCollection getImdbCollection() {
		return m_imdb.getDb().getCollection("entity");
	}
	
	private DBCollection getRegistrarCollection() {
		return m_registrar.getDb().getCollection("registrar");
	}
	
	/**
	 * findOne( 
	 * 		{ //Query 
	 * 			$or: [ 
	 * 				{ uid: "205" }, 
	 * 				{ "als.rln": "alias", "als.id": "205" } 
	 * 			] 
	 * 		}, 
	 * 		{ //Projection
	 * 			_id:0, uid: 1 
	 * 		}
	 *  )
	 *  
	 */
	public String getUser(String userOrAliasId) {
		userOrAliasId = userOrAliasId.split("@")[0];
		
		String ID_IN_ALIAS = String.format("%s.%s", ALIASES, ALIAS_ID);
		String RELATION_IN_ALIAS = String.format("%s.%s", ALIASES, RELATION);
		
		QueryBuilder queryBuilder = new QueryBuilder();
		queryBuilder.or(
			QueryBuilder.start( UID ).is( userOrAliasId ).get(),
			QueryBuilder.start( ID_IN_ALIAS ).is( userOrAliasId ).and( RELATION_IN_ALIAS ).is( ALIAS ).get()	
		);
		DBObject query = queryBuilder.get();
		
		DBObject projection = new BasicDBObject();
		projection.put( ID, 0);
		projection.put( UID , 1);
		
		DBObject user = getImdbCollection().findOne(query, projection);
		return user == null ? null : ( String ) user.get(UID);
	}
	
	/**
	 * 
	 * find( 
	 * 		{
	 * 			instrument: "001565283736"
	 * 		}, 
	 * 		{ 
	 * 			_id:0, 
	 * 			contact:1
	 * 		}  
	 * ).sort( 
	 * 		{ 
	 * 			timestamp: -1  
	 * 		} 
	 * )[0]
	 * 
	 */
	public String getContactByPhoneId(String phoneId) {
		DBObject query = QueryBuilder.start("instrument").is(phoneId).and(QueryBuilder.start("expired").is(false).get()).get();
		DBObject projection = new BasicDBObject();
		projection.put(ID, 0);
		projection.put("contact", 1);
		
		DBCursor cursor = getRegistrarCollection().find(query, projection).sort(new BasicDBObject("timestamp", -1)).limit(1);
		if(!cursor.hasNext())
			return null;
		DBObject phone = cursor.next();
		String contact = (String) phone.get("contact");
		
		return contact;
	}
	
	public String getExtensionFromContact(String contact) {
		Matcher matcher = addressPattern.matcher(contact);
		boolean found = matcher.find();
		int groupCount = matcher.groupCount();
		if( found && groupCount == 2 ) {
			String extension = matcher.group(1);
			return extension;
		}
		return null;
	}
	
	public String getIpFromContact(String contact) {
		Matcher matcher = addressPattern.matcher(contact);
		boolean found = matcher.find();
		int groupCount = matcher.groupCount();
		if( found && groupCount == 2 ) {
			String extension = matcher.group(2);
			return extension;
		}
		return null;
	}
	

}
