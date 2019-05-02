package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
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

import java.io.InputStream;
import java.util.Set;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/** Implements the System interface (from SL)
 * 
 * I.e. the start of the JSON over HTTP(text/plain) connections.
 * Handed off from the HTTPListener via the main "Interface".
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interface {

    /** this is where the request comes in after generic processing.
     * We basically just encapsulate all requests in an Exception handler that will spew errors as HTML errors (rather than JSON errors).
     * These are rather useless in production, but in DEV we dump the stack traces too.
     * @param st Session State
     */
    @Override
    public void process(State st) {
        boolean debug=false;
        st.source=State.Sources.SYSTEM;
        //for (Header h:headers) { System.out.println(h.getName()+"="+h.getValue()); }
        try {
            // does it contain a "body" (its a POST request, it should...)
            HttpRequest req=st.req;
            HttpResponse resp=st.resp;
            if (req instanceof HttpEntityEnclosingRequest) {
                // stream it into a buffer
                HttpEntityEnclosingRequest r=(HttpEntityEnclosingRequest)req;
                InputStream is=r.getEntity().getContent();
                byte buffer[]=new byte[65*1024];
                is.read(buffer);
                String message=new String(buffer);
                // DEBUGGING ONLY log entire JSON input
                if (debug) { GPHUD.getLogger().finer("JSON in : "+message); }
                // JSONify it
                JSONObject obj;
                try { obj = new JSONObject(message); }
                catch (JSONException e) { 
                    SystemException se = new SystemException("Parse error in '"+message+"'");
                    se.initCause(e);
                    throw se;
                }
                // stash it in the state
                st.json=obj;
                // refresh tokens if necessary
                if (obj.has("callback")) { st.callbackurl=obj.getString("callback"); }
                if (obj.has("callback")) { Char.refreshURL(obj.getString("callback")); }
                if (obj.has("callback")) { Region.refreshURL(obj.getString("callback")); }
                if (obj.has("cookie")) { Cookies.refreshCookie(obj.getString("cookie")); }



                // attempt to run the command
                // load the original conveyances
                Response response=execute(st);
                if (response==null) { throw new SystemException("NULL RESPONSE FROM EXECUTE!!!"); }
                // convert response to JSON
                JSONObject jsonresponse=response.asJSON(st);
                // did titler change?
                if (st.getCharacterNullable()!=null) { 
                    st.getCharacter().appendConveyance(st,jsonresponse);
                }
                // respond to request
                resp.setStatusCode(HttpStatus.SC_OK);
                String out=jsonresponse.toString();
                /*PrintWriter pw = new PrintWriter(System.out);
                jsonresponse.write(pw,4,0);
                pw.flush();
                pw.close();
                System.out.println(out);*/
                if (debug) { GPHUD.getLogger().finer("JSON out : "+out); }
                if (out.length()>=2048) { GPHUD.getLogger().severe("Output exceeds limit of 2048 characters"); }
                resp.setEntity(new StringEntity(out,ContentType.TEXT_PLAIN));                
                return;
            }
            GPHUD.getLogger().warning("Processing command of request class "+req.getClass().getName()+" which is odd?");
            // if we get here, there was no POST content, but LSL only ever POSTS (the way we use it).
            // probably some user snooping around with a browser :P 
            resp.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            resp.setEntity(new StringEntity("<html><body><pre>Hello there :) What are you doing here?</pre></body></html>",ContentType.TEXT_HTML));
        }
        catch (UserException e) {
                SL.report("GPHUD system interface user error",e,st);
                GPHUD.getLogger().log(WARNING,"User generated error : "+e.getLocalizedMessage(),e);
                HttpResponse resp=st.resp;
                resp.setStatusCode(HttpStatus.SC_OK);
                resp.setEntity(new StringEntity("{\"error\":\""+e.getLocalizedMessage()+"\"}",ContentType.TEXT_PLAIN)); 
                resp.setStatusCode(HttpStatus.SC_OK);                        
        }
        catch (Exception e) {
            try {
                SL.report("GPHUD system interface error",e,st);
                GPHUD.getLogger().log(SEVERE,"System Interface caught unhandled Exception : "+e.getLocalizedMessage(),e);
                HttpResponse resp=st.resp;
                resp.setStatusCode(HttpStatus.SC_OK);
                resp.setEntity(new StringEntity("{\"error\":\"Internal error occured, sorry.\"}",ContentType.TEXT_PLAIN)); 
                resp.setStatusCode(HttpStatus.SC_OK);            
            } catch (Exception ex) {
                SL.report("Error in system interface error handler",ex,st);
                GPHUD.getLogger().log(SEVERE,"Exception in exception handler - "+ex.getLocalizedMessage(),ex);
            }
        }
    }
    

    protected Response execute(State st) throws SystemException, UserException {
        JSONObject obj=st.json;
        // get developer key
        String developerkey=obj.getString("developerkey");
        // resolve the developer, or error
        User developer=User.resolveDeveloperKey(developerkey);
        if (developer==null) { st.logger().warning("Unable to resolve developer for request "+st.jsoncommand);
            return new TerminateResponse("Developer key is not known");
        }
        st.sourcedeveloper=developer;                


        // extract SL headers
        String ownername=null;
        String objectname=null;
        String regionname=null;
        String ownerkey=null;
        String shard=null;
        String position="???";
        for (Header h:st.headers) {
            //Log.log(Log.INFO,"SYSTEM","SystemInterface",h.getName()+"="+h.getValue());
            String name=h.getName();
            String value=h.getValue();
            if (name.equals("X-SecondLife-Owner-Name")) { ownername=value; }
            if (name.equals("X-SecondLife-Owner-Key")) { ownerkey=value; }            
            if (name.equals("X-SecondLife-Region")) { regionname=value; }
            if (name.equals("X-SecondLife-Object-Name")) { objectname=value; }
            if (name.equals("X-SecondLife-Shard")) { shard=value; }
            if (name.equals("X-SecondLife-Local-Position")) { position=value; }
        }
        if (shard==null || (!(shard.equals("Production")))) {
            if (shard==null) { shard="<null>"; }
            GPHUD.getLogger().severe("Unknown shard ["+shard+"]");
            return new TerminateResponse("Only accessible from Second Life Production systems.");
        }
        if (ownername==null || objectname==null || regionname==null) { GPHUD.getLogger().severe("Failed to decode headers expected from SL"); return new TerminateResponse("Parse failure"); }
        regionname=regionname.replaceFirst(" \\([0-9]+, [0-9]+\\)","");
                
        st.setRegionName(regionname);
        st.issuid=false;
        st.sourcename=objectname;
        st.sourceregion=Region.find(regionname);
        st.sourcelocation=position;
        User owner=User.findOrCreateAvatar(ownername,ownerkey);
        st.sourceowner=owner;
        st.setAvatar(owner);
        // hooks to allow things to run as "not the objects owner" (the default)
        String runasavatar=null;
        try { runasavatar=obj.getString("runasavatar"); } catch (JSONException e) {}
        if (runasavatar!=null && (!(runasavatar.equals("")))) { st.setAvatar(User.findMandatory(runasavatar)); st.issuid=true; }
        String runascharacter=null;
        try { runascharacter=obj.getString("runascharacter"); } catch (JSONException e) {}
        if (runascharacter!=null && (!(runascharacter.equals("")))) { st.setCharacter(Char.get(Integer.parseInt(runascharacter))); st.issuid=true; }
        // load region from database, if it exists
        Region region=Region.find(regionname);
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
                st.logger().warning("Developer "+developer.toString()+" is not authorised at this location:"+st.getRegionName());
                return new TerminateResponse("Developer key is not authorised at this instance");
            } else {
                // OK.  object has dev key, is authorised, resolves.  Process the actual contents oO
                Instance instance=region.getInstance();
                st.setInstance(instance);
                st.setRegion(region);
                if (st.getCharacterNullable()==null) { st.setCharacter(PrimaryCharacters.getPrimaryCharacter(st,st.getKV("Instance.AutoNameCharacter").boolValue())); }
                try { obj.getString("runasnocharacter"); st.setCharacter(null); } catch (JSONException e) {}
                if (st.getCharacterNullable()!=null) { st.zone=st.getCharacter().getZone(); }
                SafeMap parametermap=new SafeMap();
                for (String key:st.json.keySet()) {
                    String value=st.json.get(key).toString();
                    //System.out.println(key+"="+(value==null?"NULL":value));
                    parametermap.put(key,value);
                }
                String command=obj.getString("command");
                return Modules.run(st, obj.getString("command"), parametermap);
            }
        }

    }


    private Response processUnregistered(State st) {
        // region is not registered, all we allow is registration
        // note connections from non-registered regions are cause to SUSPEND operation, unless you're a GPHUD Server, cos they do 'registration'
        // if we're a "GPHUD Server" of some kind from dev id 1 then... bob's ya uncle, dont suspend :P
        String regionname=st.getRegionName();
        if (st.sourcedeveloper.getId()!=1 || !st.sourcename.startsWith("GPHUD Region Server")) {
            GPHUD.getLogger().log(WARNING,"Region '"+regionname+"' not registered but connecting with "+st.sourcename+" from developer "+st.sourcedeveloper+" owner by "+st.sourceowner);
            return new TerminateResponse("Region not registered.");
        }
        GPHUD.getLogger().log(WARNING,"Region '"+regionname+"' not registered but connecting, recognised as GPHUD server owned by "+st.sourceowner);
        if (!st.json.getString("command").equals("console")) {
            return new ErrorResponse("Region not registered, only pre-registration commands may be run");
        }
        // only the server's owner can run these commands, call them the pre-reg commands
        if (st.avatar()!=st.sourceowner) { return new ErrorResponse("Command not authorised.  Must be Server's owner for pre-registration commands."); } 
        
        // authorised, is a GPHUD Server by developer Iain Maltz (me), caller is the owner, command is console.  Lets go!
        String console=st.json.getString("console");
        if (console.charAt(0)=='*') {
            console=console.substring(1);
        }
        if (console.startsWith("createinstance "))
        {
            User ava=st.avatar();
            if (ava==null) { return new ErrorResponse("Null avatar associated with request??"); }
            boolean ok=false;
            if (ava.isSuperAdmin()) { ok=true; }
            //if (ava.canCreate()) { ok=true; }
            if (!ok) { return new ErrorResponse("You are not authorised to register a new instance, please contact Iain Maltz"); }
            console=console.replaceFirst("createinstance ","");
            try { Instance.create(console,st.avatar()); }
            catch (UserException e) { return new ErrorResponse("Instance registration failed: "+e.getMessage()); }
            Instance instance=Instance.find(console);
            if (instance==null) { return new ErrorResponse("Failed to find instance after registering it :("); }
            st.setInstance(instance);
            //ava.canCreate(false);
            Audit.audit(st, Audit.OPERATOR.AVATAR,null,null,"Create","Instance","",console,"");
            String success=Region.joinInstance(regionname, instance);
            if (!success.equals("")) {
                return new ErrorResponse("Region registration failed after instance creation: "+success);
            }
            Region region=Region.find(regionname);
            st.setRegion(region);
            st.sourceregion=region;
            Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Join", "Instance","",regionname,"Joined instance "+console);
            Modules.initialiseInstance(st);
            JSONObject response=new JSONObject();
            response.put("rebootserver","rebootserver");
            return new JSONResponse(response);
        }
        if (console.startsWith("joininstance "))
        {
            console=console.replaceFirst("joininstance ","");
            Instance instance=Instance.find(console);
            if (instance!=null && instance.getOwner()!=st.avatar()) {
                return new ErrorResponse("Instance exists and does not belong to you");
            }
            if (instance==null) { return new ErrorResponse("Failed to find named instance, see *listinstances"); }
            String success=Region.joinInstance(regionname,instance);
            Region region=Region.find(regionname);
            st.setRegion(region);
            st.sourceregion=region;
            st.setInstance(instance);
            Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Join", "Instance","",regionname,"Joined instance "+console);
            JSONObject response=new JSONObject();
            response.put("rebootserver","rebootserver");
            return new JSONResponse(response);
        }
        if (console.startsWith("listinstances")) {
          String response="Instances:\n";
          Set<Instance> instances=Instance.getInstances(st.avatar());
          for(Instance i:instances) { response+=i.getName()+"\n"; }
          return new OKResponse(response);
        }
        return new ErrorResponse("Pre-Registration command not recognised.  Use *listinstances, *createinstance <name>, or *joininstance <name>");
    }   

}
