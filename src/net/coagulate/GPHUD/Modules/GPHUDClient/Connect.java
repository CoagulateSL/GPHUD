package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Characters.BackgroundGroupInviter;
import net.coagulate.GPHUD.Modules.Characters.CharactersModule;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Instance.Distribution;
import net.coagulate.GPHUD.Modules.KVValue;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Zoning.ZoneTransport;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class Connect {

	/**
	 * Connects GPHUD
	 */
	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.AVATAR,
	          permitConsole=false,
	          permitUserWeb=false,
	          permitScripting=false,
	          description="Bind a running GPHUD HUD to a particular character, potentially auto creating, or prompting for naming if auto create is off",
	          permitObject=false,
	          permitExternal=false)
	public static Response connect(@Nonnull final State st,
	                               @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                   description="Version number of the HUD that is connecting",
	                                                   max=128) final String version,
	                               @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                   description="Version date of the HUD that is connecting",
	                                                   max=128) final String versiondate,
	                               @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                   description="Version time of the HUD that is connecting",
	                                                   max=128) final String versiontime,
	                               @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                   description="URL for the client",
	                                                   max=256) final String url,
	                               @Nonnull @Arguments(type=ArgumentType.INTEGER,
	                                                   description="Resume session for character id") final Integer characterid) {
		st.json(); // ensure we have the jsons
		// log client version
		if (version!=null && versiondate!=null && versiontime!=null && !version.isEmpty() && !versiondate.isEmpty() && !versiontime.isEmpty()) {
			st.getRegion().recordHUDVersion(st,version,versiondate,versiontime);
		}
		// forcibly invite instance owners to group
		if (st.getInstance().getOwner().getId()==st.getAvatar().getId()) {
			new BackgroundGroupInviter(st).start();
		}
		// try find a character, or auto create
		final boolean autocreate=st.getKV("Instance.AutoNameCharacter").boolValue();
		System.out.println("About to get most recent for "+st.getAvatar()+" at "+st.getInstance()+", autocreate is "+autocreate);
		Char character=Char.getMostRecent(st.getAvatar(),st.getInstance());
		if (character==null) {
			if (autocreate) {
				character=Char.autoCreate(st);
				if (character==null) {
					throw new UserInputStateException("Failed to get/create a character for user "+st.getAvatarNullable());
				}
			} // autocreate or die :P
			// if not auto create, offer "characters.create" which will order the HUD to relog if there's no active character (relog=call us)
			final JSONResponse response=new JSONResponse(Modules.getJSONTemplate(st,"characters.create"));
			response.asJSON(st)
			        .put("hudtext","Creating character...")
			        .put("hudcolor","<1.0,0.75,0.75>")
			        .put("titlertext","Creating character...")
			        .put("titlercolor","<1.0,0.75,0.75>")
			        .put("message","Welcome.  You do not have any characters, please create a new one.");
			return response;
		}
		// connect the character we found, which disconnects the avatar from other characters and closes old URLs if its a restart
		character.login(st.getAvatar(),st.getRegion(),url);
		// set up the state so postConnect can do stuff
		st.setCharacter(character);
		// IS this the same as the old character?
		if (character.getId()==characterid) { return new OKResponse("GPHUD Connection Re-established"); }
		// and purge the conveyances so they get re-set
		character.wipeConveyances(st);
		// chain postConnect
		return postConnect(st);
	}


	@Nonnull
	@Commands(context=Context.AVATAR,
	          permitConsole=false,
	          permitUserWeb=false,
	          permitScripting=false,
	          description="Performs character login checks, called repeatedly until character passes all creation time tests and is considered complete",
	          permitObject=false,
	          permitExternal=false)
	public static Response postConnect(@Nonnull final State st) {
		List<String> loginmessages=new ArrayList<>();

		// Run the character initialisation script, if it exists.
		Response interception=runCharacterInitScript(st,loginmessages);
		if (interception!=null) { return interception; }
		// and the default attribute populator
		interception=populateCharacterAttributes(st);
		if (interception!=null) { return interception; }

		// server version note
		loginmessages.add(GPHUD.serverVersion()+" [https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head Release Notes]");

		// if instance owner and region version is out of date, send update and message
		if (st.getInstance().getOwner().getId()==st.getAvatar().getId()) {
			if (st.getRegion().needsUpdate()) {
				loginmessages.add(
						"Update required: A new GPHUD Region Server has been released and is being sent to you, please place it near the existing one.  The old one will then disable its self and can be deleted.");
				Distribution.getServer(st);
			}
		}

		// start a player visit
		Visit.initVisit(st,st.getCharacter(),st.getRegion());

		// create the post create "all ok" response, it's a blank object by default
		JSONObject rawresponse=new JSONObject();

		// but we might populate it with the spend ability point command
		if (st.hasModule("Experience")) {
			final int apremain=CharactersModule.abilityPointsRemaining(st);
			if (apremain>0) {
				rawresponse=Modules.getJSONTemplate(st,"characters.spendabilitypoint");
			}
		}

		// we dump the main menu this way for now, seems like it could be a conveyance too.
		rawresponse.put("legacymenu",Modules.getJSONTemplate(st,"menus.main").toString());

		// dump the messages
		String message="";
		for (String amessage: loginmessages) {
			if (!message.isEmpty()) { message+="\n"; }
			message+=amessage;
		}
		if (!message.isEmpty()) { rawresponse.put("message",message); }
		// update message count
		rawresponse.put("messagecount",Message.count(st));
		// send zoning information
		rawresponse.put("zoning",ZoneTransport.createZoneTransport(st.getRegion()));
		// and if there's a login command, do that too
		final String logincommand=st.getKV("Instance.RunOnLogin").value();
		if (logincommand!=null && (!logincommand.isEmpty())) {
			rawresponse.put("logincommand",logincommand);
		}
		// and tell the HUD we're all great
		rawresponse.put("logincomplete",st.getCharacter().getId());
		Effect.conveyEffects(st,st.getCharacter(),rawresponse);
		return new JSONResponse(rawresponse);
	}

	// ----- Internal Statics -----
	@Nullable
	private static Response populateCharacterAttributes(@Nonnull final State st) {
		for (final Attribute a: st.getAttributes()) {
			if (a.getRequired()) {
				final Attribute.ATTRIBUTETYPE type=a.getType();
				switch (type) {
					case TEXT:
					case FLOAT:
					case INTEGER:
						final String value=st.getRawKV(st.getCharacter(),"characters."+a.getName());
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
						if (a.getSubType()!=null && CharacterGroup.getGroup(st.getCharacter(),a.getSubType())==null && CharacterGroup.hasChoices(st,a)) {
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
					case POOL:
						//System.out.println("Character "+character+" validation check for pool "+a+" has no currently defined meaning.  NO-OP.  Passing check.");
						break;
					default:
						throw new SystemConsistencyException("Unhandled attribute type "+type);
				}
			}
		}
		return null;
	}

	@Nullable
	private static Response runCharacterInitScript(State st,List<String> loginmessages) {
		final String initscript=st.getKV("Instance.CharInitScript").toString();
		if (initscript!=null && (!initscript.isEmpty())) {
			// let the init script have a "run"
			final Script init=Script.findNullable(st,initscript);
			if (init==null) {
				loginmessages.add("===> Character initialisation script "+initscript+" was not found");
				return null;
			}
			else {
				final GSVM initialisecharacter=new GSVM(init.getByteCode());
				initialisecharacter.invokeOnExit("GPHUDClient.postConnect");
				final Response response=initialisecharacter.execute(st);
				if (initialisecharacter.suspended()) { // bail here
					return response;
				} // else carry on and discard the response
			}
		}
		return null;
	}

	/*
	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Create a new character",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response create(@Nonnull final State st,
	                              @Nullable
	                              @Arguments(type=ArgumentType.TEXT_CLEAN,
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
		try {
			final User user=User.findOptional(charactername);
			if (user!=null) {
				if (user!=st.getAvatarNullable()) {
					return new ErrorResponse("You may not name a character after an avatar, other than yourself");
				}
			}
		}
		catch (@Nonnull final NoDataException e) {}
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
		return login(st,null,null,null);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Switch to a character",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response select(@Nonnull final State st,
	                              @Nullable
	                              @Arguments(type=ArgumentType.CHARACTER_PLAYABLE,
	                                         description="Character to load") final Char character) {
		if (character==null) { return new ErrorResponse("No such character"); }
		if (character.getOwner()!=st.getAvatarNullable()) {
			return new ErrorResponse("That character does not belong to you");
		}
		final boolean charswitchallowed=st.getKV("Instance.CharacterSwitchEnabled").boolValue();
		if (!charswitchallowed) {
			return new ErrorResponse("You are not allowed to create or switch characters in this location");
		}
		if (character.retired()) {
			return new ErrorResponse("Character '"+character+"' has been retired and can not be selected");
		}
		GPHUD.purgeURL(st.callbackurl());
		if (st.getCharacterNullable()!=null) { st.purgeCache(st.getCharacter()); }
		PrimaryCharacter.setPrimaryCharacter(st,character);
		//character.setURL(st.callbackurl());
		//GPHUD.purgeURL(st.callbackurl());
		return login(st,null,null,null);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Initialise a character attribute",
	          permitObject=false,
	          permitExternal=false)
	public static Response initialise(@Nonnull final State st,
	                                  @Nonnull
	                                  @Arguments(type=ArgumentType.ATTRIBUTE,
	                                             description="Attribute to initialise") final Attribute attribute,
	                                  @Nonnull
	                                  @Arguments(type=ArgumentType.TEXT_ONELINE,
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
					System.out.println("Checking bounds on "+attribute.getName()+" of type "+attribute.getType()+" with value "+value+" and max "+maxkv);
					if (!maxkv.value().isEmpty()) { max=maxkv.floatValue(); }
					System.out.println("Max is "+max);
					if (max!=null && max>0) {
						System.out.println("About to check "+max+" > "+Float.parseFloat(value));
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
		return login(st,null,null,null);
	}

	 */
}
