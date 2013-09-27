package org.sipfoundry.hotdesking.mongo;

import static org.sipfoundry.commons.mongo.MongoConstants.PERMISSIONS;
import static org.sipfoundry.commons.mongo.MongoConstants.UID;
import static org.sipfoundry.commons.mongo.MongoConstants.VALID_USER;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sipfoundry.commons.mongo.MongoSpringTemplate;
import org.sipfoundry.commons.userdb.ValidUsersSpring;
import org.sipfoundry.hotdesking.HotdeskingUser;
import org.springframework.beans.factory.annotation.Required;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

public class HotdeskingPermissionManager {
	
	static final Logger LOG = Logger.getLogger(HotdeskingPermissionManager.class);
	
	private MongoSpringTemplate m_imdbSpringTemplate;
	private ValidUsersSpring m_validUsers;
	
	private String m_hotdeskingPermissionName;
	private String m_hotdeskingWithoutPincodePermissionName;
	private String m_hotdeskingAutoLogoffPermissionName;
	
	@Required
	public void setImdbSpringTemplate(MongoSpringTemplate imdbSpringTemplate) {
		this.m_imdbSpringTemplate = imdbSpringTemplate;
	}

	@Required
	public void setValidUsers(ValidUsersSpring validUsers) {
		this.m_validUsers = validUsers;
	}
	
	public ValidUsersSpring getValidUsers() {
		return m_validUsers;
	}
	
	private DBCollection getImdbCollection() {
		return m_imdbSpringTemplate.getDb().getCollection("entity");
	}

	public void setHotdeskingPermissionName(String hotdeskingPermissionName) {
		this.m_hotdeskingPermissionName = hotdeskingPermissionName;
	}

	public void setHotdeskingWithoutPincodePermissionName(
			String hotdeskingWithoutPincodePermissionName) {
		this.m_hotdeskingWithoutPincodePermissionName = hotdeskingWithoutPincodePermissionName;
	}

	public void setHotdeskingAutoLogoffPermissionName(
			String hotdeskingAutoLogoffPermissionName) {
		this.m_hotdeskingAutoLogoffPermissionName = hotdeskingAutoLogoffPermissionName;
	}
	
	public List<HotdeskingUser> getAutoLogoffHotdeskingUsers() {
		return getHotdeskingUsers(m_hotdeskingPermissionName, m_hotdeskingAutoLogoffPermissionName);
	}
	
	//db.entity.find({ $and: [ {vld:true}, {prm:"hotdesking"}, {prm:"hotdeskingwithoutpincode"}]})
	public List<HotdeskingUser> getHotdeskingUsers(String... perms) {
		
		DBObject[] ands = new DBObject[perms.length + 1];
		ands[0] = QueryBuilder.start(VALID_USER).is(Boolean.TRUE).get();
		
		for(int i = 1; i <= perms.length; i++) {
			ands[i] = QueryBuilder.start(PERMISSIONS).is(perms[i-1]).get();
		}
		
		QueryBuilder qbPermissions = new QueryBuilder();
		DBObject query = qbPermissions.and(ands).get();
		//DBObject query = qbPermissions.and(QueryBuilder.start(VALID_USER).is(Boolean.TRUE).get(), QueryBuilder.start(PERMISSIONS).is("perm_1").get(), QueryBuilder.start(PERMISSIONS).is("perm_3").get()).get();
		//DBObject permissionQuery = QueryBuilder.start(PERMISSIONS).is("perm_1").and(QueryBuilder.start(PERMISSIONS).is("perm_2").get()).get();
//		qbPermissions.is("perm_1").and("perm_2");
//		for(String perm: perms) {
//			qbPermissions.is(perm);
//		}
//		try {
//			DBObject permissionQuery = qbPermissions.get();
//		} catch(Exception ex) {
//			LOG.error(ex);
//		}
		
//		DBObject query = QueryBuilder.start(VALID_USER).is(Boolean.TRUE).and(
//				qbPermissions.get()
//		).get();
		DBCursor userObjects = getImdbCollection().find(query);
		
		List<HotdeskingUser> hotdeskingUsers = new ArrayList<HotdeskingUser>();
		HotdeskingUser hotdeskingUser;
		while(userObjects.hasNext()) {
			hotdeskingUser = new HotdeskingUser();
			DBObject userObject = userObjects.next();
			loadImdbInfo(userObject, hotdeskingUser);
			loadHotdeskingPermissions(userObject, hotdeskingUser);
			hotdeskingUsers.add(hotdeskingUser);
		}
		
		return hotdeskingUsers;
	}
	
	public HotdeskingUser getHotdeskingUser(String userId) throws UserNotFoundException {
		HotdeskingUser hotdeskingUser = new HotdeskingUser();
		hotdeskingUser.setUserId(userId);
		
		DBObject userObject = getUserObject(userId);
		
		loadImdbInfo(userObject, hotdeskingUser);
		loadHotdeskingPermissions(userObject, hotdeskingUser);
		
		return hotdeskingUser;
	}
	
	public void loadImdbInfo(DBObject userObject, HotdeskingUser hotdeskingUser) {
		Object userId = userObject.get("uid");
		Object realm = userObject.get("rlm");
		Object pincodeHash = userObject.get("pntk");
		
		hotdeskingUser.setUserId((String)userId);
		hotdeskingUser.setRealm((String)realm);
		hotdeskingUser.setUserPincode((String)pincodeHash);
	}
	
	/**
	 * 
	 * @param userId with or without realm (200 or 200@realm.xx)
	 * @return
	 * @throws UserNotFoundException
	 */
	public DBObject getUserObject(String userId) throws UserNotFoundException {
		userId = userId.split("@")[0];
		DBObject query = QueryBuilder.start(VALID_USER).is(Boolean.TRUE).and(UID).is(userId).get();
		DBObject userObject = getImdbCollection().findOne(query);

		if(userObject == null)
			throw new UserNotFoundException(userId);
		
		return userObject;
	}
	
//	public boolean hasHotdeskingPermission(String userId) throws UserNotFoundException {
//		HotdeskingUser hotdeskingUser = new HotdeskingUser();
//		hotdeskingUser.setUserId(userId);
//		loadHotdeskingPermissions(hotdeskingUser);
//		return hotdeskingUser.isHotdeskingPermission();
//	}
	
	public void loadHotdeskingPermissions(DBObject userObject, HotdeskingUser hotdeskingUser) {
		BasicDBList permissions = (BasicDBList) userObject.get(PERMISSIONS);
		if (permissions != null) {
			hotdeskingUser.setHotdeskingPermission(permissions.contains(m_hotdeskingPermissionName));
			hotdeskingUser.setHotdeskingWithoutPincodePermission(permissions.contains(m_hotdeskingWithoutPincodePermissionName));
			hotdeskingUser.setAutoLogoffPermission(permissions.contains(m_hotdeskingAutoLogoffPermissionName));
        }
	}
    
    public class UserNotFoundException extends Exception {
    	public UserNotFoundException(String userId) {
    		super(String.format("User %s not found in mongo imdb table", userId));
    	}
    }

}
