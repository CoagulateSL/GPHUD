package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Instance.Distribution;
import net.coagulate.GPHUD.Modules.KVValue;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Zoning.ZoneTransport;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONException;
import org.json.JSONObject;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static net.coagulate.GPHUD.Modules.Characters.CharactersModule.abilityPointsRemaining;

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
	@Commands(context = Context.AVATAR, permitConsole = false, permitUserWeb = false, permitHUDWeb = false, description = "Register this session as a character connection")
	public static Response login(State st,
	                             @Arguments(type = ArgumentType.TEXT_ONELINE, description = "Version number of the HUD that is connecting", max = 128)
			                             String version,
	                             @Arguments(type = ArgumentType.TEXT_ONELINE, description = "Version date of the HUD that is connecting", max = 128)
			                             String versiondate,
	                             @Arguments(type = ArgumentType.TEXT_ONELINE, description = "Version time of the HUD that is connecting", max = 128)
			                             String versiontime
	) throws UserException, SystemException {
		final boolean debug = false;
		////// CHANGE ALL THIS, HAVE A
		////// "USINGCHARACTER" COLUMN FOR <AVATAR> TYPES
		////// THIS IS IMPORTANT TO RESOLVE A TARGET FROM AN AVATAR LATER WHEN TARGETTING FOR EXAMPLE

		String url = null;
		if (st.json != null) { try { url = st.json.getString("callback"); } catch (JSONException e) {} }
		if (url == null || "".equals(url)) {
			st.logger().log(WARNING, "No callback URL sent with character registration");
			return new ErrorResponse("You are not set up with a callback URL");
		}
		boolean autocreate = st.getKV("Instance.AutoNameCharacter").boolValue();
		Char character = PrimaryCharacters.getPrimaryCharacter(st, autocreate);
		if (character == null) {
			if (autocreate) {
				throw new UserException("Failed to get/create a character for user " + st.avatar());
			} // autocreate or die :P
			// if not auto create, offer "characters.create" i guess
			JSONResponse response = new JSONResponse(Modules.getJSONTemplate(st, "characters.create"));
			response.asJSON(st).put("hudtext", "Creating character...").put("hudcolor", "<1.0,0.75,0.75>").put("titlertext", "Creating character...").put("titlercolor", "<1.0,0.75,0.75>").put("message", "Welcome.  You do not have any characters, please create a new one.");
			return response;
		}
		// we have a character at least
		// before actually logging it in, we should check that it is 'complete'
		State simulate = st.simulate(character);
		for (Attribute a : st.getAttributes()) {
			if (a.getRequired()) {
				Attribute.ATTRIBUTETYPE type = a.getType();
				switch (type) {
					case TEXT: // mandatory text doesn't work at this time
					case FLOAT:
					case INTEGER:
						String value = simulate.getRawKV(character,"characters." + a.getName());
						if (value == null || value.isEmpty()) {
							if (debug) {
								System.out.println("Character " + character + " fails validation check for input " + a);
							}
							KVValue maxkv=st.getKV("characters."+a.getName()+"MAX");
							Float max=null;
							if (maxkv!=null && !maxkv.value().isEmpty()) { max=maxkv.floatValue(); }
							String maxstring="";
							if (max!=null && max>0) { maxstring=", which must be no greater than "+max; }
							JSONObject json = new JSONObject();
							json.put("hudtext", "Initialising character...").put("hudcolor", "<1.0,0.75,0.75>")
									.put("titlertext", "Initialising character...").put("titlercolor", "<1.0,0.75,0.75>")
									.put("message", "Character creation requires you to input attribute " + a.getName()+maxstring);
							json.put("incommand", "runtemplate");
							json.put("invoke", "characters.initialise");
							json.put("args", "1");
							json.put("attribute", a.getName());
							json.put("arg0name","value");
							json.put("arg0description", "You must select a " + a.getName() + " for your Character before you can use it"+maxstring);
							json.put("arg0type","TEXTBOX");
							if (debug) { System.out.println("Choice JSON : " + json.toString()); }
							return new JSONResponse(json);
						} else {
							if (debug) {
								System.out.println("Character " + character + " passes validation check for input " + a);
							}
						}
						break;
					case GROUP:
						if (character.getGroup(a.getSubType()) == null) {
							if (debug) {
								System.out.println("Character " + character + " fails validation check for group " + a);
							}
							JSONObject json = new JSONObject();
							json.put("hudtext", "Initialising character...").put("hudcolor", "<1.0,0.75,0.75>")
									.put("titlertext", "Initialising character...").put("titlercolor", "<1.0,0.75,0.75>")
									.put("message", "Character creation requires you to select a choice for attribute " + a.getName());
							CharacterGroup.createChoice(st, json, "arg0", a);
							json.put("incommand", "runtemplate");
							json.put("invoke", "characters.initialise");
							json.put("args", "1");
							json.put("attribute", a.getName());
							json.put("arg0description", "You must select a " + a.getName() + " for your Character before you can use it");
							if (debug) { System.out.println("Choice JSON : " + json.toString()); }
							return new JSONResponse(json);
						} else {
							if (debug) {
								System.out.println("Character " + character + " passes validation check for group " + a);
							}
						}
						break;
					case POOL:
						//System.out.println("Character "+character+" validation check for pool "+a+" has no currently defined meaning.  NO-OP.  Passing check.");
						break;
					default:
						throw new SystemException("Unhandled attribute type " + type);
				}
			}
		}
		// AND LOGIN
		st.setCharacter(character);
		st.logger().log(INFO, "Logging in as " + character);
        /*
        String loginmessage="Welcome back, "+character.getName();
        if (!character.getName().equals(st.avatar().getName())) { loginmessage+=" ["+st.avatar().getName()+"]"; }
        loginmessage+="\n\n";
        loginmessage+="Instance MOTD goes here";
        */
		String oldavatarurl = st.getCharacter().getURL();
		if (oldavatarurl != null && !oldavatarurl.equals(url)) {
			JSONObject shutdownjson = new JSONObject().put("incommand", "shutdown").put("shutdown", "Replaced by new registration");
			Transmission shutdown = new Transmission((Char) null, shutdownjson, oldavatarurl);
			shutdown.start();
		}
		String oldurl = character.getURL();
		st.getCharacter().setURL(url);
		Region region = st.getRegion();
		st.getCharacter().setRegion(region);
		JSONObject registeringjson = new JSONObject().put("incommand", "registering");
		String regmessage = "";
		if (st.getInstance().getOwner().getId() == st.getAvatar().getId()) {
			// is instance owner
			regmessage = GPHUD.serverVersion() + " https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head";
			if (st.getRegion().needsUpdate()) {
				regmessage += "\n=====\nUpdate required: A new GPHUD Region Server has been released and is being sent to you, please place it near the existing one.  The old one will then disable its self and can be deleted.\n=====";
				Distribution.getServer(st);
			}
		} else {
			//regmessage="O:"+st.getInstance().getOwner().getId()+" U:"+st.getCharacter().getId()+" "+GPHUD.serverVersion();
			regmessage = GPHUD.serverVersion();
		}
		registeringjson.put("message", regmessage);
		Transmission registering = new Transmission((Char) null, registeringjson, url);
		registering.run(); // note null char to prevent it sticking payloads here, it clears the titlers :P
		Visits.initVisit(st, st.getCharacter(), region);
		if (version != null && versiondate != null && versiontime != null) {
			region.recordHUDVersion(st, version, versiondate, versiontime);
		}
		character.setPlayedBy(st.avatar());
		Instance instance = st.getInstance();
		String cookie = Cookies.generate(st.avatar(), st.getCharacter(), instance, true);
		JSONObject legacymenu = Modules.getJSONTemplate(st, "menus.main");
		JSONObject rawresponse = new JSONObject();
		if (st.hasModule("Experience")) {
			int apremain = abilityPointsRemaining(st);
			if (apremain > 0) {
				new Transmission(st.getCharacter(), Modules.getJSONTemplate(st, "characters.spendabilitypoint"), 1).start();
			}
		}
		//rawresponse.put("message",loginmessage);
		rawresponse.put("incommand", "registered");
		rawresponse.put("cookie", cookie);
		rawresponse.put("legacymenu", legacymenu.toString());
		rawresponse.put("messagecount", st.getCharacter().messages());
		st.getCharacter().initialConveyances(st, rawresponse);
		rawresponse.put("zoning", ZoneTransport.createZoneTransport(region));
		// pretty sure initial conveyances does this now.
		//SafeMap convey=Modules.getConveyances(st);
		//for (String key:convey.keySet()) {
		//    rawresponse.put(key,convey.get(key));
		//}
		return new JSONResponse(rawresponse);
	}

	@Commands(context = Context.AVATAR, description = "Create a new character")
	public static Response create(State st,
	                              @Arguments(type = ArgumentType.TEXT_CLEAN, description = "Name of the new character\n \nPLEASE ENTER A NAME ONLY\nNOT A DESCRIPTION OF E.G. SCENT.  YOU MAY GET AN OPPORTUNITY TO DO THIS LATER.\n \nThe name is how your character will be represented, including e.g. people trying to give you XP will need this FULL NAME.  It should JUST be a NAME.", max = 40)
			                              String charactername) {
		if (Char.resolve(st, charactername) != null) {
			JSONObject json = Modules.getJSONTemplate(st, "characters.create");
			json.put("message", "Character name already taken - please retry");
			return new JSONResponse(json);
		}
		if (charactername.startsWith(">")) {
			return new ErrorResponse("You are not allowed to start a character name with the character >");
		}
		try {
			User user = User.findOptional(charactername);
			if (user != null) {
				if (user != st.avatar()) {
					return new ErrorResponse("You may not name a character after an avatar, other than yourself");
				}
			}
		} catch (NoDataException e) {}
		boolean autoname = st.getKV("Instance.AutoNameCharacter").boolValue();
		if (autoname && !st.avatar().getName().equalsIgnoreCase(charactername)) {
			return new ErrorResponse("You must name your one and only character after your avatar");
		}
		int maxchars = st.getKV("Instance.MaxCharacters").intValue();
		if (maxchars <= Char.getCharacters(st.getInstance(), st.avatar()).size() && !st.hasPermission("Characters.ExceedCharLimits")) {
			return new ErrorResponse("You are not allowed more than " + maxchars + " active characters");
		}
		boolean charswitchallowed = st.getKV("Instance.CharacterSwitchEnabled").boolValue();
		if (!charswitchallowed) {
			return new ErrorResponse("You are not allowed to create or switch characters in this location");
		}
		Char.create(st, charactername);
		Char c = Char.resolve(st, charactername);
		Audit.audit(true, st, Audit.OPERATOR.AVATAR, null, c, "Create", "Character", "", charactername, "Avatar attempted to create character, result: " + c);
		return login(st, null, null, null);
	}

	@Commands(context = Context.AVATAR, description = "Switch to a character")
	public static Response select(State st,
	                              @Arguments(type = ArgumentType.CHARACTER_PLAYABLE, description = "Character to load")
			                              Char character) {
		if (character == null) { return new ErrorResponse("No such character"); }
		if (character.getOwner() != st.avatar()) { return new ErrorResponse("That character does not belong to you"); }
		boolean charswitchallowed = st.getKV("Instance.CharacterSwitchEnabled").boolValue();
		if (!charswitchallowed) {
			return new ErrorResponse("You are not allowed to create or switch characters in this location");
		}
		if (character.retired()) {
			return new ErrorResponse("Character '" + character + "' has been retired and can not be selected");
		}
		GPHUD.purgeURL(st.callbackurl);
		if (st.getCharacterNullable() != null) { st.purgeCache(st.getCharacter()); }
		PrimaryCharacters.setPrimaryCharacter(st, character);
		return login(st, null, null, null);
	}

	@Commands(context = Context.AVATAR, description = "Initialise a character attribute")
	public static Response initialise(State st,
	                                  @Arguments(type = ArgumentType.ATTRIBUTE, description = "Attribute to initialise")
			                                  Attribute attribute,
	                                  @Arguments(type = ArgumentType.TEXT_ONELINE, description = "Value to initialise to", max = 4096)
			                                  String value) {
		//System.out.println("Initialise "+attribute+" to "+value);
		final boolean debug=true;
		switch (attribute.getType()) {
			case FLOAT:
			case INTEGER:
			case TEXT:
				// its a KV.  check it has no KV already.
				String existingvalue = st.getRawKV(st.getCharacter(),"characters."+attribute.getName());
				if (existingvalue != null) {
					return new ErrorResponse("Can not initialise a non null value (currently:" + existingvalue + ")");
				}
				// does it exceed the max, if a max is configured, or anything
				if (attribute.getType()== Attribute.ATTRIBUTETYPE.FLOAT || attribute.getType()== Attribute.ATTRIBUTETYPE.INTEGER) {
					KVValue maxkv=st.getKV("characters."+attribute.getName()+"MAX");
					Float max=null;
					if (debug) { System.out.println("Checking bounds on "+attribute.getName()+" of type "+attribute.getType()+" with value "+value+" and max "+maxkv); }
					if (maxkv!=null && !maxkv.value().isEmpty()) { max=maxkv.floatValue(); }
					if (debug) { System.out.println("Max is "+max); }
					if (max!=null && max>0) {
						if (debug) { System.out.println("About to check "+max+" > "+Float.parseFloat(value)); }
						if (Float.parseFloat(value)>max) {
							JSONObject json = new JSONObject();
							json.put("hudtext", "Initialising character...").put("hudcolor", "<1.0,0.75,0.75>")
									.put("titlertext", "Initialising character...").put("titlercolor", "<1.0,0.75,0.75>")
									.put("message", "Character creation requires you to input attribute " + attribute.getName()+" WHICH MUST BE NO MORE THAN "+max);
							json.put("incommand", "runtemplate");
							json.put("invoke", "characters.initialise");
							json.put("args", "1");
							json.put("attribute", attribute.getName());
							json.put("arg0name","value");
							json.put("arg0description", "You must select a " + attribute.getName() + " for your Character before you can use it (no greater than "+max+")");
							json.put("arg0type","TEXTBOX");
							return new JSONResponse(json);
						}
					}
				}
				st.setKV(st.getCharacter(), "characters."+attribute.getName(), value);
				break;
			case GROUP:
				// its a group... check user has no group already
				CharacterGroup group = st.getCharacter().getGroup(attribute.getSubType());
				if (group != null) {
					return new ErrorResponse("You already have membership of '" + group.getNameSafe() + "' which is of type " + attribute.getSubType());
				}
				// check the target group is of the right type
				CharacterGroup target = CharacterGroup.resolve(st, value);
				if (target == null) { return new ErrorResponse("Unable to find the requested group " + value); }
				if (!target.getType().equals(attribute.getSubType())) {
					return new ErrorResponse("Group " + target.getNameSafe() + " is of type " + target.getType() + " rather than the required " + attribute.getSubType() + " required by attribute " + attribute.getName());
				}
				target.addMember(st.getCharacter());
				break;
			case POOL:
				throw new UserException("Attempt to initialise pool attribute is invalid.");
		}
		Audit.audit(true, st, Audit.OPERATOR.AVATAR, null, st.getCharacter(), "Initialise", attribute.getName(), null, value, "Character creation initialised attribute");
		return login(st, null, null, null);
	}
}
