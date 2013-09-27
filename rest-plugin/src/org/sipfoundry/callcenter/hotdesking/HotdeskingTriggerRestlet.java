package org.sipfoundry.callcenter.hotdesking;

import nl.telecats.sipxecs.callcenter.callCenterService.CallCenterServiceException;

import org.apache.log4j.Logger;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.sipfoundry.callcenter.hotdesking.exceptions.PhoneNotFoundException;
import org.sipfoundry.callcenter.hotdesking.exceptions.UserNotFoundException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class HotdeskingTriggerRestlet extends Restlet implements ApplicationContextAware {

	private static final Logger LOG = Logger.getLogger( HotdeskingTriggerRestlet.class );
	public static final String BEAN_ID = "hotdeskingTriggerRestlet";
	public static final String METHOD_LOGIN = "login";
	public static final String METHOD_LOGOUT = "logout";
	private ApplicationContext m_ctx;
	
	@Override
    public void handle( Request request, Response response ) {
		
		String serviceName = ( String ) request.getAttributes().get( HotdeskingTriggerParams.SERVICE );
        String method = ( String ) request.getAttributes().get( HotdeskingTriggerParams.METHOD );
        String userId = ( String ) request.getAttributes().get( HotdeskingTriggerParams.USER_ID );
        String phoneId = ( String ) request.getAttributes().get( HotdeskingTriggerParams.PHONE_ID );
        
        LOG.info( "Received request" );
        LOG.info( "Service provider requested = " + serviceName );
        LOG.info( "Method = " + method );
        LOG.info( "Query params (userId, phoneId) = (" + userId + ", " + phoneId + ")" );
        HotdeskingTriggerService service = null;
        try {
        	service = m_ctx.getBean( serviceName, HotdeskingTriggerService.class );
	        if( service == null ) {
	        	LOG.warn( String.format( "Requested service provider (%s) not found in spring context", serviceName ) );
	        	response.setStatus( Status.SERVER_ERROR_SERVICE_UNAVAILABLE, String.format("Service %s could not be found.", serviceName) );
	        	return;
	        }
        } catch(Exception e) {
        	LOG.warn( String.format( "Requested service provider (%s) not found in spring context", serviceName ) );
        	response.setStatus( Status.SERVER_ERROR_SERVICE_UNAVAILABLE, String.format("Service %s could not be found.", serviceName) );
        	return;
        }
        
        if( method.equals(METHOD_LOGIN) ) {
        	try {
				service.login( userId, phoneId );
			} catch (UserNotFoundException e) {
				LOG.error(String.format("User %s could not be found.", userId ), e);
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, String.format("User %s could not be found.", userId ));
				return;
			} catch (PhoneNotFoundException e) {
				LOG.error(String.format("Phone %s could not be found.", phoneId ), e);
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, String.format("Phone %s could not be found.", phoneId ));
				return;
			} catch (CallCenterServiceException e) {
				String error = String.format("Error while connecting to soap endpoint. \n\tErrocode: %d\n\tDescription: %s",e.getErrorCode(), e.getDescription()); 
				LOG.error(error, e);
				response.setStatus(Status.SERVER_ERROR_INTERNAL, error);
				return;
			}
        } else if( method.equals(METHOD_LOGOUT) ) {
        	try {
				service.logout( userId, phoneId );
			} catch (UserNotFoundException e) {
				LOG.error(String.format("User %s could not be found.", userId ), e);
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, String.format("User %s could not be found.", userId ));
				return;
			} catch (PhoneNotFoundException e) {
				LOG.error(String.format("Phone %s could not be found.", phoneId ), e);
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, String.format("Phone %s could not be found.", phoneId ));
				return;
			} catch (CallCenterServiceException e) {
				String error = String.format("Error while connecting to soap endpoint. \n\tErrocode: %d\n\tDescription: %s",e.getErrorCode(), e.getDescription());
				LOG.error(error, e);
				response.setStatus(Status.SERVER_ERROR_INTERNAL, error);
				return;
			}
        } else {
        	String error = String.format( "Requested method (%s) is not available", method ); 
        	LOG.warn( error );
        	response.setStatus( Status.SERVER_ERROR_NOT_IMPLEMENTED, error );
        	return;
        }
        response.setStatus( Status.SUCCESS_OK );
    }

	@Override
	public void setApplicationContext( ApplicationContext applicationContext )
			throws BeansException {
		this.m_ctx = applicationContext;		
	}
}
 