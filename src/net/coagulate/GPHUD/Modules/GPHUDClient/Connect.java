package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.EndOfLifing;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
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

	private Connect() {}

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
		if (EndOfLifing.hasExpired(version)) {
			st.logger().warning("Rejected HUD connection from end-of-life product version "+version+" from "+versiondate+" "+versiontime);
			return new TerminateResponse("Sorry, this HUD is so old it is no longer supported.\nPlease tell your sim administrator to deploy an update.");
		}
		st.json(); // ensure we have the jsons
		// log client version
		if (!version.isEmpty() && !versiondate.isEmpty() && !versiontime.isEmpty()) {
			st.getRegion().recordHUDVersion(st,version,versiondate,versiontime);
		}
		// forcibly invite instance owners to group
		if (st.getInstance().getOwner().getId()==st.getAvatar().getId()) {
			new BackgroundGroupInviter(st).start();
		}
		// try find a character, or auto create
		final boolean autocreate=st.getKV("Instance.AutoNameCharacter").boolValue();
		Char character=Char.getMostRecent(st.getAvatar(),st.getInstance());
		if (character==null) {
			if (autocreate) {
				character=Char.autoCreate(st);
			} // autocreate or die :P
			// if not auto create, offer "characters.create" which will order the HUD to relog if there's no active character (relog=call us)
			else {
				final JSONResponse response=new JSONResponse(Modules.getJSONTemplate(st,"characters.create"));
				response.asJSON(st)
				        .put("hudtext","Creating character...")
				        .put("hudcolor","<1.0,0.75,0.75>")
				        .put("titlertext","Creating character...")
				        .put("titlercolor","<1.0,0.75,0.75>")
				        .put("message","Welcome.  You do not have any characters, please create a new one.");
				return response;
			}
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
		final List<String> loginmessages=new ArrayList<>();

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
		// rawresponse.put("legacymenu",Modules.getJSONTemplate(st,"menus.main").toString());
		// and maybe now it is (?)

		// dump the messages
		final StringBuilder message=new StringBuilder();
		for (final String amessage: loginmessages) {
			if (message.length()>0) { message.append("\n"); }
			message.append(amessage);
		}
		if (message.length()>0) { rawresponse.put("message",message.toString()); }
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
		// purge some things we forced upon the character so their conveyances might refresh
		st.getCharacter().wipeConveyance(st,"hudtext");
		st.getCharacter().wipeConveyance(st,"hudcolor");
		st.getCharacter().wipeConveyance(st,"titlertext");
		st.getCharacter().wipeConveyance(st,"titlercolor");
		return new JSONResponse(rawresponse);
	}

	// ----- Internal Statics -----
	@Nullable
	private static Response populateCharacterAttributes(@Nonnull final State st) {
		for (final Attribute a: st.getAttributes()) {
			boolean required=a.getRequired();
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) { required=true; }
			if (required) {
				final Attribute.ATTRIBUTETYPE type=a.getType();
				switch (type) {
					case TEXT:
					case FLOAT:
					case INTEGER:
						final String value=st.getRawKV(st.getCharacter(),"characters."+a.getName());
						if (value==null || value.isEmpty()) { return requestTextInput(st,a); }
						break;
					case GROUP:
						if (a.getSubType()!=null && CharacterGroup.getGroup(st.getCharacter(),a.getSubType())==null && CharacterGroup.hasChoices(st,a)) {
							return requestChoiceInput(st,a);
						}
						break;
					case CURRENCY:
						if (st.hasModule("Currency")) {
							final Currency currency=Currency.findNullable(st,a.getName());
							if (currency!=null && currency.entries(st,st.getCharacter())==0 && a.getDefaultValue()!=null && !a.getDefaultValue().isEmpty()) {
								final int ammount=Integer.parseInt(a.getDefaultValue());
								currency.spawnInAsSystem(st,st.getCharacter(),ammount,"Starting balance issued");
							}
						}
						break;
					case POOL:
						break;
					default:
						throw new SystemConsistencyException("Unhandled attribute type "+type);
				}
			}
		}
		return null;
	}

	@Nonnull
	private static Response requestChoiceInput(@Nonnull final State st,
	                                           @Nonnull final Attribute a) {
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

	@Nonnull
	private static Response requestTextInput(@Nonnull final State st,
	                                         @Nonnull final Attribute a) {
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

	@Nullable
	private static Response runCharacterInitScript(final State st,
	                                               final List<String> loginmessages) {
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

}
