#ifndef _HOTDESKING_H_
#define _HOTDESKING_H_

// SYSTEM INCLUDES
#include "os/OsMutex.h"

// APPLICATION INCLUDES
#include <sipxproxy/AuthPlugin.h>

// DEFINES
#define PROP_USER				"hotdesking.logoffUser"
#define PROP_NUMBERS			"hotdesking.allowedNumbers"
#define PROP_EXTENSION			"hotdesking.extension"

// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS



extern "C" AuthPlugin* getAuthPlugin(const UtlString& name);


class Hotdesking : public AuthPlugin
{
  public:

   virtual ~Hotdesking();

   /// Read (or re-read) the authorization rules.
   virtual void readConfig( OsConfigDb& configDb /**< a subhash of the individual configuration
                                                  * parameters for this instance of this plugin. */
                           );

   /**<
    * @note
    * The parent service may call the readConfig method at any time to
    * indicate that the configuration may have changed.  The plugin
    * should reinitialize itself based on the configuration that exists when
    * this is called.  The fact that it is a subhash means that whatever prefix
    * is used to identify the plugin (see PluginHooks) has been removed (see the
    * examples in PluginHooks::readConfig).
    */

   /// Called for any request - provides caller alias facility.
   virtual
      AuthResult authorizeAndModify(const UtlString& id,    /**< The authenticated identity of the
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
                                    bool bSpiralingRequest, ///< request spiraling indication
                                    UtlString&  reason      ///< rejection reason
                                    );
   ///< See class description.

   virtual void announceAssociatedSipRouter( SipRouter* sipRouter );



  protected:
   friend class SipRouterTest;

   static OsMutex        sSingletonLock;
   static Hotdesking*   spInstance;


  private:
   bool			bEnabled;
   UtlString	fileName;

   friend AuthPlugin* getAuthPlugin(const UtlString& name);

   Hotdesking(const UtlString& instanceName ///< the configured name for this plugin instance
              );

   SipRouter* mpSipRouter;
};

#endif // _HOTDESKING_H_
