package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.Data.Landmark;
import net.coagulate.GPHUD.Data.Obj;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.GPHUDClient.OpenWebsite;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class Output {
	private Output() {
	}
	
	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the character",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+
	                                   ">String - message to speak",
	                        returns="Integer - The number 0",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsChar(final State st,
	                                    @Nonnull final GSVM vm,
	                                    @Nonnull final BCCharacter target,
	                                    @Nonnull final BCString message) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		vm.queueSayAs(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the HUD",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+
	                                   ">String - message to speak",
	                        returns="Integer - The number 0",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsHUD(final State st,
	                                   @Nonnull final GSVM vm,
	                                   @Nonnull final BCCharacter target,
	                                   @Nonnull final BCString message) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		vm.queueSay(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to send a message to its wearer",
	                        parameters="Character - character whose HUD will message the wearer (the character "+
	                                   "themselves)<br>String - message to pass",
	                        returns="Integer - The number 0",
	                        notes="Messages are stacked up, per user, until the script completes or is "+"suspended",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayToChar(final State st,
	                                    @Nonnull final GSVM vm,
	                                    @Nonnull final BCCharacter target,
	                                    @Nonnull final BCString message) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		vm.queueOwnerSay(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Teleports the player",
	                        parameters="BCCharacter - who to teleport<br>BCString - Landmark name to teleport to",
	                        returns="Integer - 0",
	                        notes="",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsTeleport(@Nonnull final State st,
	                                   @Nonnull final GSVM vm,
	                                   @Nonnull final BCCharacter target,
	                                   @Nonnull final BCString landmark) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		GSFunctions.assertModule(st,"Teleportation");
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		final Landmark t=Landmark.find(st,landmark.getContent());
		if (t==null) {
			throw new GSResourceUnavailableException("Can not find landmark "+landmark.getContent());
		}
		vm.queueTeleport(target.getContent(),t.getHUDRepresentation(false));
		return new BCInteger(null,0);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the character, if online, otherwise silently returns",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+
	                                   ">String - message to speak",
	                        returns="Integer - 1 if the message was sent, 0 if not",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsCharIfOnline(final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString message) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		if (!target.isOnline()) {
			return new BCInteger(null,0);
		}
		vm.queueSayAs(target.getContent(),message.getContent());
		return new BCInteger(null,1);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the HUD, if online, otherwise silently returns",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+
	                                   ">String - message to speak",
	                        returns="Integer - 1 if the message was sent, 0 if not",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsHUDIfOnline(final State st,
	                                           @Nonnull final GSVM vm,
	                                           @Nonnull final BCCharacter target,
	                                           @Nonnull final BCString message) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		if (!target.isOnline()) {
			return new BCInteger(null,0);
		}
		vm.queueSay(target.getContent(),message.getContent());
		return new BCInteger(null,1);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to send a message to its wearer, if online, otherwise silently returns",
	                        parameters="Character - character whose HUD will message the wearer (the character "+
	                                   "themselves)<br>String - message to pass",
	                        returns="Integer - 1 if the message was sent, 0 if not",
	                        notes="Messages are stacked up, per user, until the script completes or is "+"suspended",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayToCharIfOnline(final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString message) {
		if (vm.simulation) {
			return new BCInteger(null,0);
		}
		if (!target.isOnline()) {
			return new BCInteger(null,0);
		}
		vm.queueOwnerSay(target.getContent(),message.getContent());
		return new BCInteger(null,1);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Causes an Object to emit a link message",
	                        parameters="String - UUID for the object to emit the link message<br/>Integer - Integer for the link message<br/>String - Message for the link message<br/>String - Key sent by the message",
	                        notes="Only works with an object driver, HUDs do not support this function",
	                        returns="Integer - The number zero",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsObjectEmitLinkMessage(final State st,
	                                                @Nonnull final GSVM vm,
	                                                @Nonnull final BCString objectUUID,
	                                                @Nonnull final BCInteger messageNumber,
	                                                @Nonnull final BCString message,
	                                                @Nonnull final BCString id) {
		final Obj object=Obj.findOrNull(st,objectUUID.toString());
		if (object==null) {
			throw new GSUnknownIdentifier("No object with ID "+objectUUID+" was found");
		}
		if (object.getInstance()!=st.getInstance()) {
			throw new SystemConsistencyException("Object driver attempting cross instance link messaging...");
		}
		final JSONObject send=new JSONObject();
		send.put("linkmessagenumber",messageNumber.toString());
		send.put("linkmessage",message.toString());
		send.put("linkid",id.toString());
		new Transmission(object,send).start();
		return new BCInteger(null,0);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Offer a website to a specific character",
	                        parameters="Character - Character to offer to<br>String - URL to offer, must be http:// or https://<br>String - Description to show for the URL",
	                        notes="",
	                        returns="Integer - One if the character is offline, zero otherwise",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsOfferWebsite(final State st,
	                                       @Nonnull final GSVM vm,
	                                       @Nonnull final BCCharacter target,
	                                       @Nonnull final BCString URL,
	                                       @Nonnull final BCString description) {
		if (!target.isOnline()) { return new BCInteger(null,1); }
		final State bypass=new State(st.getInstanceNullable(),st.getRegionNullable(),st.zone,st.getCharacterNullable());
		bypass.source=State.Sources.SYSTEM;
		final Response response=OpenWebsite.offerWebsite(bypass,URL.getContent(),description.getContent());
		target.getContent().push(response.asJSON(st));
		return new BCInteger(null,0);
	}
}
