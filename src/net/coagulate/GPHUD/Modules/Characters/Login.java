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

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Create a new character",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response create(@Nonnull final State st,
	                              @Nullable @Arguments(type=ArgumentType.TEXT_CLEAN,
	                                                   name="charactername",description="Name of the new character\n \nPLEASE ENTER A NAME ONLY\nNOT A DESCRIPTION OF E.G. "+"SCENT.  YOU MAY GET AN "+"OPPORTUNITY TO DO THIS LATER.\n \nThe name is how your character will be represented, including e.g. "+"people"+" "+"trying to give you XP will need this FULL NAME.  It should JUST be a NAME.",
	                                                   max=40) final String charactername) {
		if (Char.resolve(st,charactername)!=null) {
			final JSONObject json=Modules.getJSONTemplate(st,"characters.create");
			JSONResponse.message(json,"Character name already taken - please retry");
			return new JSONResponse(json);
		}
		if (charactername==null) { return new ErrorResponse("You must enter a name for the new character"); }
		if (charactername.startsWith(">")) {
			return new ErrorResponse("You are not allowed to start a character name with the character >");
		}

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

		if (st.getCharacterNullable()==null) {
			final JSONObject reconnect=new JSONObject();
			reconnect.put("incommand","forcereconnect");
			return new JSONResponse(reconnect);
		}
		else {
			return new OKResponse("New character created and available");
		}

	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Switch to a character",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response select(@Nonnull final State st,
	                              @Nullable @Arguments(type=ArgumentType.CHARACTER_PLAYABLE,
	                                                   name="character",description="Character to load") final Char character) {
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

		final String url=st.getCharacter().getURL();
		st.getCharacter().disconnect();
		character.login(st.getAvatar(),st.getRegion(),url);
		st.setCharacter(character);
		character.wipeConveyances(st);
		return Connect.postConnect(st);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Initialise a character attribute",
	          permitObject=false,
	          permitExternal=false)
	public static Response initialise(@Nonnull final State st,
	                                  @Nonnull @Arguments(type=ArgumentType.ATTRIBUTE,
	                                                      name="attribute",description="Attribute to initialise") final Attribute attribute,
	                                  @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                      name="value",description="Value to initialise to",
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
							    .put("titlercolor","<1.0,0.75,0.75>");
							JSONResponse.message(json,"Character creation requires you to input attribute "+attribute.getName()+" WHICH MUST BE NO MORE THAN "+max);
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

		if (st.getCharacterNullable()==null) {
			final JSONObject reconnect=new JSONObject();
			reconnect.put("incommand","forcereconnect");
			return new JSONResponse(reconnect);
		}
		else {
			return Connect.postConnect(st);
		}
	}
}
