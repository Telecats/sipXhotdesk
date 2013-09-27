package org.sipfoundry.sipxconfig.sipxhotdesking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.provisioning.hot.HotProvisionable;
import org.sipfoundry.provisioning.hot.HotProvisioningManager;
import org.sipfoundry.provisioning.taskfile.HotdeskingApplicationEvent;
import org.sipfoundry.provisioning.taskfile.HotdeskingFreeswitchExtensionProvider;
import org.sipfoundry.provisioning.taskfile.HotdeskingRule;
import org.sipfoundry.sipxconfig.address.Address;
import org.sipfoundry.sipxconfig.address.AddressManager;
import org.sipfoundry.sipxconfig.cfgmgt.ConfigManager;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.commserver.Location;
import org.sipfoundry.sipxconfig.commserver.LocationsManager;
import org.sipfoundry.sipxconfig.commserver.SipxReplicationContext;
import org.sipfoundry.sipxconfig.commserver.imdb.DataSet;
import org.sipfoundry.sipxconfig.commserver.imdb.RegistrationItem;
import org.sipfoundry.sipxconfig.commserver.imdb.ReplicationManager;
import org.sipfoundry.sipxconfig.device.ProfileManager;
import org.sipfoundry.sipxconfig.device.RestartManager;
import org.sipfoundry.sipxconfig.dialplan.DialPlanContext;
import org.sipfoundry.sipxconfig.dialplan.DialingRule;
import org.sipfoundry.sipxconfig.domain.DomainManager;
import org.sipfoundry.sipxconfig.feature.Bundle;
import org.sipfoundry.sipxconfig.feature.FeatureChangeRequest;
import org.sipfoundry.sipxconfig.feature.FeatureChangeValidator;
import org.sipfoundry.sipxconfig.feature.FeatureManager;
import org.sipfoundry.sipxconfig.feature.FeatureProvider;
import org.sipfoundry.sipxconfig.feature.GlobalFeature;
import org.sipfoundry.sipxconfig.feature.LocationFeature;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchAction;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchCondition;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchExtension;
import org.sipfoundry.sipxconfig.freeswitch.FreeswitchFeature;
import org.sipfoundry.sipxconfig.localization.LocalizationContext;
import org.sipfoundry.sipxconfig.permission.Permission;
import org.sipfoundry.sipxconfig.permission.PermissionManager;
import org.sipfoundry.sipxconfig.permission.PermissionName;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.phone.PhoneContext;
import org.sipfoundry.sipxconfig.registrar.Registrar;
import org.sipfoundry.sipxconfig.registrar.RegistrationContext;
import org.sipfoundry.sipxconfig.setting.BeanWithSettingsDao;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.setting.SettingDao;
import org.sipfoundry.sipxconfig.setup.SetupListener;
import org.sipfoundry.sipxconfig.setup.SetupManager;
import org.sipfoundry.sipxconfig.snmp.ProcessDefinition;
import org.sipfoundry.sipxconfig.snmp.ProcessProvider;
import org.sipfoundry.sipxconfig.snmp.SnmpManager;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class SipXHotdeskingImpl implements SipXHotdesking, FeatureProvider,
		ProcessProvider, SetupListener, ApplicationListener<ApplicationEvent>,
		HotdeskingFreeswitchExtensionProvider {

	// TODO: Cleanup unused managers
	private FeatureManager m_featureManager;
	private BeanWithSettingsDao<SipXHotdeskingSettings> m_settingsDao;
	private ListableBeanFactory m_beanFactory;
	private CoreContext m_coreContext;
	private ReplicationManager m_replicationManager;
	private ConfigManager m_configManager;
	private SettingDao m_settingDao;
	private DomainManager m_domainManager;
	private LocationsManager m_locationsManager;
	private SipxReplicationContext m_sipxReplicationContext;
	private PermissionManager m_permissionManager;
	private AddressManager m_addressManager;
	private LocalizationContext m_localizationContext;
	private PhoneContext m_phoneContext;
	private RegistrationContext m_registrationContext;
	private ProfileManager m_profileManager;
	private RestartManager m_restartManager;
	private HotProvisioningManager m_hotProvisioningManager;

	private static final Log LOG = LogFactory.getLog(SipXHotdeskingImpl.class);

	private static final Pattern EXTRACT_USER_RE = Pattern
			.compile("\\s*<?(?:sip:)?(.+?)[@;](.+)[:].+");

	private static final Pattern EXTRACT_FULL_USER_RE = Pattern
			.compile("\\s*(?:\"?\\s*([^\"<]+?)\\s*\"?)?\\s*<(?:sip:)?(.+?)[@;](.+)[:].+");

	/**
	 * Logout user's id
	 */
	public static final String LOGOFF_USER = "hotdesking.logoffUser";

	/**
	 * Calling to hotdesking user
	 */
	public static final String CALLING_USER = "callingUser";
	/**
	 * Switch to
	 */
	public static final String NEW_USER = "newUser";
	/**
	 * Calling phone serial
	 */
	public static final String CALLING_PHONE_SERIAL = "phoneSerial";
	/**
	 * Just logout on calling phone
	 */
	public static final String LOGOUT_ON_THIS_PHONE = "logoutOnThisPhone";
	/**
	 * Logout everywhere (on all phones).
	 */
	public static final String LOGOUT_EVERYWHERE = "logoutEverywhere";
	/**
	 * Logout on all other phones and login only on calling phone
	 */
	public static final String HOTDESKING_LOGOUT_EVERYWHERE = "hotdeskingWithEverywhereLogout";
	/**
	 * Login on calling phone. Other phones with given user will stay online.
	 */
	public static final String HOTDESKING_WITHOUT_LOGOUT = "hotdeskingWithoutLogout";

	private static final String SIP_CONTACT_HOST = "sipContactHost";

	private static final String HOTDESKING_ENABLE_AUTO_LOGOFF_OTHER_PHONES = "hotdesking.enable_auto_logoff_other_phones";

	@Override
	public Collection<GlobalFeature> getAvailableGlobalFeatures(
			FeatureManager featureManager) {
		return null;
	}

	@Override
	public Collection<LocationFeature> getAvailableLocationFeatures(
			FeatureManager featureManager, Location l) {
		return Collections.singleton(FEATURE);
	}

	public SipXHotdeskingSettings getSettings() {
		return m_settingsDao.findOrCreateOne();
	}

	public void saveSettings(SipXHotdeskingSettings settings) {
		m_settingsDao.upsert(settings);
	}

	public void setFeatureManager(FeatureManager featureManager) {
		m_featureManager = featureManager;
	}

	public void setSettingsDao(
			BeanWithSettingsDao<SipXHotdeskingSettings> settingsDao) {
		m_settingsDao = settingsDao;
	}

	@Override
	public Collection<ProcessDefinition> getProcessDefinitions(
			SnmpManager manager, Location location) {
		boolean enabled = manager.getFeatureManager().isFeatureEnabled(FEATURE,
				location);
		return (enabled ? Collections.singleton(ProcessDefinition.sysvByRegex(
				"hotdesking", ".*\\s-Dprocname=hotdesking\\s.*")) : null);
	}

	public void setConfigManager(ConfigManager configManager) {
		m_configManager = configManager;
	}

	@Override
	public void getBundleFeatures(FeatureManager featureManager, Bundle b) {
		if (b == Bundle.PROVISION) {
			b.addFeature(FEATURE);
		}
	}

	@Override
	public void featureChangePrecommit(FeatureManager manager,
			FeatureChangeValidator validator) {
		validator.requiredOnSameHost(FEATURE, Registrar.FEATURE);
		validator.primaryLocationOnly(FEATURE);
	}

	@Override
	public void featureChangePostcommit(FeatureManager manager,
			FeatureChangeRequest request) {
		if (request.getAllNewlyEnabledFeatures().contains(
				SipXHotdesking.FEATURE)) {
			SipXHotdeskingSettings settings = getSettings();
			if (settings.isNew()) {
				saveSettings(settings);
			}
			// m_sipxReplicationContext.generateAll(DataSet.ALIAS);
			// m_sipxReplicationContext.generateAll(DataSet.CREDENTIAL);
			m_sipxReplicationContext.generateAll(DataSet.PERMISSION);
		}

		// Make sure other hosts get configured as well
		if (request.hasChanged(SipXHotdesking.FEATURE)) {
			m_configManager.configureEverywhere(Registrar.FEATURE);
			m_configManager.configureEverywhere(DialPlanContext.FEATURE);
		}
	}

	@Required
	public void setAddressManager(AddressManager addressManager) {
		m_addressManager = addressManager;
	}

	@Required
	public void setLocalizationContext(LocalizationContext localizationContext) {
		m_localizationContext = localizationContext;
	}

	@Required
	public void setRegistrationContext(RegistrationContext registrationContext) {
		this.m_registrationContext = registrationContext;
	}

	// @Override
	// public void setBeanFactory(BeanFactory beanFactory) {
	// m_beanFactory = (ListableBeanFactory) beanFactory;
	// }

	@Required
	public void setPhoneContext(PhoneContext phoneContext) {
		this.m_phoneContext = phoneContext;
	}

	@Required
	public void setProfileManager(ProfileManager profileManager) {
		this.m_profileManager = profileManager;
	}

	@Required
	public void setRestartManager(RestartManager restartManager) {
		this.m_restartManager = restartManager;
	}

	@Required
	public void setHotProvisioningManager(
			HotProvisioningManager hotProvisioningManager) {
		this.m_hotProvisioningManager = hotProvisioningManager;
	}

	public void setCoreContext(CoreContext coreContext) {
		m_coreContext = coreContext;
	}

	public void setDomainManager(DomainManager domainManager) {
		m_domainManager = domainManager;
	}

	public void setReplicationManager(ReplicationManager replicationManager) {
		m_replicationManager = replicationManager;
	}

	public void setPermissionManager(PermissionManager permissionManager) {
		this.m_permissionManager = permissionManager;
	}

	@Required
	public void setSipxReplicationContext(
			SipxReplicationContext replicationContext) {
		m_sipxReplicationContext = replicationContext;
	}

	public void setLocationsManager(LocationsManager locationsManager) {
		m_locationsManager = locationsManager;
	}

	@Override
	public boolean setup(SetupManager manager) {
	
		if( !hasHDUser() ) {
			createHotdeskingUser();
		}
		
		return checkOrCreate("hotdesking", "Allow Hotdesking", false) != null
				&& checkOrCreate("hotdesking_without_pincode",
						"Allow Hotdesking without pincode", false) != null
				&& checkOrCreate("hotdesking_auto_logoff",
						"Auto logoff hotdesking user", false) != null;
	}

	private boolean hasHDUser() {
		User hotdeskingUser = m_coreContext
				.loadUserByUserName(SipXHotdeskingSettings.HOTDESKING_USER_NAME);
		return hotdeskingUser != null;
	}

	private void createHotdeskingUser() {
		User specialUser = m_coreContext.newUser();
		specialUser.setName(SipXHotdeskingSettings.HOTDESKING_USER_NAME);
		String hotdeskingPrefix = this.getSettings().getSettingValue(
				SipXHotdeskingSettings.SETTING_HOTDESKING_EXTENSION);
		specialUser.setFirstName(hotdeskingPrefix + " to login");
		specialUser.setUserName(specialUser.getUserName());
		specialUser.setPin("1234");
		specialUser.setSipPassword(RandomStringUtils.randomAlphanumeric(10));
		// specialUser.setSettingTypedValue(PermissionName.HOTDESKING.getPath(),
		// true);
		specialUser.setSettingTypedValue(
				PermissionName.AUTO_ATTENDANT_DIALING.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.EXCHANGE_VOICEMAIL.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.FREESWITH_VOICEMAIL.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.INTERNATIONAL_DIALING.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.LOCAL_DIALING.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.LONG_DISTANCE_DIALING.getPath(), false);
		specialUser
				.setSettingTypedValue(PermissionName.MOBILE.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.MUSIC_ON_HOLD.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.NINEHUNDERED_DIALING.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.PERSONAL_AUTO_ATTENDANT.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.RECORD_SYSTEM_PROMPTS.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.SUBSCRIBE_TO_PRESENCE.getPath(), false);
		specialUser.setSettingTypedValue(PermissionName.SUPERADMIN.getPath(),
				false);
		specialUser.setSettingTypedValue(
				PermissionName.TOLL_FREE_DIALING.getPath(), false);
		specialUser.setSettingTypedValue(
				PermissionName.TUI_CHANGE_PIN.getPath(), false);
		specialUser.setSettingTypedValue(PermissionName.VOICEMAIL.getPath(),
				false);

		Permission hotdesking = checkOrCreate("hotdesking", "Allow Hotdesking",
				false);
		specialUser.setPermission(hotdesking, true);
		m_coreContext.saveUser(specialUser);
	}

	private Permission checkOrCreate(String label, String description,
			boolean defaultValue) {
		Permission permission = m_permissionManager.getPermissionByLabel(label);
		if (permission == null) {
			permission = new Permission();
			permission.setType(Permission.Type.CALL);
			permission.setLabel(label);
			permission.setDescription(description);
			permission.setDefaultValue(defaultValue);
			m_permissionManager.saveCallPermission(permission);
		}
		return permission;
	}

	@Override
	public List<? extends DialingRule> getDialingRules(Location location) {
		Address freeswitchAddress = m_addressManager
				.getSingleAddress(FreeswitchFeature.SIP_ADDRESS);
		if (freeswitchAddress == null)
			return Collections.EMPTY_LIST;
		String extension = getSettings().getSettingValue(
				SipXHotdeskingSettings.SETTING_HOTDESKING_EXTENSION);
		String language = m_localizationContext.getCurrentLanguage();
		String hotdeskingPermissionName = m_permissionManager
				.getPermissionByLabel("hotdesking").getName();

		DialingRule rule = new HotdeskingRule(
				freeswitchAddress.addressColonPort(), extension, language,
				hotdeskingPermissionName);

		return Collections.singletonList(rule);
	}

	public boolean isEnabled() {
		return m_featureManager.isFeatureEnabled(FEATURE);
	}

	@Override
	public List<? extends FreeswitchExtension> getFreeswitchExtensions() {
		if (!isEnabled()) {
			return new ArrayList<FreeswitchExtension>();
		}

		final List<FreeswitchExtension> exts = new ArrayList<FreeswitchExtension>();
		final FreeswitchExtension ext = new FreeswitchExtension() {
			@Override
			protected Setting loadSettings() {
				return null;
			}
		};
		ext.setName("HTD");
		exts.add(ext);

		final Set<FreeswitchCondition> conditions = new HashSet<FreeswitchCondition>();
		final FreeswitchCondition hotdeskingCondition = new FreeswitchCondition();
		hotdeskingCondition.setField("destination_number");
		hotdeskingCondition.setExpression("^HTD$");
		conditions.add(hotdeskingCondition);
		ext.setConditions(conditions);

		final Set<FreeswitchAction> actions = new HashSet<FreeswitchAction>();
		final FreeswitchAction hotdeskingAction = new FreeswitchAction();
		actions.add(hotdeskingAction);
		hotdeskingCondition.setActions(actions);

		hotdeskingAction.setApplication("socket");
		List<Location> addresses = m_featureManager
				.getLocationsForEnabledFeature(FreeswitchFeature.FEATURE);
		if (addresses.isEmpty()) {
			LOG.error("Couldn't determine address of the server, needed to initialize the HotdeskingFreeswitchExtension.");
			return null;
		}
		String address = addresses.get(0).getAddress();
		String port = getSettings().getSettingValue(
				SipXHotdeskingSettings.SETTING_FREESWITCH_PORT);
		hotdeskingAction.setData(address + ":" + port + " async full");
		return exts;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof HotdeskingApplicationEvent) {
			final HotdeskingApplicationEvent ev = (HotdeskingApplicationEvent) event;
			final Properties props = (Properties) ev.getSource();
			if (props == null) {
				LOG.info("Hotdesking received ApplicationInitializedEvent, but properties is null");
				return;
			} else {
				// Manage phones
				LOG.info("Hotdesking received ApplicationInitializedEvent, properties:"
						+ props);
				managePhones(props);
			}
		}

	}

	/**
	 * Manage phones
	 * 
	 * @param props
	 */
	private void managePhones(Properties props) {
		LOG.debug("Hotdesking: staring phone managment by given task's property file: "
				+ props.toString());
		// Resolve given properties
		final String userId = props.getProperty(CALLING_USER);
		final String newUserIdstr = props.getProperty(NEW_USER);
		final String callingPhoneSerial = props
				.getProperty(CALLING_PHONE_SERIAL);
		final String logoffUserId = props.getProperty(LOGOFF_USER);
		final String autoLogoffString = props
				.getProperty(HOTDESKING_ENABLE_AUTO_LOGOFF_OTHER_PHONES);
		final String sipContactHost = props.getProperty(SIP_CONTACT_HOST);

		final int indexOf = userId.indexOf("@");
		String callingUserId = userId;
		if (indexOf > 0) {
			LOG.debug("Hotedesking: looks like given userId contains domain with '@'. Use only digits.");
			callingUserId = userId.substring(0, indexOf);
			LOG.debug("Hotedesking: calling callingUserId=" + callingUserId);
		}

		if (logoffUserId == null || StringUtils.isEmpty(logoffUserId)) {
			LOG.warn("Hotedesking: No logoffUser configured");
		}

		// debug output
		LOG.debug("Hotdesking: userId                            : " + userId);
		LOG.debug("Hotdesking: callingUserId                     : "
				+ callingUserId);
		LOG.debug("Hotdesking: newUserIdstr                      : "
				+ newUserIdstr);
		LOG.debug("Hotdesking: callingPhoneSerial                : "
				+ callingPhoneSerial);
		LOG.debug("Hotdesking: logoffUserId                      : "
				+ logoffUserId);
		LOG.debug("Hotdesking: autoLogoffString                  : "
				+ autoLogoffString);
		LOG.debug("Hotdesking: sipContactHost                    : "
				+ sipContactHost);

		boolean autoLogoff = Boolean.parseBoolean(autoLogoffString);

		boolean logoutOnThisPhone = "true".equalsIgnoreCase(props
				.getProperty(LOGOUT_ON_THIS_PHONE));
		boolean logoutWherewhere = "true".equalsIgnoreCase(props
				.getProperty(LOGOUT_EVERYWHERE));
		boolean hotdeskingWhithoutLogout = "true".equalsIgnoreCase(props
				.getProperty(HOTDESKING_WITHOUT_LOGOUT));
		boolean hotdeskingLogoutWherewhere = "true".equalsIgnoreCase(props
				.getProperty(HOTDESKING_LOGOUT_EVERYWHERE));

		// autoLogoff
		if (autoLogoff) {
			if (logoutOnThisPhone) {
				logoutOnThisPhone = false;
				logoutWherewhere = true;
			} else if (hotdeskingWhithoutLogout) {
				hotdeskingWhithoutLogout = false;
				hotdeskingLogoutWherewhere = true;
			}
		}

		// debug output
		LOG.debug("Hotdesking: autoLogoff                 : " + autoLogoff);
		LOG.debug("Hotdesking: logoutOnThisPhone          : "
				+ logoutOnThisPhone);
		LOG.debug("Hotdesking: logoutWherewhere           : "
				+ logoutWherewhere);
		LOG.debug("Hotdesking: hotdeskingLogoutWherewhere : "
				+ hotdeskingLogoutWherewhere);
		LOG.debug("Hotdesking: hotdeskingWhithoutLogout   : "
				+ hotdeskingWhithoutLogout);

		// find users
		User callingUser = null;
		User newUser = null;
		User logoffUser = null;

		try {
			callingUser = m_coreContext.loadUserByUserName(callingUserId);
			if (newUserIdstr != null) {
				newUser = m_coreContext.loadUserByUserName(newUserIdstr);
			}
			logoffUser = m_coreContext
					.loadUserByUserName(SipXHotdeskingSettings.HOTDESKING_USER_NAME);
		} catch (Exception e) {
			LOG.error("Hotedesking: one of given userId is not exists: "
					+ callingUserId + "; " + newUserIdstr + ";" + logoffUserId,
					e);
			return;
		}

		final Integer callingId = callingUser.getId();
		Integer newUserId = -1;
		if (newUser != null) {
			newUserId = newUser.getId();
		}
		Phone callingPhone = null;
		Integer phoneIdBySerialNumber = null;
		LOG.debug("loading phone device for serial:" + callingPhoneSerial);
		try {
			phoneIdBySerialNumber = m_phoneContext
					.getPhoneIdBySerialNumber(callingPhoneSerial);
			LOG.debug("found phoneId:" + phoneIdBySerialNumber);
			callingPhone = m_phoneContext.loadPhone(phoneIdBySerialNumber);
		} catch (Exception e) {
			if (phoneIdBySerialNumber == null) {
				LOG.error("Hotedesking: Lookuping phone by serial ("
						+ callingPhoneSerial + ") fail: " + e.getMessage(), e);
			} else if (callingPhone == null) {
				LOG.error("Error during constructing phone device by serial ("
						+ callingPhoneSerial + "):" + e.getMessage(), e);
			} else {
				LOG.error(
						"Unkonown error when loading phone device by serial ("
								+ callingPhoneSerial + "):" + e.getMessage(), e);
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("=================================");
				LOG.debug("Known registrations:");
				LOG.debug("---------------------------------");
				List<RegistrationItem> registrations = m_registrationContext
						.getRegistrations();
				for (RegistrationItem reg : registrations) {
					LOG.debug("- '" + reg.getInstrument() + "'");
				}
				LOG.debug("=================================");
				LOG.debug("");
				LOG.debug("=================================");
				LOG.debug("Known phones:");
				LOG.debug("---------------------------------");
				List<Phone> phones = m_phoneContext.loadPhones();
				for (Phone phone : phones) {
					LOG.debug(phone.getId() + " - " + phone.getSerialNumber()
							+ " - " + phone.getModelLabel());
				}
				LOG.debug("=================================");

			}
			return;
		}

		// debug output
		LOG.debug("Hotdesking: callingUser                : " + callingUser);
		LOG.debug("Hotdesking: newUser                    : " + newUser);
		LOG.debug("Hotdesking: logoffUser                 : " + logoffUser);
		LOG.debug("Hotdesking: callingPhone               : " + callingPhone);

		// Do management
		if (logoutOnThisPhone) {
			LOG.debug("Hotdesking: requested logout");
			// we need the logoutUser
			logout(callingPhone, callingId, logoffUser, sipContactHost,
					callingUser);

		} else if (logoutWherewhere) {
			LOG.debug("Hotdesking: requested logout on all phones");
			// explicite logoff callingPhone (this will logoff the calling phone
			// also if it's
			// accounts isn't in sync
			// with sipxecs phone account)
			logout(callingPhone, callingId, logoffUser, sipContactHost,
					callingUser);
			logoutEverywhere(callingUser, callingId, null, logoffUser);

		} else if (hotdeskingLogoutWherewhere) {
			LOG.debug("Hotdesking: requested login with new user and logout on other phones");
			setNewUserForPhone(callingPhone, callingId, newUser,
					sipContactHost, callingUser, false);
			logoutEverywhere(newUser, newUserId, callingPhone.getId(),
					logoffUser);

		} else if (hotdeskingWhithoutLogout) {
			LOG.debug("Hotdesking: requested re-login");
			setNewUserForPhone(callingPhone, callingId, newUser,
					sipContactHost, callingUser, false);

		} else {
			LOG.warn("Hotdesking: nothing todo");
		}
	}

	/**
	 * Logout on all phones
	 * 
	 * @param callingUserId
	 */
	private void logoutEverywhere(User callingUser, int callingUserId,
			Integer excludePhone, User logoffUser) {
		// get user phones
		LOG.debug("Hotdesking::logoutEverywhere get user phones (callingUserId):"
				+ callingUserId);
		Collection<Phone> phonesByUserId = null;
		try {
			phonesByUserId = m_phoneContext.getPhonesByUserId(callingUserId);
		} catch (Exception e) {
			LOG.error(
					"Hotedesking: Lookuping phone by calling ID fail: "
							+ e.getMessage(), e);
			return;
		}

		// get phone addresses
		LOG.debug("Hotdesking::logoutEverywhere get phone addresses (callingUser):"
				+ (callingUser != null ? callingUser.getUserName()
						: callingUser));
		HashMap<String, String> phoneAddressBySerial = new HashMap<String, String>();
		LOG.debug("Hotdesking::logoutEverywhere registrationContext:"
				+ m_registrationContext);
		List<RegistrationItem> regsOfThisUser = m_registrationContext
				.getRegistrationsByUser(callingUser);
		for (RegistrationItem ri : regsOfThisUser) {
			phoneAddressBySerial.put(ri.getInstrument().toUpperCase(),
					extractAddress(ri.getContact()));
		}
		LOG.debug("Hotdesking: phoneAddressBySerial:" + phoneAddressBySerial);

		// logoff user phones
		LOG.debug("Hotdesking::logoutEverywhere  logoff user phones (phonesByUserId count):"
				+ phonesByUserId.size());
		final Iterator<Phone> iterator = phonesByUserId.iterator();
		while (iterator.hasNext()) {
			final Phone phone = iterator.next();
			final Integer id = phone.getId();
			if (excludePhone == null || !id.equals(excludePhone)) {
				logout(phone, callingUserId, logoffUser,
						phoneAddressBySerial.get(phone.getSerialNumber()
								.toUpperCase()), callingUser);
			}
		}
	}

	/**
	 * Set new user for given phone
	 * 
	 * @param phone
	 * @param callingId
	 * @param newUser
	 */
	private void setNewUserForPhone(Phone phone, Integer callingId,
			User newUser, String sipContactHost, User callingUser,
			boolean logout) {
		final List<Line> lines = phone.getLines();
		LOG.debug("Hotdesking: newUser:" + newUser.getName());
		LOG.debug("Hotdesking: setNewUserForPhone phone, newUser, callingId : "
				+ phone.getSerialNumber() + ", " + newUser.getName() + "("
				+ newUser.getId() + "), " + callingId);
		final Integer phoneId = phone.getId();
		Date nowTS = new Date();

		if (lines == null || lines.isEmpty()) {
			LOG.debug("Hotdesking: there are no any line assigned to given phone");
			return;
		}

		// update line of user
		final Iterator<Line> linesIterator = lines.iterator();
		boolean userFoundOnLine = false;
		while (linesIterator.hasNext()) {
			final Line line = linesIterator.next();
			final User user = line.getUser();
			LOG.debug("Hotdesking: found line -> userId : " + user.getId());
			userFoundOnLine = true;
			updateLine(phone, newUser, sipContactHost, callingUser, logout,
					phoneId, nowTS, line);
		}

		// fallback is because of some glitch user isn't on any line
		if (!userFoundOnLine && lines.size() > 0) {
			Line firstLine = lines.get(0);
			LOG.warn("Hotdesking: CallingUser not found on any of the phone's lines. Using fallback, apply hotdesking to first line. CallingUser is:"
					+ (callingUser != null ? callingUser.getUserName() : "null")
					+ ", user on first line:" + firstLine.getUserName());

			updateLine(phone, newUser, sipContactHost, callingUser, logout,
					phoneId, nowTS, firstLine);

		} else if (!userFoundOnLine) {
			LOG.warn("Hotdesking: CallingUser not found on any of the phone's lines. Hotdesking failed. CallingUser:"
					+ (callingUser != null ? callingUser.getUserName() : "null"));
		}
	}

	private void updateLine(Phone phone, User newUser, String sipContactHost,
			User callingUser, boolean logout, final Integer phoneId,
			Date nowTS, final Line line) {
		LOG.debug("Hotdesking: updating line by new given user: lineId="
				+ line.getId());
		// set new user to line
		line.setUser(newUser);

		// store line
		m_phoneContext.storeLine(line);
		LOG.debug("Hotdesking: generate new profile for phoneId=" + phoneId);

		// write new profiles
		m_profileManager.generateProfile(phoneId, false, nowTS);

		// provision (restart phone if xml provisioning isn't supported)
		// this only is needed when sipContextHost is known. If sipContextHost
		// isn't set, phone is
		// probably offline and doesn't need to be provisioned
		if (!StringUtils.isEmpty(sipContactHost)) {
			boolean hotProvisionable = (phone instanceof HotProvisionable);
			if (hotProvisionable) {
				LOG.info("Hotdesking: SendXmlProvisionNotify");
				HashMap<String, String> hotProvProps = new HashMap<String, String>();
				hotProvProps.put(HotProvisioningManager.SIP_CONTACT_HOST_PROP,
						sipContactHost);
				m_hotProvisioningManager.hotProvision(phoneId, nowTS,
						hotProvProps);
			} else {
				LOG.info("Hotdesking: Restarting phone");
				m_restartManager.restart(phoneId, nowTS);
			}
		} else {
			LOG.debug("Hotdesking: Skipping provisioning, no contact host for (phoneId):"
					+ phone.getId());
		}
	}

	/**
	 * Logout on single phone
	 * 
	 * @param phone
	 * @param callingId
	 */
	private void logout(Phone phone, Integer callingId, User logoutUser,
			String sipContactHost, User callingUser) {
		setNewUserForPhone(phone, callingId, logoutUser, sipContactHost,
				callingUser, true);
	}

	/**
	 * Extracts user name if available. Otherwise it returns the user id SipUri
	 * class should be used for this
	 */
	private String extractAddress(String uri) {
		if (uri == null) {
			return null;
		}
		Matcher matcher = EXTRACT_FULL_USER_RE.matcher(uri);
		if (!matcher.matches()) {
			matcher = EXTRACT_USER_RE.matcher(uri);
			if (matcher.matches()) {
				return matcher.group(2);
			}
			return null;
		} else {
			return matcher.group(3);
		}
	}
}
