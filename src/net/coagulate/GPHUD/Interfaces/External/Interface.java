package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputValidationException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.JsonTools;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.InstanceDevelopers;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.GPHUD.State.Sources;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.InputStream;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Implements the 3rd party External interface (from SL)
 * <p>
 * I.e. the start of the JSON over HTTP connections.
 * Handed off from the HTTPListener via the main "Interface".
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interface {

	// ---------- INSTANCE ----------

	/**
	 * this is where the request comes in after generic processing.
	 * We basically just encapsulate all requests in an Exception handler that will spew errors as HTML errors (rather than JSON errors).
	 * These are rather useless in production, but in DEV we dump the stack traces too.
	 *
	 * @param st Session State
	 */
	@Override
	public void process(@Nonnull final State st) {
		final boolean debug=false;
		st.source=Sources.EXTERNAL;
		//for (Header h:headers) { System.out.println(h.getName()+"="+h.getValue()); }
		try {
			// does it contain a "body" (its a POST request, it should...)
			final HttpRequest req=st.req();
			final HttpResponse resp=st.resp();
			if (req instanceof HttpEntityEnclosingRequest) {
				// stream it into a buffer
				final HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest) req;
				final InputStream is=r.getEntity().getContent();
				final byte[] buffer=new byte[65*1024];
				final int ammountread=is.read(buffer);
				if (ammountread==-1) {
					throw new SystemInitialisationException("Reading from HTTP Response gave immediate EOF?");
				}
				final String message=new String(buffer,0,ammountread);
				// DEBUGGING ONLY log entire JSON input
				// JSONify it
				final JSONObject obj;
				try { obj=new JSONObject(message); }
				catch (@Nonnull final JSONException e) {
					SystemBadValueException badvalue=new SystemBadValueException("Unable to parse '"+message+"'",e);
					throw new UserInputValidationParseException("JSON Parse Error:"+e.getLocalizedMessage(),badvalue);
				}
				// stash it in the state
				// if (obj==null) { GPHUD.getLogger().warning("About to set a JSON in state to null ; input was "+message); }
				st.setJson(obj);

				// attempt to run the command
				// load the original conveyances
				final Response response=execute(st);
				if (response==null) { throw new SystemBadValueException("NULL RESPONSE FROM EXECUTE!!!"); }
				// convert response to JSON
				final JSONObject jsonresponse=response.asJSON(st);
				// respond to request
				resp.setStatusCode(HttpStatus.SC_OK);
				jsonresponse.remove("developerkey");
				jsonresponse.put("responsetype",response.getClass().getSimpleName());
				final String out=JsonTools.jsonToString(jsonresponse);
                /*PrintWriter pw = new PrintWriter(System.out);
                jsonresponse.write(pw,4,0);
                pw.flush();
                pw.close();
                System.out.println(out);*/
				resp.setEntity(new StringEntity(out,ContentType.APPLICATION_JSON));
				return;
			}
			GPHUD.getLogger().warning("Processing command of request class "+req.getClass().getName()+" which is odd?");
			// if we get here, there was no POST content, but LSL only ever POSTS (the way we use it).
			// probably some user snooping around with a browser :P
			resp.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			resp.setEntity(new StringEntity("<html><body><pre>Hello there :) This isn't how this works.</pre></body></html>\n",ContentType.TEXT_HTML));
		}
		catch (@Nonnull final UserException e) {
			SL.report("GPHUD external interface user error",e,st);
			GPHUD.getLogger().log(WARNING,"User generated error : "+e.getLocalizedMessage(),e);
			final HttpResponse resp=st.resp();
			resp.setStatusCode(HttpStatus.SC_OK);
			JSONObject newresponse=new JSONObject();
			newresponse.put("error",e.getLocalizedMessage());
			newresponse.put("errorclass",e.getClass().getSimpleName());
			newresponse.put("responsetype","UserException");
			resp.setEntity(new StringEntity(JsonTools.jsonToString(newresponse),ContentType.APPLICATION_JSON));
			resp.setStatusCode(HttpStatus.SC_OK);
		}
		catch (@Nonnull final Exception e) {
			try {
				SL.report("GPHUD external interface error",e,st);
				GPHUD.getLogger().log(SEVERE,"External Interface caught unhandled Exception : "+e.getLocalizedMessage(),e);
				final HttpResponse resp=st.resp();
				resp.setStatusCode(HttpStatus.SC_OK);
				JSONObject newresponse=new JSONObject();
				newresponse.put("error","Internal error occured, sorry.");
				newresponse.put("responsetype","SystemException");
				resp.setEntity(new StringEntity(JsonTools.jsonToString(newresponse),ContentType.APPLICATION_JSON));
				resp.setStatusCode(HttpStatus.SC_OK);
			}
			catch (@Nonnull final Exception ex) {
				SL.report("Error in external interface error handler",ex,st);
				GPHUD.getLogger().log(SEVERE,"Exception in exception handler - "+ex.getLocalizedMessage(),ex);
			}
		}
	}

	// ----- Internal Instance -----
	protected Response execute(@Nonnull final State st) {
		final JSONObject obj=st.json();
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
		st.setSourcedeveloper(developer);
		if (obj.has("applicationname")) { st.setSourcename(obj.getString("applicationname")); }
		st.setSourceowner(developer);

		st.setInstance(decodeInstance(obj));

		if (!InstanceDevelopers.isDeveloper(st.getInstance(),developer)) {
			return new TerminateResponse("Your developer ID is not authorised at this instance");
		}
		st.setAvatar(decodeAvatar(obj));
		st.setCharacter(decodeCharacter(st,obj));

		if (st.getCharacterNullable()!=null) {
			if (st.getCharacter().getInstance()!=st.getInstance()) {
				throw new UserInputStateException("There is a mismatch between the specified instanceid and the character's instanceid");
			}
			Region region=st.getCharacter().getRegion();
			if (region!=null) {
				st.setRegion(region);
				if (st.getRegionNullable()!=null) {
					if (st.getRegion().getInstance()!=st.getInstance()) {
						throw new UserInputStateException("There is a mismatch between the region's instanceid and the character's instanceid");
					}
				}
			}
		}
		st.sourcelocation=st.getClientIP();
		if (st.getCharacterNullable()!=null) { st.zone=st.getCharacter().getZone(); }
		final SafeMap parametermap=new SafeMap();
		for (final String key: st.json().keySet()) {
			final String value=st.json().get(key).toString();
			//System.out.println(key+"="+(value==null?"NULL":value));
			parametermap.put(key,value);
		}
		final String command=obj.getString("command");
		st.postmap(parametermap);
		GPHUD.getLogger("ExternalInterface")
		     .fine("Processing command "+command+" from "+st.sourcelocation+" identifying as '"+st.getSourcenameNullable()+"' devkey:"+st.getSourcedeveloper());
		Response response=Modules.run(st,obj.getString("command"),parametermap);
		InstanceDevelopers.accounting(st.getInstance(),developer,1,response.toString().length()+obj.toString().length());
		return response;
	}

	private Instance decodeInstance(JSONObject obj) {
		Instance instance=null;
		if (obj.has("runasinstancename")) {
			Instance newinstance=Instance.find(obj.getString("runasinstancename"));
			if (newinstance!=null) { instance=newinstance; }
		}
		if (obj.has("runasinstanceid")) {
			Instance newinstance=Instance.get(obj.getInt("runasinstanceid"));
			if (newinstance!=null) { instance=newinstance; }
		}
		return instance;
	}

	private Char decodeCharacter(State st,
	                             JSONObject obj) {
		Char character=null;
		if (obj.has("runascharactername")) {
			Char newcharacter=Char.resolve(st,obj.getString("runascharactername"));
			if (newcharacter!=null) { character=newcharacter; }
		}
		if (obj.has("runascharacterid")) {
			Char newcharacter=Char.get(obj.getInt("runascharacterid"));
			if (newcharacter!=null) { character=newcharacter; }
		}
		return character;
	}

	@Nonnull
	private User decodeAvatar(JSONObject obj) {
		User user=User.getSystem();
		if (obj.has("runasavatarname")) {
			User newuser=User.findOptional(obj.getString("runasavatarname"));
			if (newuser!=null) { user=newuser; }
		}
		if (obj.has("runasavatarid")) {
			User newuser=User.get(obj.getInt("runasavatarid"));
			if (newuser!=null) { user=newuser; }
		}
		if (user.isSuperAdmin()) {
			throw new UserInputValidationException("Unable to set user to a SUPERADMIN");
		}
		return user;
	}

}
