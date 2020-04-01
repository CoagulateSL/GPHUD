package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.*;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Set;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Implements the System interface (from SL)
 * <p>
 * I.e. the start of the JSON over HTTP(text/plain) connections.
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
		st.source=State.Sources.SYSTEM;
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
				final String message=new String(buffer);
				// DEBUGGING ONLY log entire JSON input
				// JSONify it
				final JSONObject obj;
				try { obj=new JSONObject(message); }
				catch (@Nonnull final JSONException e) {
					throw new SystemBadValueException("Parse error in '"+message+"'",e);
				}
				// stash it in the state
				// if (obj==null) { GPHUD.getLogger().warning("About to set a JSON in state to null ; input was "+message); }
				st.setJson(obj);
				// refresh tokens if necessary
				if (obj.has("callback")) { st.callbackurl(obj.getString("callback")); }
				if (obj.has("callback")) { Char.refreshURL(obj.getString("callback")); }
				if (obj.has("callback")) { Region.refreshURL(obj.getString("callback")); }
				if (obj.has("cookie")) { Cookie.refreshCookie(obj.getString("cookie")); }
				if (obj.has("interface") && obj.get("interface").equals("object")) { st.source=State.Sources.OBJECT; }

				// attempt to run the command
				// load the original conveyances
				final Response response=execute(st);
				if (response==null) { throw new SystemBadValueException("NULL RESPONSE FROM EXECUTE!!!"); }
				// convert response to JSON
				final JSONObject jsonresponse=response.asJSON(st);
				// did titler change?
				if (st.getCharacterNullable()!=null) {
					st.getCharacter().appendConveyance(st,jsonresponse);
				}
				// respond to request
				resp.setStatusCode(HttpStatus.SC_OK);
				jsonresponse.remove("developerkey");
				final String out=jsonresponse.toString();
                /*PrintWriter pw = new PrintWriter(System.out);
                jsonresponse.write(pw,4,0);
                pw.flush();
                pw.close();
                System.out.println(out);*/
				if (out.length() >= 2048) { GPHUD.getLogger().severe("Output exceeds limit of 2048 characters"); }
				resp.setEntity(new StringEntity(out,ContentType.APPLICATION_JSON));
				return;
			}
			GPHUD.getLogger().warning("Processing command of request class "+req.getClass().getName()+" which is odd?");
			// if we get here, there was no POST content, but LSL only ever POSTS (the way we use it).
			// probably some user snooping around with a browser :P
			resp.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			resp.setEntity(new StringEntity("<html><body><pre>Hello there :) What are you doing here?</pre></body></html>",ContentType.TEXT_HTML));
		}
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
				resp.setEntity(new StringEntity("{\"error\":\"Internal error occured, sorry.\"}",ContentType.APPLICATION_JSON));
				resp.setStatusCode(HttpStatus.SC_OK);
			}
			catch (@Nonnull final Exception ex) {
				SL.report("Error in system interface error handler",ex,st);
				GPHUD.getLogger().log(SEVERE,"Exception in exception handler - "+ex.getLocalizedMessage(),ex);
			}
		}
	}


	// ----- Internal Instance -----
	protected Response execute(@Nonnull final State st) {
		final JSONObject obj=st.json();
		// get developer key
		final String developerkey=obj.getString("developerkey");
		// resolve the developer, or error
		final User developer=User.resolveDeveloperKey(developerkey);
		if (developer==null) {
			st.logger().warning("Unable to resolve developer for request "+obj);
			return new TerminateResponse("Developer key is not known");
		}
		st.json().remove("developerkey");
		st.setSourcedeveloper(developer);


		// extract SL headers
		String ownername=null;
		String objectname=null;
		String regionname=null;
		String ownerkey=null;
		String shard=null;
		String objectkey=null;
		String position="???";
		for (final Header h: st.headers()) {
			//Log.log(Log.INFO,"SYSTEM","SystemInterface",h.getName()+"="+h.getValue());
			final String name=h.getName();
			final String value=h.getValue();
			if ("X-SecondLife-Owner-Name".equals(name)) { ownername=value; }
			if ("X-SecondLife-Owner-Key".equals(name)) { ownerkey=value; }
			if ("X-SecondLife-Object-Key".equals(name)) { objectkey=value; }
			if ("X-SecondLife-Region".equals(name)) { regionname=value; }
			if ("X-SecondLife-Object-Name".equals(name)) { objectname=value; }
			if ("X-SecondLife-Shard".equals(name)) { shard=value; }
			if ("X-SecondLife-Local-Position".equals(name)) { position=value; }
		}
		if ((!("Production".equals(shard)))) {
			if (shard==null) { shard="<null>"; }
			GPHUD.getLogger().severe("Unknown shard ["+shard+"]");
			return new TerminateResponse("Only accessible from Second Life Production systems.");
		}
		if (objectname==null || objectname.isEmpty()) {
			GPHUD.getLogger().severe("Failed to decode objectname header expected from SL");
			SL.report("Parse failure",new SystemRemoteFailureException("An objectname is blank."),st);
			return new TerminateResponse("Parse failure");
		}
		if (regionname==null || regionname.isEmpty()) {
			GPHUD.getLogger().severe("Failed to decode regionname header expected from SL");
			SL.report("Parse failure",new SystemRemoteFailureException("A regionname is blank."),st);
			return new TerminateResponse("Parse failure");
		}
		if (ownerkey==null || ownerkey.isEmpty()) {
			GPHUD.getLogger().severe("Failed to decode ownerkey header expected from SL");
			SL.report("Parse failure",new SystemRemoteFailureException("An ownerkey is blank."),st);
			return new TerminateResponse("Parse failure");
		}
		if (ownername==null || ownername.isEmpty()) {
			final User userlookup=User.findOptional(ownerkey);
			if (userlookup!=null) { ownername=userlookup.getName(); }
			if (ownername==null || ownername.isEmpty()) {
				GPHUD.getLogger().severe("Failed to extract ownername header from SL or ownerkeylookup via DB");
				SL.report("Parse failure",new SystemRemoteFailureException("Ownername is blank, even from DB cache."),st);
				return new TerminateResponse("Parse failure");
			}
			else {
				GPHUD.getLogger().info("Failed to get ownername from headers for key "+ownerkey+", looked up in DB as "+ownername+".");
			}
		}
		regionname=regionname.replaceFirst(" \\([0-9]+, [0-9]+\\)","");

		st.setRegionName(regionname);
		st.issuid=false;
		st.setSourcename(objectname);
		st.sourceregion=Region.findNullable(regionname,false);
		st.sourcelocation=position;
		final User owner=User.findOrCreateAvatar(ownername,ownerkey);
		st.setSourceowner(owner);
		st.objectkey=objectkey;
		st.setAvatar(owner);
		// hooks to allow things to run as "not the objects owner" (the default)
		String runasavatar=null;
		try { runasavatar=obj.getString("runasavatar"); } catch (@Nonnull final JSONException e) {}
		if (runasavatar!=null && (!("".equals(runasavatar)))) {
			st.setAvatar(User.findMandatory(runasavatar));
			st.issuid=true;
		}
		st.object=Obj.findOrNull(st,objectkey);
		if (st.object!=null) { st.object.updateRX(); }
		String runascharacter=null;
		try { runascharacter=obj.getString("runascharacter"); } catch (@Nonnull final JSONException e) {}
		if (runascharacter!=null && (!("".equals(runascharacter)))) {
			st.setCharacter(Char.get(Integer.parseInt(runascharacter)));
			st.issuid=true;
		}
		// load region from database, if it exists
		final Region region=Region.findNullable(regionname,false);
		if (region==null) {
			return processUnregistered(st);
		}
		else {
			// we are a known region, connected to an instance

			// are they authorised to run stuff?
			boolean authorised=false;
			if (developer.getId()==1) { authorised=true; }
			// TODO check the region's instance, check the permits, blah blah, proper authorisation.  "iain" gets to skip all this.  smugmode.
			// respond to it
			if (!authorised) {
				st.logger().warning("Developer "+developer+" is not authorised at this location:"+st.getRegionName());
				return new TerminateResponse("Developer key is not authorised at this instance");
			}
			else {
				// OK.  object has dev key, is authorised, resolves.  Process the actual contents oO
				final Instance instance=region.getInstance();
				st.setInstance(instance);
				st.setRegion(region);
				if (st.getCharacterNullable()==null) {
					st.setCharacter(PrimaryCharacter.getPrimaryCharacter(st,st.getKV("Instance.AutoNameCharacter").boolValue()));
				}
				try {
					obj.getString("runasnocharacter");
					st.setCharacter(null);
				}
				catch (@Nonnull final JSONException e) {}
				if (st.getCharacterNullable()!=null) { st.zone=st.getCharacter().getZone(); }
				final SafeMap parametermap=new SafeMap();
				for (final String key: st.json().keySet()) {
					final String value=st.json().get(key).toString();
					//System.out.println(key+"="+(value==null?"NULL":value));
					parametermap.put(key,value);
				}
				final String command=obj.getString("command");
				st.postmap(parametermap);
				return Modules.run(st,obj.getString("command"),parametermap);
			}
		}

	}


	@Nonnull
	private Response processUnregistered(@Nonnull final State st) {
		// region is not registered, all we allow is registration
		// note connections from non-registered regions are cause to SUSPEND operation, unless you're a GPHUD Server, cos they do 'registration'
		// if we're a "GPHUD Server" of some kind from dev id 1 then... bob's ya uncle, dont suspend :P
		final String regionname=st.getRegionName();
		if (regionname==null || regionname.isEmpty()) { throw new UserInputStateException("No region information was supplied to the GPHUD Cluster"); }
		if (st.getSourcedeveloper().getId()!=1 || !st.getSourcename().startsWith("GPHUD Region Server")) {
			GPHUD.getLogger()
			     .log(WARNING,
			          "Region '"+regionname+"' not registered but connecting with "+st.getSourcename()+" from developer "+st.getSourcedeveloper()+" owner by "+st.getSourceowner()
			         );
			return new TerminateResponse("Region not registered.");
		}
		GPHUD.getLogger().log(WARNING,"Region '"+regionname+"' not registered but connecting, recognised as GPHUD server owned by "+st.getSourceowner());
		if (!"console".equals(st.json().getString("command"))) {
			return new ErrorResponse("Region not registered, only pre-registration commands may be run");
		}
		// only the server's owner can run these commands, call them the pre-reg commands
		if (st.getAvatarNullable()!=st.getSourceowner()) {
			return new ErrorResponse("Command not authorised.  Must be Server's owner for pre-registration commands.");
		}

		// authorised, is a GPHUD Server by developer Iain Maltz (me), caller is the owner, command is console.  Lets go!
		String console=st.json().getString("console");
		if (console.charAt(0)=='*') {
			console=console.substring(1);
		}
		if (console.startsWith("createinstance ")) {
			final User ava=st.getAvatarNullable();
			if (ava==null) { return new ErrorResponse("Null avatar associated with request??"); }
			boolean ok=false;
			if (ava.isSuperAdmin()) { ok=true; }
			//if (ava.canCreate()) { ok=true; }
			if (!ok) {
				return new ErrorResponse("You are not authorised to register a new instance, please contact Iain Maltz");
			}
			console=console.replaceFirst("createinstance ","");
			try { Instance.create(console,st.getAvatarNullable()); }
			catch (@Nonnull final UserException e) {
				return new ErrorResponse("Instance registration failed: "+e.getMessage());
			}
			final Instance instance=Instance.find(console);
			st.setInstance(instance);
			//ava.canCreate(false);
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create","Instance","",console,"");
			final String success=Region.joinInstance(regionname,instance);
			if (!"".equals(success)) {
				return new ErrorResponse("Region registration failed after instance creation: "+success);
			}
			final Region region=Region.find(regionname,false);
			st.setRegion(region);
			st.sourceregion=region;
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Join","Instance","",regionname,"Joined instance "+console);
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
			final String success=Region.joinInstance(regionname,instance);
			final Region region=Region.find(regionname,false);
			st.setInstance(instance);
			st.setRegion(region);
			st.sourceregion=region;
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Join","Instance","",regionname,"Joined instance "+console);
			final JSONObject response=new JSONObject();
			response.put("rebootserver","rebootserver");
			return new JSONResponse(response);
		}
		if (console.startsWith("listinstances")) {
			final StringBuilder response=new StringBuilder("Instances:\n");
			final Set<Instance> instances=Instance.getInstances(st.getAvatarNullable());
			for (final Instance i: instances) { response.append(i.getName()).append("\n"); }
			return new OKResponse(response.toString());
		}
		return new ErrorResponse("Pre-Registration command not recognised.  Use *listinstances, *createinstance <name>, or *joininstance <name>");
	}

}
