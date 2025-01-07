package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTML.Elements.PlainText;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.JsonTools;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.*;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTTPPipelines.PlainTextMapper;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static java.util.logging.Level.WARNING;

/**
 * Implements the System interface (from SL)
 * <p>
 * I.e. the start of the JSON over HTTP(text/plain) connections.
 * Handed off from the HTTPListener via the main "Interface".
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interfaces.Interface {
	/** Enable debugging of JSON payloads */
	public static final boolean DEBUG_JSON          =false;
	/** Maximum size of a response being transmitted to SL */
	public static final int     MAX_SL_RESPONSE_SIZE=4096;
	
	@Override
	protected void earlyInitialiseState(final HttpRequest request,final HttpContext context) {
		super.earlyInitialiseState(request,context);
		final State st=state();
		st.source=State.Sources.SYSTEM;
	}
	
	@Override
	protected void processPostEntity(final HttpEntity entity,final Map<String,String> parameters) {
		try {
			final State st=state();
			final JSONObject obj;
			final String message=ByteTools.convertStreamToString(entity.getContent());
			try {
				obj=new JSONObject(message);
			} catch (@Nonnull final JSONException e) {
				throw new SystemBadValueException("Parse error in '"+message+"'",e);
			}
			if (DEBUG_JSON) {
				//noinspection UseOfSystemOutOrSystemErr
				System.out.println("SYSTEM INTERFACE INPUT:\n"+JsonTools.jsonToString(obj));
			}
			st.setJson(obj);
			if (obj.has("callback")) {
				st.callBackURL(obj.getString("callback"));
				Char.refreshURL(obj.getString("callback"));
				Region.refreshURL(obj.getString("callback"));
			}
			st.protocol=0;
			if (obj.has("protocol")) {
				try {
					st.protocol=obj.getInt("protocol");
				} catch (final NumberFormatException ignore) {
				}
			}
			if (obj.has("cookie")) {
				Cookie.refreshCookie(obj.getString("cookie"));
			}
			if (obj.has("interface")&&obj.get("interface").equals("object")) {
				st.source=State.Sources.OBJECT;
			}
		} catch (final IOException e) {
			throw new SystemRemoteFailureException("Failure processing System Interface input",e);
		}
	}
	
	@Nullable
	@Override
	protected Method lookupPage(final HttpRequest request) {
		try {
			return getClass().getDeclaredMethod("execute",State.class);
		} catch (final NoSuchMethodException e) {
			throw new SystemImplementationException("Weird internal method lookup failure",e);
		}
	}
	
	@Override
	protected void executePage(final Method content) {
		final State st=state();
		final Response response=execute(st);
		if (response==null) {
			throw new SystemBadValueException("NULL RESPONSE FROM EXECUTE!!!");
		}
		final JSONObject jsonResponse=response.asJSON(st);
		// did titler change?
		if (st.getCharacterNullable()!=null) {
			st.getCharacter().appendConveyance(st,jsonResponse);
		}
		jsonResponse.remove("developerkey");
		final String out=jsonResponse.toString();
		if (DEBUG_JSON) {
			//noinspection UseOfSystemOutOrSystemErr
			System.out.println("SYSTEM INTERFACE OUTPUT:\n"+JsonTools.jsonToString(jsonResponse));
		}
		if (out.length()>=MAX_SL_RESPONSE_SIZE) {
			SL.report("Output exceeds limit of "+MAX_SL_RESPONSE_SIZE+" characters",
			          new SystemImplementationException("Trace"),
			          st);
		}
		Page.page().template(new PlainTextMapper.PlainTextTemplate());
		Page.page().contentType(ContentType.APPLICATION_JSON);
		Page.page().root().add(new PlainText(out));
		/*}
		catch (@Nonnull final UserException e) {
			SL.report("GPHUD system interface user error",e,st);
			GPHUD.getLogger().log(WARNING,"User generated error : "+e.getLocalizedMessage(),e);
			final HttpResponse resp=st.resp();
			resp.setStatusCode(HttpStatus.SC_OK);
			resp.setEntity(new StringEntity("{\"error\":\""+e.getLocalizedMessage()+"\"}",ContentType.APPLICATION_JSON));
			resp.setStatusCode(HttpStatus.SC_OK);
		}
		catch (@Nonnull final Exception e) {
			try {
				SL.report("GPHUD system interface error",e,st);
				GPHUD.getLogger().log(SEVERE,"System Interface caught unhandled Exception : "+e.getLocalizedMessage(),e);
				final HttpResponse resp=st.resp();
				resp.setStatusCode(HttpStatus.SC_OK);
				resp.setEntity(new StringEntity("{\"error\":\"Internal error occurred, sorry.\"}",ContentType.APPLICATION_JSON));
				resp.setStatusCode(HttpStatus.SC_OK);
			}
			catch (@Nonnull final Exception ex) {
				SL.report("Error in system interface error handler",ex,st);
				GPHUD.getLogger().log(SEVERE,"Exception in exception handler - "+ex.getLocalizedMessage(),ex);
			}
		}*/
	}
	
	// ----- Internal Instance -----
	protected Response execute(@Nonnull final State st) {
		final JSONObject obj=st.jsonNullable();
		if (obj==null) {
			throw new UserInputValidationParseException(
					"No JSON payload was presented (Are you a real client or a web browser?");
		}
		// get developer key
		final String developerkey=obj.getString("developerkey");
		// resolve the developer, or error
		final User developer=User.resolveDeveloperKey(developerkey);
		if (developer==null) {
			st.logger().warning("Unable to resolve developer for request "+obj);
			return new TerminateResponse("Developer key is not known");
		}
		st.json().remove("developerkey");
		st.setSourceDeveloper(developer);
		
		
		// extract SL headers
		String ownerName=null;
		String objectName=null;
		String regionName=null;
		String ownerKey=null;
		String shard=null;
		String objectKey=null;
		String position="???";
		for (final Header h: st.req().getAllHeaders()) {
			//Log.log(Log.INFO,"SYSTEM","SystemInterface",h.getName()+"="+h.getValue());
			final String name=h.getName();
			final String value=h.getValue();
			if ("X-SecondLife-Owner-Name".equals(name)) {
				ownerName=value;
			}
			if ("X-SecondLife-Owner-Key".equals(name)) {
				ownerKey=value;
			}
			if ("X-SecondLife-Object-Key".equals(name)) {
				objectKey=value;
			}
			if ("X-SecondLife-Region".equals(name)) {
				regionName=value;
			}
			if ("X-SecondLife-Object-Name".equals(name)) {
				objectName=value;
			}
			if ("X-SecondLife-Shard".equals(name)) {
				shard=value;
			}
			if ("X-SecondLife-Local-Position".equals(name)) {
				position=value;
			}
		}
		if (Config.enforceShardCheck()) {
			if ((!("Production".equals(shard)))) {
				if (shard==null) {
					shard="<null>";
				}
				GPHUD.getLogger().severe("Unknown shard ["+shard+"]");
				return new TerminateResponse("Only accessible from Second Life Production systems.");
			}
		}
		if (objectName==null||objectName.isEmpty()) {
			GPHUD.getLogger().severe("Failed to decode object name header expected from SL");
			SL.report("Parse failure",new SystemRemoteFailureException("An object name is blank."),st);
			return new TerminateResponse("Parse failure");
		}
		if (regionName==null||regionName.isEmpty()) {
			GPHUD.getLogger().severe("Failed to decode region name header expected from SL");
			SL.report("Parse failure",new SystemRemoteFailureException("A region name is blank."),st);
			return new TerminateResponse("Parse failure");
		}
		if (ownerKey==null||ownerKey.isEmpty()) {
			GPHUD.getLogger().severe("Failed to decode owner key header expected from SL");
			SL.report("Parse failure",new SystemRemoteFailureException("An owner key is blank."),st);
			return new TerminateResponse("Parse failure");
		}
		if (ownerName==null||ownerName.isEmpty()) {
			final User userLookup=User.findUserKeyNullable(ownerKey);
			if (userLookup!=null) {
				ownerName=userLookup.getName();
			}
			if (ownerName==null||ownerName.isEmpty()) {
				GPHUD.getLogger().severe("Failed to extract owner name header from SL or owner key lookup via DB");
				SL.report("Parse failure",
				          new SystemRemoteFailureException("Owner name is blank, even from DB cache."),
				          st);
				return new TerminateResponse("Parse failure");
			} else {
				GPHUD.getLogger()
				     .info("Failed to get owner name from headers for key "+ownerKey+", looked up in DB as "+ownerName+
				           ".");
			}
		}
		regionName=regionName.replaceFirst(" \\(\\d+, \\d+\\)","");
		
		st.setRegionName(regionName);
		st.isSuid=false;
		st.setSourceName(objectName);
		st.sourceRegion=Region.findNullable(regionName,true);
		if (st.sourceRegion!=null&&st.sourceRegion.isRetired()) {
			//SL.report("Retired region connecting",new UserInputStateException("Region "+regionName+" is retired!"),st);
			return new TerminateResponse(
					"This region has been used previously and marked as retired, please contact Iain Maltz to rectify this.");
		}
		st.sourceLocation=position;
		final User owner=User.findOrCreate(ownerName,ownerKey,false);
		if (owner.isSuspended()) {
			return new TerminateResponse(
					"Your access to GPHUD has been suspended.  If you feel this is in error please contact the system operator ");
		}
		st.setSourceOwner(owner);
		st.objectKey=objectKey;
		st.setAvatar(owner);
		// hooks to allow things to run as "not the objects owner" (the default)
		String runAsAvatar=null;
		try {
			runAsAvatar=obj.getString("runasavatar");
		} catch (@Nonnull final JSONException ignored) {
		}
		if (runAsAvatar!=null&&(!(runAsAvatar.isEmpty()))) {
			st.setAvatar(User.findUsername(runAsAvatar,false));
			if (st.getAvatar().isSuspended()) {
				return new TerminateResponse(
						"Target's (runAsAvatar) access to GPHUD has been suspended.  If you feel this is in error please contact the system operator ");
			}
			st.isSuid=true;
		}
		st.object=Obj.findOrNull(st,objectKey);
		if (st.object!=null) {
			st.object.updateRX();
		}
		String runAsCharacter=null;
		try {
			runAsCharacter=obj.getString("runascharacter");
		} catch (@Nonnull final JSONException ignored) {
		}
		if (runAsCharacter!=null&&(!(runAsCharacter.isEmpty()))) {
			st.setCharacter(Char.get(Integer.parseInt(runAsCharacter)));
			st.isSuid=true;
		}
		// load region from database, if it exists
		final Region region=Region.findNullable(regionName,false);
		if (region==null) {
			return processUnregistered(st);
		} else {
			// we are a known region, connected to an instance
			
			// are they authorised to run stuff?
			final boolean authorised=developer.getId()==1;
			// TODO check the region's instance, check the permits, blah blah, proper authorisation.  "iain" gets to skip all this.
			// respond to it
			if (authorised) {
				// OK.  object has dev key, is authorised, resolves.  Process the actual contents oO
				final Instance instance=region.getInstance();
				st.setInstance(instance);
				st.setRegion(region);
				if (st.getCharacterNullable()==null) {
					st.setCharacter(Char.getMostRecent(st.getAvatar(),st.getInstance()));
				}
				try {
					obj.getString("runasnocharacter");
					st.setCharacter(null);
				} catch (@Nonnull final JSONException ignored) {
				}
				if (st.getCharacterNullable()!=null) {
					st.zone=st.getCharacter().getZone();
				}
				final SafeMap parameterMap=new SafeMap();
				for (final String key: st.json().keySet()) {
					final String value=st.json().get(key).toString();
					//System.out.println(key+"="+(value==null?"NULL":value));
					parameterMap.put(key,value);
				}
				st.postMap(parameterMap);
				Thread.currentThread().setName("Command "+obj.getString("command"));
				return Modules.run(st,obj.getString("command"),parameterMap);
			} else {
				st.logger().warning("Developer "+developer+" is not authorised at this location:"+st.getRegionName());
				return new TerminateResponse("Developer key is not authorised at this instance");
			}
		}
		
	}
	
	
	@Nonnull
	private Response processUnregistered(@Nonnull final State st) {
		// region is not registered, all we allow is registration
		// note connections from non-registered regions are cause to SUSPEND operation, unless you're a GPHUD Server, cos they do 'registration'
		// if we're a "GPHUD Server" of some kind from dev id 1 then... bob's ya uncle, don't suspend :P
		final String regionName=st.getRegionName();
		if (regionName==null||regionName.isEmpty()) {
			throw new UserInputStateException("No region information was supplied to the GPHUD Stack");
		}
		if (st.getSourceDeveloper().getId()!=1||!st.getSourceName().startsWith("GPHUD Region Server")) {
			GPHUD.getLogger()
			     .log(WARNING,
			          "Region '"+regionName+"' not registered but connecting with "+st.getSourceName()+
			          " from developer "+st.getSourceDeveloper()+" owner by "+st.getSourceOwner());
			return new TerminateResponse("Region not registered.");
		}
		GPHUD.getLogger()
		     .log(WARNING,
		          "Region '"+regionName+"' not registered but connecting, recognised as GPHUD server owned by "+
		          st.getSourceOwner());
		if (!"console".equals(st.json().getString("command"))) {
			return new ErrorResponse("Region not registered, only pre-registration commands may be run");
		}
		// only the server's owner can run these commands, call them the pre-reg commands
		if (st.getAvatarNullable()!=st.getSourceOwner()) {
			return new ErrorResponse("Command not authorised.  Must be Server's owner for pre-registration commands.");
		}
		
		// authorised, is a GPHUD Server by developer Iain Maltz (me), caller is the owner, command is console.  Lets go!
		String console=st.json().getString("console");
		if (console.charAt(0)=='*') {
			console=console.substring(1);
		}
		if (console.startsWith("createinstance ")) {
			final User ava=st.getAvatarNullable();
			if (ava==null) {
				return new ErrorResponse("Null avatar associated with request??");
			}
			boolean ok=ava.isSuperAdmin();
			if (Integer.parseInt(ava.getPreference("gphud","instancepermit","0"))>UnixTime.getUnixTime()) {
				ok=true;
			}
			if (!ok) {
				return new ErrorResponse("You are not authorised to register a new instance, please contact Iain Maltz");
			}
			console=console.replaceFirst("createinstance ","");
			try {
				Instance.create(console,st.getAvatarNullable());
			} catch (@Nonnull final UserException e) {
				return new ErrorResponse("Instance registration failed: "+e.getMessage());
			}
			ava.setPreference("gphud","instancepermit",null);
			final Instance instance=Instance.find(console);
			st.setInstance(instance);
			//ava.canCreate(false);
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create","Instance","",console,"");
			final String success=Region.joinInstance(regionName,instance);
			if (!success.isEmpty()) {
				return new ErrorResponse("Region registration failed after instance creation: "+success);
			}
			final Region region=Region.find(regionName,false);
			st.setRegion(region);
			st.sourceRegion=region;
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Join","Instance","",regionName,"Joined instance "+console);
			Modules.initialiseInstance(st);
			final JSONObject response=new JSONObject();
			response.put("rebootserver","rebootserver");
			return new JSONResponse(response);
		}
		if (console.startsWith("joininstance ")) {
			console=console.replaceFirst("joininstance ","");
			final Instance instance=Instance.find(console);
			if (instance.getOwner()!=st.getAvatar()) {
				return new ErrorResponse("Instance exists and does not belong to you");
			}
			Region.joinInstance(regionName,instance);
			final Region region=Region.find(regionName,false);
			st.setInstance(instance);
			st.setRegion(region);
			st.sourceRegion=region;
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Join","Instance","",regionName,"Joined instance "+console);
			final JSONObject response=new JSONObject();
			response.put("rebootserver","rebootserver");
			return new JSONResponse(response);
		}
		if (console.startsWith("listinstances")) {
			final StringBuilder response=new StringBuilder("Instances:\n");
			final Set<Instance> instances=Instance.getInstances(st.getAvatarNullable());
			for (final Instance i: instances) {
				response.append(i.getName()).append("\n");
			}
			return new OKResponse(response.toString());
		}
		return new ErrorResponse(
				"Pre-Registration command not recognised.  Use *listinstances, *createinstance <name>, or *joininstance <name>");
	}
	
	@Override
	protected void renderUserError(final HttpRequest request,
	                               final HttpContext context,
	                               final HttpResponse response,
	                               final UserException userException) {
		SL.report("SysIF User: "+userException.getLocalizedMessage(),userException,state());
		final JSONObject json=new JSONObject();
		json.put("error",userException.getLocalizedMessage());
		json.put("responsetype","UserException");
		json.put("errorclass",userException.getClass().getSimpleName());
		response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
		response.setStatusCode(HttpStatus.SC_OK);
	}
	
	@Override
	protected void renderSystemError(final HttpRequest request,
	                                 final HttpContext context,
	                                 final HttpResponse response,
	                                 final SystemException systemException) {
		SL.report("SysIF SysEx: "+systemException.getLocalizedMessage(),systemException,state());
		final JSONObject json=new JSONObject();
		json.put("error","Sorry, an internal error occurred.");
		json.put("responsetype","SystemException");
		response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
		response.setStatusCode(HttpStatus.SC_OK);
	}
	
	@Override
	protected void renderUnhandledError(final HttpRequest request,
	                                    final HttpContext context,
	                                    final HttpResponse response,
	                                    final Throwable t) {
		SL.report("SysIF UnkEx: "+t.getLocalizedMessage(),t,state());
		final JSONObject json=new JSONObject();
		json.put("error","Sorry, an unhandled internal error occurred.");
		json.put("responsetype","UnhandledException");
		response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
		response.setStatusCode(HttpStatus.SC_OK);
	}
}
