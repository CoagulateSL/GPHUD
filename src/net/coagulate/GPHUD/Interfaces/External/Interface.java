package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputValidationFilterException;
import net.coagulate.Core.Exceptions.User.UserRemoteFailureException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTML.Elements.PlainText;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.Core.Tools.JsonTools;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.GPHUD.State.Sources;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTTPPipelines.PlainTextMapper;
import net.coagulate.SL.SL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Implements the 3rd party External interface (from SL)
 * <p>
 * I.e. the start of the JSON over HTTP connections.
 * Handed off from the HTTPListener via the main "Interface".
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interfaces.Interface {
	public static final boolean DEBUG_JSON=false;

	@Override
	protected void initialiseState(HttpRequest request, HttpContext context, Map<String, String> parameters, Map<String, String> cookies) {
		State st=state();
		st.source= Sources.EXTERNAL;
	}

	@Override
	protected void loadSession() {

	}

	@Override
	protected boolean checkAuthenticationNeeded(Method content) {
		return false;
	}

	@Override
	protected void processPostEntity(HttpEntity entity, Map<String, String> parameters) {
		try {
			final State st = state();
			final JSONObject obj;
			final String message = ByteTools.convertStreamToString(entity.getContent());
			try {
				obj = new JSONObject(message);
				st.setJson(obj);
			} catch (@Nonnull final JSONException e) {
				throw new SystemBadValueException("Parse error in '" + message + "'", e);
			}
			if (DEBUG_JSON) {
				System.out.println("EXTERNAL INTERFACE INPUT:\n" + JsonTools.jsonToString(obj));
			}
			st.setJson(obj);
			if (obj.has("callback")) {
				st.callBackURL(obj.getString("callback"));
			}
			if (obj.has("callback")) {
				Char.refreshURL(obj.getString("callback"));
			}
			if (obj.has("callback")) {
				Region.refreshURL(obj.getString("callback"));
			}
			if (obj.has("cookie")) {
				Cookie.refreshCookie(obj.getString("cookie"));
			}
			if (obj.has("interface") && obj.get("interface").equals("object")) {
				st.source = State.Sources.OBJECT;
			}
		} catch (IOException e) {
			throw new SystemRemoteFailureException("Failure processing System Interface input");
		}
	}

	@Override
	protected Method lookupPageFromUri(String line) {
		try { return getClass().getDeclaredMethod("execute",State.class); }
		catch (NoSuchMethodException e) { throw new SystemImplementationException("Local method reflection failed..."); }
	}

	@Override
	protected ContentType getContentType() {
		return ContentType.APPLICATION_JSON;
	}

	@Override
	protected void executePage(Method content) {
		final State st = state();
		Page.page().template(new PlainTextMapper.PlainTextTemplate());
		final Response response = execute(st);
		if (response == null) {
			throw new SystemBadValueException("NULL RESPONSE FROM EXECUTE!!!");
		}
		final JSONObject jsonResponse = response.asJSON(st);
		// did titler change?
		if (st.getCharacterNullable() != null) {
			st.getCharacter().appendConveyance(st, jsonResponse);
		}
		jsonResponse.remove("developerkey");
		jsonResponse.put("responsetype", response.getClass().getSimpleName());
		final String out = jsonResponse.toString(2);
		if (DEBUG_JSON) {
			System.out.println("EXTERNAL INTERFACE OUTPUT:\n" + JsonTools.jsonToString(jsonResponse));
		}
		if (out.length() >= 4096) {
			SL.report("Output exceeds limit of 4096 characters", new SystemImplementationException("Trace"), st);
		}
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
		if (obj==null) { throw new UserInputEmptyException("No JSON payload was received with this request, please make a POST request to this URL with a valid JSON structure (See External Access API Documentation for more details)",true); }
		st.sourceLocation =st.getClientIP();
		// get developer key
		if (!obj.has("developerkey")) { throw new UserInputEmptyException("No developer credentials were supplied in the request"); }
		final String developerkey=obj.getString("developerkey");
		if (developerkey.length()<16) { return new TerminateResponse("Developer key is invalid"); }
		st.json().remove("developerkey");
		// resolve the developer, or error
		final User developer=User.resolveDeveloperKey(developerkey);
		if (developer==null) {
			st.logger().warning("Unable to resolve developer for request "+obj);
			return new TerminateResponse("Developer key is not known");
		}
		st.setSourceDeveloper(developer);
		if (obj.has("applicationname")) { st.setSourceName(obj.getString("applicationname")); }
		st.setSourceOwner(developer);

		st.setInstance(decodeInstance(obj));

		if (!InstanceDevelopers.isDeveloper(st.getInstance(),developer)) {
			return new TerminateResponse("Your developer ID is not authorised at this instance");
		}
		st.setAvatar(decodeAvatar(obj));
		st.setCharacter(decodeCharacter(st,obj));

		if (st.getCharacterNullable()!=null) {
			if (st.getCharacter().getInstance()!=st.getInstance()) {
				throw new UserInputStateException("There is a mismatch between the specified instance id and the character's instance id");
			}
			final Region region=st.getCharacter().getRegion();
			if (region!=null) {
				st.setRegion(region);
				if (st.getRegionNullable()!=null) {
					if (st.getRegion().getInstance()!=st.getInstance()) {
						throw new UserInputStateException("There is a mismatch between the region's instance id and the character's instance id");
					}
				}
			}
		}

		if (obj.has("checkavatarpermission")) {
			if (obj.isNull("checkavatarpermission")) {
				throw new UserRemoteFailureException("External API Error - checkavatarpermission key is supplied but null value attached");
			}
			final String userid=obj.getString("checkavatarpermission");
			if (userid.isBlank()) { throw new UserRemoteFailureException("External API Error - checkavatarpermission supplied a blank string",true); }
			User user=User.findUserKeyNullable(userid);
			if (user==null) { user=User.findUsername(userid,false); }
			final State testPermission=new State(st.getInstance());
			testPermission.setAvatar(user);
			if (!testPermission.hasPermission("External.ConnectObjects")) {
				throw new ExternalInterfaceObjectAccessDeniedException("User "+user.getUsername()+" does not have permission External.ConnectObjects at instance "+testPermission
						.getInstance());
			}
		}

		if (st.getCharacterNullable()!=null) { st.zone=st.getCharacter().getZone(); }
		final SafeMap parameterMap=new SafeMap();
		for (final String key: st.json().keySet()) {
			final String value=st.json().get(key).toString();
			//System.out.println(key+"="+(value==null?"NULL":value));
			parameterMap.put(key,value);
		}
		final String command=obj.getString("command");
		st.postMap(parameterMap);
		GPHUD.getLogger("ExternalInterface")
		     .fine("Processing command "+command+" from "+st.sourceLocation +" identifying as '"+st.getSourceNameNullable()+"' devKey:"+st.getSourceDeveloper());
		final Response response=Modules.run(st,obj.getString("command"),parameterMap);
		InstanceDevelopers.accounting(st.getInstance(),developer,1,response.toString().length()+obj.toString().length());
		return response;
	}

	private Instance decodeInstance(final JSONObject obj) {
		Instance instance=null;
		if (obj.has("runasinstancename")) {
			instance=Instance.find(obj.getString("runasinstancename"));
		}
		if (obj.has("runasinstanceid")) {
			instance=Instance.get(obj.getInt("runasinstanceid"));
		}
		return instance;
	}

	private Char decodeCharacter(final State st,
	                             final JSONObject obj) {
		Char character=null;
		if (obj.has("runascharactername")) {
			final Char newCharacter=Char.resolve(st,obj.getString("runascharactername"));
			if (newCharacter!=null) { character=newCharacter; }
		}
		if (obj.has("runascharacterid")) {
			character=Char.get(obj.getInt("runascharacterid"));
		}
		return character;
	}

	@Nonnull
	private User decodeAvatar(final JSONObject obj) {
		User user=User.getSystem();
		if (obj.has("runasavatarname")) {
			final User newUser=User.findUsernameNullable(obj.getString("runasavatarname"),false);
			if (newUser!=null) { user=newUser; }
		}
		if (obj.has("runasavatarid")) {
			final User newUser=User.get(obj.getInt("runasavatarid"));
			if (newUser!=null) { user=newUser; }
		}
		if (user.isSuperAdmin()) {
			throw new UserInputValidationFilterException("Unable to set user to a SUPER-ADMIN");
		}
		return user;
	}


	@Override
	protected void renderUnhandledError(HttpRequest request, HttpContext context, HttpResponse response, Throwable t) {
		SL.report("ExtIF UnkEx: "+t.getLocalizedMessage(),t,state());
		JSONObject json=new JSONObject();
		json.put("error","Sorry, an unhandled internal error occurred.");
		json.put("responsetype","UnhandledException");
		response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
		response.setStatusCode(200);
	}

	@Override
	protected void renderSystemError(HttpRequest request, HttpContext context, HttpResponse response, SystemException t) {
		SL.report("ExtIF SysEx: "+t.getLocalizedMessage(),t,state());
		JSONObject json=new JSONObject();
		json.put("error","Sorry, an internal error occurred.");
		json.put("responsetype","SystemException");
		response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
		response.setStatusCode(200);
	}

	@Override
	protected void renderUserError(HttpRequest request, HttpContext context, HttpResponse response, UserException t) {
		SL.report("ExtIF User: "+t.getLocalizedMessage(),t,state());
		JSONObject json=new JSONObject();
		json.put("error",t.getLocalizedMessage());
		json.put("responsetype","UserException");
		json.put("errorclass",t.getClass().getSimpleName());
		response.setEntity(new StringEntity(json.toString(2),ContentType.APPLICATION_JSON));
		response.setStatusCode(200);
	}
}
