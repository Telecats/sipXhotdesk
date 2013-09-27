// SYSTEM INCLUDES
#include "os/OsLock.h"
#include "os/OsConfigDb.h"
#include "os/OsLogger.h"
#include "os/OsFS.h"
#include "os/iostream"
#include "net/Url.h"
#include "os/linux/OsFileLinux.h"


// APPLICATION INCLUDES
//
#include <sipxproxy/RouteState.h>
#include <sipxproxy/SipRouter.h>
#include "Hotdesking.h"
#include "sipXecsService/SipXecsService.h"
#include <utl/UtlTokenizer.h>

// DEFINES
// CONSTANTS

const char* HD_FILENAME = "hotdesking.properties";
const char* hd_filename = "/usr/local/sipx/etc/sipxpbx/sipxhotdesking.properties";
const char* ENABLE_PARAM="ENABLE_HD_AUTH";
OsFileLinux hdConfig();

//NOT NEEDED?
//const char* ENABLE_TRUE="true";

// TYPEDEFS
OsMutex        Hotdesking::sSingletonLock(OsMutex::Q_FIFO);
Hotdesking*   Hotdesking::spInstance;

/// Factory used by PluginHooks to dynamically link the plugin instance
extern "C" AuthPlugin* getAuthPlugin(const UtlString& pluginName)
{
   OsLock singleton(Hotdesking::sSingletonLock);

   if (!Hotdesking::spInstance)
   {
         Hotdesking::spInstance = new Hotdesking(pluginName);
   }
   else
   {
      Os::Logger::instance().log(FAC_SIP, PRI_CRIT, "Hotdesking[%s]::it is invalid to configure more than one instance of the Hotdesking plugin.",
                    pluginName.data());
      assert(false);
   }

   return Hotdesking::spInstance;
}

/// constructor
Hotdesking::Hotdesking(const UtlString& pluginName ///< the name for this instance
                         )
   : AuthPlugin(pluginName),
     mpSipRouter( 0 )
{

};

/// Nothing configurable outside the database right now
void
Hotdesking::readConfig( OsConfigDb& configDb /**< a subhash of the individual configuration
                                               * parameters for this instance of this plugin. */
                             )
{
	/*
	 * @note
	 * The parent service may call the readConfig method at any time to
	 * indicate that the configuration may have changed.  The plugin
	 * should reinitialize itself based on the configuration that exists when
	 * this is called.  The fact that it is a subhash means that whatever prefix
	 * is used to identify the plugin (see PluginHooks) has been removed (see the
	 * examples in PluginHooks::readConfig).
	 */
	Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]::readConfig",
			mInstanceName.data()
	);

	bEnabled = configDb.getBoolean(ENABLE_PARAM,false);
	fileName = SipXecsService::Path(SipXecsService::ConfigurationDirType, HD_FILENAME);

	if(bEnabled)
	{
		Os::Logger::instance().log(FAC_SIP, PRI_CRIT, "Hotdesking: Hotdesking[%s] plugin is now enabled", mInstanceName.data());
	}
	else
	{
		Os::Logger::instance().log(FAC_SIP, PRI_CRIT, "Hotdesking: Hotdesking[%s] plugin is now disabled", mInstanceName.data());
	}


}


AuthPlugin::AuthResult
Hotdesking::authorizeAndModify(const UtlString& id,    /**< The authenticated identity of the
 *   request originator, if any (the null
 *   string if not).
 *   This is in the form of a SIP uri
 *   identity value as used in the
 *   credentials database (user@domain)
 *   without the scheme or any parameters.
 */
		const Url&  requestUri, ///< parsed target Uri
		RouteState& routeState, ///< the state for this request.
		const UtlString& method,///< the request method
		AuthResult  priorResult,///< results from earlier plugins.
		SipMessage& request,    ///< see AuthPlugin regarding modifying
		bool bSpiralingRequest, ///< spiraling indication
		UtlString&  reason      ///< rejection reason
)
{
	if(!bEnabled)
	{
		return AuthPlugin::CONTINUE;
	}

	/* Is this an INVITE request? */
	if (method.compareTo(SIP_INVITE_METHOD) == 0)
	{

		if (priorResult != DENY) // no point in modifying a request that won't be sent
		{
			if (   routeState.isMutable()
					&& routeState.directionIsCallerToCalled(mInstanceName.data())
			) // a new dialog?
			{
				// Define UtlStrings
				UtlString originalFromField, callingUser, originalToField, calledUser, logoffUser, allowedNumbers, allowedNumber, hdLine, hdExtension;
				UtlString prop_user = PROP_USER;
				UtlString prop_numbers = PROP_NUMBERS;
				UtlString prop_extension = PROP_EXTENSION;
				bool isAllowed = FALSE;

				// Get calling user
				request.getFromField(&originalFromField);
				Url originalFromUrl(originalFromField);
				originalFromUrl.getUserId(callingUser);

				// Get called user
				request.getToField(&originalToField);
				Url originalToUrl(originalToField);
				originalToUrl.getUserId(calledUser);

				OsFileLinux hdConfig(fileName);
                                if (hdConfig.open( 1 ) != OS_SUCCESS) {
					Os::Logger::instance().log(FAC_SIP, PRI_ERR, "Hotdesking[%s]::hotdesking auth plugin REJECTED CALL. CAUSE:configuration file not found: [%s], correct this error asap!!! (sendProfiles may help)", mInstanceName.data(), fileName.data());
					return AuthPlugin::DENY;
				}


				bool identityIsLocal = mpSipRouter->isLocalDomain(originalFromUrl);
				if (identityIsLocal) {

					Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]::Processing calling user '%s', called user '%s'", mInstanceName.data(), callingUser.data(), calledUser.data());

					// Parse properties file
					while (!hdConfig.isEOF()) {
						hdConfig.readLine(hdLine);
						Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]::Parsing .properties file, line: '%s'", mInstanceName.data(), hdLine.data());


						if (hdLine.index( prop_user ) != UTL_NOT_FOUND) { // Looking for logoff user
							int length = prop_user.length() + 1;
							Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: Found line '%s'. Found logoffUser '%s'", mInstanceName.data(), hdLine.data(), logoffUser.data());
							hdLine.remove(0, length);
							logoffUser = hdLine.strip();
						}
						else if (hdLine.index( prop_extension) != UTL_NOT_FOUND) {
							int loc;
							loc = hdLine.index("=");
							int length = loc + 1;
							hdLine.remove(0, length);
							hdExtension = hdLine.strip();
							Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: Found line '%s'. Hotdesking extension: '%s'", mInstanceName.data(), hdLine.data(), hdExtension.data());

							// Retrieve the length of the hotdesking extension, and use it to get the first <lenght> characters from the called extension.
							UtlString subUtl = calledUser(0, hdExtension.length());
							if ( subUtl.compareTo(hdExtension, UtlString::ignoreCase) == 0 ) {
								isAllowed = TRUE;
							}
						}
						else if (hdLine.index( prop_numbers ) != UTL_NOT_FOUND) { // Looking for allowed numbers
							int loc;
							loc = hdLine.index("=");
							int length = loc + 1;
							hdLine.remove(0, length);
							allowedNumbers = hdLine.strip();
							Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: Found line '%s'. Allowed number: '%s'", mInstanceName.data(), hdLine.data(), allowedNumbers.data());

							UtlTokenizer tokenizer(allowedNumbers);
							while( tokenizer.next( allowedNumber, " " ) ) {
								if( calledUser.compareTo( allowedNumber ) == 0 ) {
									isAllowed = TRUE;
								}
							}
							



							//if (calledUser.compareTo(allowedNumber) == 0) {
							//	isAllowed = TRUE;
							//}
						}
						else {
							Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: Dont need this line, continue", mInstanceName.data());
							continue;
						}
					} // End parse

					// Everything parsed. Now checking if this call is allowed.
					Os::Logger::instance().log( FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: XML Parsed, now check if this call is allowed.", mInstanceName.data());

					try {
						if (callingUser.compareTo(logoffUser) == 0) {
							Os::Logger::instance().log( FAC_SIP, PRI_DEBUG, "Hotdesking[%s]::parsing Hotdesking settings match. logoffUser: '%s' and callingUser '%s'", mInstanceName.data(), logoffUser.data(), callingUser.data());

							if (!isAllowed) {
								// Not allowed, send DENY
								Os::Logger::instance().log( FAC_SIP, PRI_WARNING, "Hotdesking[%s]:: logged off user is trying to call a non-allowed number, DENY!", mInstanceName.data());
								return AuthPlugin::DENY;
							}
							else {
								Os::Logger::instance().log( FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: logged off user is trying to call an allowed number, ALLOW!", mInstanceName.data());
							}
						} else {
							Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]::no match for logoff_user '%s' to callingUser '%s'", mInstanceName.data(), logoffUser.data() ,callingUser.data());
						}
					} catch(const char* errorMsg) {
						Os::Logger::instance().log(FAC_SIP, PRI_ERR, "Hotdesking[%s]::failed to match logoff_user '%s' to callingUser '%s'", mInstanceName.data(), logoffUser.data() ,callingUser.data());
					}

				} // END identityIsLocal
				if (hdConfig.close()) {
					Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]:: Closed file", mInstanceName.data());
				}
			}
		} else {
			Os::Logger::instance().log(FAC_SIP, PRI_DEBUG, "Hotdesking[%s]::authorizeAndModify not mutable, or not local domain - no rewrite", mInstanceName.data());
		}
	}
	return AuthPlugin::CONTINUE;
}

void Hotdesking::announceAssociatedSipRouter( SipRouter* sipRouter )
{
   mpSipRouter = sipRouter;
}

/// destructor
Hotdesking::~Hotdesking()
{

}
