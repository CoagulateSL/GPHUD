package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSCastException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Input {
	private Input() {
	}
	
	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Triggers the character's HUD to select a nearby character, the availablility of manual selection is defined by GPHUDClient.AllowManualByDefault",
	                        parameters="Character - target - The character to ask<br>String - "+
	                                   "message - Description for the dialog box",
	                        notes="",
	                        category=SCRIPT_CATEGORY.INPUT,
	                        returns="Character - a character the user selected",
	                        privileged=false)
	public static BCCharacter gsSelectCharacter(@Nonnull final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString message) {
		if (vm.simulation) {
			vm.suspend(st,st.getCharacter());
			return target;
		}
		target.getContent().validate(st);
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		vm.queueSelectCharacter(target.getContent(),
		                        message.getContent(),
		                        st.getKV("GPHUDClient.AllowManualByDefault").boolValue());
		vm.suspend(st,target.getContent());
		return target;
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Triggers the character's HUD to select a nearby character, specifying manual selection enablement/disablement",
	                        parameters="Character - target - The character to ask<br>String - message - Description for the dialog box<br>Integer - allowManual - If set to zero will not allow manual target selection, any other value will enable it",
	                        notes="",
	                        category=SCRIPT_CATEGORY.INPUT,
	                        returns="Character - a character the user selected",
	                        privileged=false)
	public static BCCharacter gsSelectCharacterSpecifyManual(@Nonnull final State st,
	                                                         @Nonnull final GSVM vm,
	                                                         @Nonnull final BCCharacter target,
	                                                         @Nonnull final BCString message,
	                                                         @Nonnull final BCInteger allowManual) {
		if (vm.simulation) {
			vm.suspend(st,st.getCharacter());
			return target;
		}
		target.getContent().validate(st);
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		final boolean allowManualBoolean=allowManual.getContent()!=0;
		vm.queueSelectCharacter(target.getContent(),message.getContent(),allowManualBoolean);
		vm.suspend(st,target.getContent());
		return target;
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Triggers the character's HUD to enter a string",
	                        parameters="Character - target - The character to ask<br>String - message - "+
	                                   "Description for the dialog box",
	                        notes="",
	                        returns="String - Some user input text",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.INPUT)
	public static BCString gsGetText(@Nonnull final State st,
	                                 @Nonnull final GSVM vm,
	                                 @Nonnull final BCCharacter target,
	                                 @Nonnull final BCString message) {
		if (vm.simulation) {
			vm.suspend(st,st.getCharacter());
			return new BCString(null,"Simulated user input");
		}
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		vm.queueGetText(target.getContent(),message.getContent());
		vm.suspend(st,target.getContent());
		return new BCString(null,"");
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Triggers the character's HUD to offer a choice (menu box)",
	                        parameters="Character - target - The character to ask<br>String - "+
	                                   "message - Description for the dialog box<br>List - A list of strings the "+
	                                   "user may choose from",
	                        notes="",
	                        returns="String - The user's selection",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.INPUT)
	public static BCString gsGetChoice(@Nonnull final State st,
	                                   @Nonnull final GSVM vm,
	                                   @Nonnull final BCCharacter target,
	                                   @Nonnull final BCString message,
	                                   @Nonnull final BCList choices) {
		if (vm.simulation) {
			vm.suspend(st,st.getCharacter());
			if (choices.getContent().isEmpty()) {
				return new BCString(null,"Simulation with empty choice list");
			} else {
				return choices.getContent()
				              .get(ThreadLocalRandom.current().nextInt(0,choices.getContent().size()))
				              .toBCString();
			}
		}
		if (!target.isOnline()) {
			throw new GSResourceUnavailableException("Character "+target+" is not online",true);
		}
		final List<String> strchoices=new ArrayList<>();
		for (final ByteCodeDataType s: choices.getContent()) {
			strchoices.add(s.toBCString().getContent());
		}
		vm.queueGetChoice(target.getContent(),message.getContent(),strchoices);
		vm.suspend(st,target.getContent());
		return new BCString(null,"");
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Checks if a string can be converted into a number successfull",
	                        parameters="String - string to test",
	                        notes="",
	                        returns="Integer - 1 if the string can convert to an integer, 0 if it fails to do so (and will crash your script if you try)",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.INPUT)
	public static BCInteger gsIsANumber(final State st,final GSVM vm,@Nonnull final BCString teststring) {
		try {
			teststring.toBCInteger();
		} catch (@Nonnull final GSCastException e) {
			return new BCInteger(null,0);
		}
		return new BCInteger(null,1);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Returns a new line character",
	                        parameters="None",
	                        notes="",
	                        returns="String - a newline character",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.INPUT)
	public static BCString gsNewLine(final State st,final GSVM vm) {
		return new BCString(null,"\n");
	}
}
