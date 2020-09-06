package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Landmark;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Output {
	private Output() {}

	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the character",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+">String - message to speak",
	                        returns="Integer - The number 0",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsChar(final State st,
	                                    @Nonnull final GSVM vm,
	                                    @Nonnull final BCCharacter target,
	                                    @Nonnull final BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		vm.queueSayAs(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the HUD",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+">String - message to speak",
	                        returns="Integer - The number 0",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsHUD(final State st,
	                                   @Nonnull final GSVM vm,
	                                   @Nonnull final BCCharacter target,
	                                   @Nonnull final BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		vm.queueSay(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to send a message to its wearer",
	                        parameters="Character - character whose HUD will message the wearer (the character "+"themselves)<br>String - message to pass",
	                        returns="Integer - The number 0",
	                        notes="Messages are stacked up, per user, until the script completes or is "+"suspended",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayToChar(final State st,
	                                    @Nonnull final GSVM vm,
	                                    @Nonnull final BCCharacter target,
	                                    @Nonnull final BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		vm.queueOwnerSay(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Teleports the player",
	                        parameters="BCCharacter - who to teleport<br>BCString - Landmark name to teleport to",
	                        returns="Integer - 0",
	                        notes="",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsTeleport(@Nonnull final State st,
	                                   @Nonnull final GSVM vm,
	                                   @Nonnull final BCCharacter target,
	                                   @Nonnull final BCString landmark) {
		if (vm.simulation) { return new BCInteger(null,0); }
		GSFunctions.assertModule(st,"Teleportation");
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		final Landmark t=Landmark.find(st,landmark.getContent());
		if (t==null) { throw new GSResourceUnavailableException("Can not find landmark "+landmark.getContent()); }
		vm.queueTeleport(target.getContent(),t.getHUDRepresentation(false));
		return new BCInteger(null,0);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the character, if online, otherwise silently returns",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+">String - message to speak",
	                        returns="Integer - 1 if the message was sent, 0 if not",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsCharIfOnline(final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { return new BCInteger(null,0); }
		vm.queueSayAs(target.getContent(),message.getContent());
		return new BCInteger(null,1);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to speak in local chat, as the HUD, if online, otherwise silently returns",
	                        parameters="Character - character whose HUD will emit the "+"message<br"+">String - message to speak",
	                        returns="Integer - 1 if the message was sent, 0 if not",
	                        notes="Messages are stacked up, per user, until the script completes or is suspended",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayAsHUDIfOnline(final State st,
	                                           @Nonnull final GSVM vm,
	                                           @Nonnull final BCCharacter target,
	                                           @Nonnull final BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { return new BCInteger(null,0); }
		vm.queueSay(target.getContent(),message.getContent());
		return new BCInteger(null,1);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Causes the HUD to send a message to its wearer, if online, otherwise silently returns",
	                        parameters="Character - character whose HUD will message the wearer (the character "+"themselves)<br>String - message to pass",
	                        returns="Integer - 1 if the message was sent, 0 if not",
	                        notes="Messages are stacked up, per user, until the script completes or is "+"suspended",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.OUTPUT)
	public static BCInteger gsSayToCharIfOnline(final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { return new BCInteger(null,0); }
		vm.queueOwnerSay(target.getContent(),message.getContent());
		return new BCInteger(null,1);
	}


}
