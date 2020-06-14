package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.GPHUDClient.Connect;
import net.coagulate.GPHUD.Modules.KVValue;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Logs a session in as a particular character
 * <p>
 * THIS CODE IS SUBJECT TO CHANGE (and is just a stub implementation right now)
 * REQUIRES MULTI CHAR SUPPORT and shared char support.  or maybe it doesn't and thats a post registration thing.
 * we shall see!
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Login {
	// ---------- STATICS ----------
	/*
	@Nonnull
	@Deprecated
	@Commands(context=Context.AVATAR,
	          permitConsole=false,
	          permitUserWeb=false,
	          permitScripting=false,
	          description="Register this session as a character connection",
	          permitObject=false,
	          permitExternal=false)

	public static Response login(@Nonnull final State st,
	                             @Nullable @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                  description="Version number of the HUD that is connecting",
	                                                  max=128,
	                                                  mandatory=false) final String version,
	                             @Nullable @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                  description="Version date of the HUD that is connecting",
	                                                  max=128,
	                                                  mandatory=false) final String versiondate,
	                             @Nullable @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                  description="Version time of the HUD that is connecting",
	                                                  max=128,
	                                                  mandatory=false) final String versiontime) {
		if (EndOfLifing.hasExpired(version)) {
			st.logger().warning("Rejected Legacy HUD connection from end-of-life product version "+version+" from "+versiondate+" "+versiontime);
			return new TerminateResponse("Sorry, this HUD is so old it is no longer supported.\nPlease tell your sim administrator to deploy an update.");
		}

		final boolean debug=false;
		////// CHANGE ALL THIS, HAVE A
		////// "USINGCHARACTER" COLUMN FOR <AVATAR> TYPES
		////// THIS IS IMPORTANT TO RESOLVE A TARGET FROM AN AVATAR LATER WHEN TARGETTING FOR EXAMPLE

		String url=null;
		st.json();
		try { url=st.json().getString("callback"); } catch (@Nonnull final JSONException e) {}
		if (url==null || "".equals(url)) {
			st.logger().log(WARNING,"No callback URL sent with character registration");
			return new ErrorResponse("You are not set up with a callback URL in Characters.Login");
		}
		final boolean autocreate=st.getKV("Instance.AutoNameCharacter").boolValue();
		final Char character=PrimaryCharacter.getPrimaryCharacter(st,autocreate);
		if (character==null) {
			if (autocreate) {
				throw new UserInputStateException("Failed to get/create a character for user "+st.getAvatarNullable());
			} // autocreate or die :P
			// if not auto create, offer "characters.create" i guess
			final JSONResponse response=new JSONResponse(Modules.getJSONTemplate(st,"characters.create"));
			response.asJSON(st)
			        .put("hudtext","Creating character...")
			        .put("hudcolor","<1.0,0.75,0.75>")
			        .put("titlertext","Creating character...")
			        .put("titlercolor","<1.0,0.75,0.75>")
			        .put("message","Welcome.  You do not have any characters, please create a new one.");
			return response;
		}
		// we have a character at least
		// before actually logging it in, we should check that it is 'complete'
		st.getCharacter().setURL(url);
		final Region region=st.getRegion();
		st.getCharacter().setRegion(region);
		character.setPlayedBy(st.getAvatar());
		final State simulate=st.simulate(character);
		if (st.jsonNullable()!=null) { simulate.setJson(st.json()); }
		final String initscript=simulate.getKV("Instance.CharInitScript").toString();
		String loginmessage="";
		if (initscript!=null && (!initscript.isEmpty())) {
			// let the init script have a "run"
			final Script init=Script.findNullable(simulate,initscript);
			if (init==null) { loginmessage="===> Character initialisation script "+initscript+" was not found"; }
			else {
				final GSVM initialisecharacter=new GSVM(init.getByteCode());
				initialisecharacter.invokeOnExit("characters.login");
				final Response response=initialisecharacter.execute(simulate);
				if (initialisecharacter.suspended()) { // bail here
					return response;
				} // else carry on and discard the response
			}
		}
		for (final Attribute a: st.getAttributes()) {
			if (a.getRequired()) {
				final Attribute.ATTRIBUTETYPE type=a.getType();
				switch (type) {
					case TEXT: // mandatory text doesn't work at this time
					case FLOAT:
					case INTEGER:
						final String value=simulate.getRawKV(character,"characters."+a.getName());
						if (value==null || value.isEmpty()) {
							final KVValue maxkv=st.getKV("characters."+a.getName()+"MAX");
							Float max=null;
							if (!maxkv.value().isEmpty()) { max=maxkv.floatValue(); }
							String maxstring="";
							if (max!=null && max>0) { maxstring=", which must be no greater than "+max; }
							final JSONObject json=new JSONObject();
							json.put("hudtext","Initialising character...")
							    .put("hudcolor","<1.0,0.75,0.75>")
							    .put("titlertext","Initialising character...")
							    .put("titlercolor","<1.0,0.75,0.75>")
							    .put("message","Character creation requires you to input attribute "+a.getName()+maxstring);
							json.put("incommand","runtemplate");
							json.put("invoke","characters.initialise");
							json.put("args","1");
							json.put("attribute",a.getName());
							json.put("arg0name","value");
							json.put("arg0description","You must select a "+a.getName()+" for your Character before you can use it"+maxstring);
							json.put("arg0type","TEXTBOX");
							return new JSONResponse(json);
						}
						break;
					case GROUP:
						if (a.getSubType()!=null && CharacterGroup.getGroup(character,a.getSubType())==null && CharacterGroup.hasChoices(st,a)) {
							final JSONObject json=new JSONObject();
							json.put("hudtext","Initialising character...")
							    .put("hudcolor","<1.0,0.75,0.75>")
							    .put("titlertext","Initialising character...")
							    .put("titlercolor","<1.0,0.75,0.75>")
							    .put("message","Character creation requires you to select a choice for attribute "+a.getName());
							CharacterGroup.createChoice(st,json,"arg0",a);
							json.put("incommand","runtemplate");
							json.put("invoke","characters.initialise");
							json.put("args","1");
							json.put("attribute",a.getName());
							json.put("arg0description","You must select a "+a.getName()+" for your Character before you can use it");
							return new JSONResponse(json);
						}
						break;
					case CURRENCY:
						final Currency currency=Currency.findNullable(st,a.getName());
						if (currency!=null && currency.entries(st,st.getCharacter())==0 && a.getDefaultValue()!=null && !a.getDefaultValue().isEmpty()) {
							final int ammount=Integer.parseInt(a.getDefaultValue());
							currency.spawnInAsSystem(st,st.getCharacter(),ammount,"Starting balance issued");
						}
						break;
					case POOL:
						//System.out.println("Character "+character+" validation check for pool "+a+" has no currently defined meaning.  NO-OP.  Passing check.");
						break;
					default:
						throw new SystemConsistencyException("Unhandled attribute type "+type);
				}
			}
		}
		// AND LOGIN
		st.setCharacter(character);
		st.logger().log(INFO,"Logging in as "+character);
		final String oldavatarurl=st.getCharacter().getURL();
		if (oldavatarurl!=null && !oldavatarurl.equals(url)) {
			final JSONObject shutdownjson=new JSONObject().put("incommand","shutdown").put("shutdown","Replaced by new registration");
			final Transmission shutdown=new Transmission((Char) null,shutdownjson,oldavatarurl);
			shutdown.start();
		}
		final JSONObject registeringjson=new JSONObject().put("incommand","registering");
		String regmessage;
		if (st.getInstance().getOwner().getId()==st.getAvatar().getId()) {
			// is instance owner
			regmessage=GPHUD.serverVersion()+" [https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head Release Notes]";
			if (st.getRegion().needsUpdate()) {
				regmessage+="\n=====\nUpdate required: A new GPHUD Region Server has been released and is being sent to you, please place it near the existing one.  The old "+"one will then disable its self and can be deleted.\n=====";
				Distribution.getServer(st);
			}
		}
		else {
			//regmessage="O:"+st.getInstance().getOwner().getId()+" U:"+st.getCharacter().getId()+" "+GPHUD.serverVersion();
			regmessage=GPHUD.serverVersion();
		}
		registeringjson.put("message",regmessage);
		final Transmission registering=new Transmission((Char) null,registeringjson,url);
		//noinspection CallToThreadRun
		registering.run(); // note null char to prevent it sticking payloads here, it clears the titlers :P
		Visit.initVisit(st,st.getCharacter(),region);
		if (version!=null && versiondate!=null && versiontime!=null && !version.isEmpty() && !versiondate.isEmpty() && !versiontime.isEmpty()) {
			region.recordHUDVersion(st,version,versiondate,versiontime);
		}
		final Instance instance=st.getInstance();
		final String cookie=Cookie.generate(st.getAvatarNullable(),st.getCharacter(),instance,true);
		final JSONObject legacymenu=Modules.getJSONTemplate(st,"menus.main");
		final JSONObject rawresponse=new JSONObject();
		if (st.hasModule("Experience")) {
			final int apremain=abilityPointsRemaining(st);
			if (apremain>0) {
				new Transmission(st.getCharacter(),Modules.getJSONTemplate(st,"characters.spendabilitypoint"),1).start();
			}
		}
		if (!loginmessage.isEmpty()) { rawresponse.put("message",loginmessage); }
		rawresponse.put("incommand","registered");
		rawresponse.put("cookie",cookie);
		rawresponse.put("legacymenu",legacymenu.toString());
		rawresponse.put("messagecount",Message.count(st));
		st.getCharacter().initialConveyances(st,rawresponse);
		rawresponse.put("zoning",ZoneTransport.createZoneTransport(region));
		final String logincommand=st.getKV("Instance.RunOnLogin").value();
		if (logincommand!=null && (!logincommand.isEmpty())) {
			rawresponse.put("logincommand",logincommand);
		}
		// pretty sure initial conveyances does this now.
		//SafeMap convey=Modules.getConveyances(st);
		//for (String key:convey.keySet()) {
		//    rawresponse.put(key,convey.get(key));
		//}
		if (st.getInstance().getOwner().getId()==st.getAvatar().getId()) {
			new BackgroundGroupInviter(st).start();
		}
		return new JSONResponse(rawresponse);
	}*/

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Create a new character",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response create(@Nonnull final State st,
	                              @Nullable @Arguments(type=ArgumentType.TEXT_CLEAN,
	                                                   description="Name of the new character\n \nPLEASE ENTER A NAME ONLY\nNOT A DESCRIPTION OF E.G. "+"SCENT.  YOU MAY GET AN "+"OPPORTUNITY TO DO THIS LATER.\n \nThe name is how your character will be represented, including e.g. "+"people"+" "+"trying to give you XP will need this FULL NAME.  It should JUST be a NAME.",
	                                                   max=40) final String charactername) {
		if (Char.resolve(st,charactername)!=null) {
			final JSONObject json=Modules.getJSONTemplate(st,"characters.create");
			json.put("message","Character name already taken - please retry");
			return new JSONResponse(json);
		}
		if (charactername==null) { return new ErrorResponse("You must enter a name for the new character"); }
		if (charactername.startsWith(">")) {
			return new ErrorResponse("You are not allowed to start a character name with the character >");
		}
		/*
		try {
			final User user=User.findUsernameNullable(charactername,false);
			if (user!=null) {
				if (user!=st.getAvatarNullable()) {
					return new ErrorResponse("You may not name a character after an avatar, other than yourself");
				}
			}
		}
		catch (@Nonnull final NoDataException e) {}
		*/
		final boolean autoname=st.getKV("Instance.AutoNameCharacter").boolValue();
		if (autoname && !st.getAvatar().getName().equalsIgnoreCase(charactername)) {
			return new ErrorResponse("You must name your one and only character after your avatar");
		}
		final int maxchars=st.getKV("Instance.MaxCharacters").intValue();
		if (maxchars<=Char.getCharacters(st.getInstance(),st.getAvatar()).size() && !st.hasPermission("Characters.ExceedCharLimits")) {
			return new ErrorResponse("You are not allowed more than "+maxchars+" active characters");
		}
		final boolean charswitchallowed=st.getKV("Instance.CharacterSwitchEnabled").boolValue();
		if (!charswitchallowed) {
			return new ErrorResponse("You are not allowed to create or switch characters in this location");
		}
		Char.create(st,charactername,true);
		final Char c=Char.resolve(st,charactername);
		Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,c,"Create","Character","",charactername,"Avatar attempted to create character, result: "+c);
		/* deprecated protocol 1
		if (st.json().has("protocol")) {
			if (st.json().getInt("protocol")==2) {

		 */
		if (st.getCharacterNullable()==null) {
			final JSONObject reconnect=new JSONObject();
			reconnect.put("incommand","forcereconnect");
			return new JSONResponse(reconnect);
		}
		else {
			return new OKResponse("New character created and available");
		}
	/*	deprecated protocol 1 behaviour	}
		}
		st.setCharacter(c);
		return login(st,null,null,null);*/
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Switch to a character",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response select(@Nonnull final State st,
	                              @Nullable @Arguments(type=ArgumentType.CHARACTER_PLAYABLE,
	                                                   description="Character to load") final Char character) {
		if (character==null) { return new ErrorResponse("No such character"); }
		if (character.getOwner()!=st.getAvatarNullable()) {
			return new ErrorResponse("That character does not belong to you");
		}
		final boolean charswitchallowed=st.getKV("Instance.CharacterSwitchEnabled").boolValue();
		if (!charswitchallowed) {
			return new ErrorResponse("You are not allowed to switch characters in this location");
		}
		if (character.retired()) {
			return new ErrorResponse("Character '"+character+"' has been retired and can not be selected");
		}
		/* deprecated protocol 1
		if (st.json().has("protocol")) {
			if (st.json().getInt("protocol")==2) {*/
		final String url=st.getCharacter().getURL();
		st.getCharacter().disconnect();
		character.login(st.getAvatar(),st.getRegion(),url);
		st.setCharacter(character);
		character.wipeConveyances(st);
		return Connect.postConnect(st);
			/* deprecated protocol 1
			}
		}
		if (st.getCharacterNullable()!=null) { st.getCharacter().disconnect(); }
		GPHUD.purgeURL(st.callbackurl());
		if (st.getCharacterNullable()!=null) { st.purgeCache(st.getCharacter()); }
		// deprecated PrimaryCharacter.setPrimaryCharacter(st,character);
		// deprecated character.setActive();
		character.setURL(st.callbackurl());
		st.setCharacter(character);
		//GPHUD.purgeURL(st.callbackurl());
		return login(st,null,null,null);*/
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Initialise a character attribute",
	          permitObject=false,
	          permitExternal=false)
	public static Response initialise(@Nonnull final State st,
	                                  @Nonnull @Arguments(type=ArgumentType.ATTRIBUTE,
	                                                      description="Attribute to initialise") final Attribute attribute,
	                                  @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                      description="Value to initialise to",
	                                                      max=4096) final String value) {
		//System.out.println("Initialise "+attribute+" to "+value);
		final boolean debug=false;
		switch (attribute.getType()) {
			case FLOAT:
			case INTEGER:
			case TEXT:
				// its a KV.  check it has no KV already.
				final String existingvalue=st.getRawKV(st.getCharacter(),"characters."+attribute.getName());
				if (existingvalue!=null) {
					return new ErrorResponse("Can not initialise a non null value (currently:"+existingvalue+")");
				}
				// does it exceed the max, if a max is configured, or anything
				if (attribute.getType()==Attribute.ATTRIBUTETYPE.FLOAT || attribute.getType()==Attribute.ATTRIBUTETYPE.INTEGER) {
					final KVValue maxkv=st.getKV("characters."+attribute.getName()+"MAX");
					Float max=null;
					//System.out.println("Checking bounds on "+attribute.getName()+" of type "+attribute.getType()+" with value "+value+" and max "+maxkv);
					if (!maxkv.value().isEmpty()) { max=maxkv.floatValue(); }
					//System.out.println("Max is "+max);
					if (max!=null && max>0) {
						//System.out.println("About to check "+max+" > "+Float.parseFloat(value));
						if (Float.parseFloat(value)>max) {
							final JSONObject json=new JSONObject();
							json.put("hudtext","Initialising character...")
							    .put("hudcolor","<1.0,0.75,0.75>")
							    .put("titlertext","Initialising character...")
							    .put("titlercolor","<1.0,0.75,0.75>")
							    .put("message","Character creation requires you to input attribute "+attribute.getName()+" WHICH MUST BE NO MORE THAN "+max);
							json.put("incommand","runtemplate");
							json.put("invoke","characters.initialise");
							json.put("args","1");
							json.put("attribute",attribute.getName());
							json.put("arg0name","value");
							json.put("arg0description","You must select a "+attribute.getName()+" for your Character before you can use it (no greater than "+max+")");
							json.put("arg0type","TEXTBOX");
							return new JSONResponse(json);
						}
					}
				}
				st.setKV(st.getCharacter(),"characters."+attribute.getName(),value);
				break;
			case GROUP:
				// its a group... check user has no group already
				if (attribute.getSubType()!=null) {
					final CharacterGroup group=CharacterGroup.getGroup(st,attribute.getSubType());
					if (group!=null) {
						return new ErrorResponse("You already have membership of '"+group.getNameSafe()+"' which is of type "+attribute.getSubType());
					}
				}
				// check the target group is of the right type
				final CharacterGroup target=CharacterGroup.resolve(st,value);
				if (target==null) { return new ErrorResponse("Unable to find the requested group "+value); }
				final String targettype=target.getType();
				if (targettype==null) {
					return new ErrorResponse("Group "+target.getNameSafe()+" is not a typed group");
				}
				if (!targettype.equals(attribute.getSubType())) {
					return new ErrorResponse("Group "+target.getNameSafe()+" is of type "+target.getType()+" rather than the required "+attribute.getSubType()+" required by "+"attribute "+attribute
							.getName());
				}
				// check the group is open
				if (!target.isOpen()) {
					return new ErrorResponse("You can not join group "+target.getNameSafe()+" of type "+target.getType()+", it is not open for joining");
				}
				target.addMember(st.getCharacter());
				break;

			case POOL:
				throw new UserInputStateException("Attempt to initialise pool attribute is invalid.");
		}
		Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,st.getCharacter(),"Initialise",attribute.getName(),null,value,"Character creation initialised attribute");
		/* deprecated protocol 1
		if (st.json().has("protocol")) {
			if (st.json().getInt("protocol")==2) {

		 */
		if (st.getCharacterNullable()==null) {
			final JSONObject reconnect=new JSONObject();
			reconnect.put("incommand","forcereconnect");
			return new JSONResponse(reconnect);
		}
		else {
			return Connect.postConnect(st);
		}
				/* deprecated protocol 1
			}
		}
		return login(st,null,null,null);
				 */
	}
}
