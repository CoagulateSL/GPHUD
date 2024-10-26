package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.CharacterPool;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidFunctionCall;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class KV {
	private KV() {
	}
	
	// ---------- STATICS ----------
	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Returns the value of a KV",
	                        returns="String - The value requested",
	                        parameters="Character character - The character to look the "+
	                                   "value up for (use any character if the value doesn't depend on the character)"+
	                                   "<br>String kvName - The name of the KV to lookup",
	                        notes="<B>BEWARE:</b> This "+
	                              "function <B>ALWAYS</B> returns a String value, even if you're getting a number, so code like <br><pre>Integer "+
	                              "value=gsGetKV(char,\"Characters.stat\")"+"+3</pre"+
	                              "><br>will return, for example, 13 if the character's stat is 1, as this is string "+
	                              "addition rules<br>To cast the value to an integer, first do "+"<br><pre"+
	                              ">Integer value=gsGetKV(char,\"characters.stat\"); Integer "+
	                              "maths=value+3;</pre><bre>the initial value will cast the String to an Integer, which will make"+
	                              " the "+"maths in the 2nd line work "+"properly (i.e. produce 6)",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.KV)
	public static BCString gsGetKV(@Nonnull final State st,
	                               final GSVM vm,
	                               @Nonnull final BCCharacter character,
	                               @Nonnull final BCString kvName) {
		if (character.getContent().getInstance()!=st.getInstance()) {
			throw new GSInvalidFunctionCall("Character "+character+" belongs to a different instance!");
		}
		final State altState=new State(character.getContent());
		return new BCString(null,altState.getKV(kvName.getContent()).toString());
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Returns the value of a KV from the CHARACTER LEVEL ONLY",
	                        returns="String - The value requested",
	                        parameters="Character character - The character to look the "+"value up for"+
	                                   "<br>String kvName - The name of the KV to lookup",
	                        notes="<B>BEWARE:</b> This "+
	                              "function <B>ALWAYS</B> returns a String value, even if you're getting a number, so code like <br><pre>Integer "+
	                              "value=gsGetKV(char,\"Characters.stat\")"+"+3</pre"+
	                              "><br>will return, for example, 13 if the character's stat is 1, as this is string "+
	                              "addition rules<br>To cast the value to an integer, first do "+"<br><pre"+
	                              ">Integer value=gsGetKV(char,\"characters.stat\"); Integer "+
	                              "maths=value+3;</pre><bre>the initial value will cast the String to an Integer, which will make"+
	                              " the "+"maths in the 2nd line work "+
	                              "properly (i.e. produce 4)<br><b>BEWARE:</b> This call ONLY returns the KV as set on the character directly, it is useful for delta modifications on this value but generally gsGetKV is THE correct call to use",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.KV)
	public static BCString gsGetCharacterKV(@Nonnull final State st,
	                                        final GSVM vm,
	                                        @Nonnull final BCCharacter character,
	                                        @Nonnull final BCString kvName) {
		if (character.getContent().getInstance()!=st.getInstance()) {
			throw new GSInvalidFunctionCall("Character "+character+" belongs to a different instance!");
		}
		final State altState=new State(character.getContent());
		String response=altState.getKV(character.getContent(),kvName.getContent());
		if (response==null) {
			response="";
		}
		return new BCString(null,response);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Set multiple character level KVs",
	                        category=SCRIPT_CATEGORY.KV,
	                        privileged=false,
	                        returns="The integer zero",
	                        parameters="Character - the character to modify<br>List - A list of strings, key then value",
	                        notes="This is intended to be a faster way of settings lots of character KVs by doing them all at once.  Updating them one by one will likely cause delays due to reprocessing lots of things for each single set.  Call as, for example, Integer junk=gsSetCharacterKVs(CALLER,[\"Health.health\",\"10\",\"Characters.Attribute\",\"0\",\"Characters.AdditionalHudText\",\"\"])")
	public static BCInteger gsSetCharacterKVs(@Nonnull final State state,
	                                          final GSVM vm,
	                                          @Nonnull final BCCharacter character,
	                                          @Nonnull final BCList setPairs) {
		final State altState=new State(character.getContent());
		character.getContent().validate(state);
		final ByteCodeDataType[] array=setPairs.getContent().toArray(new ByteCodeDataType[0]);
		for (int i=0;i<(array.length/2);i++) {
			if (!(array[2*i] instanceof BCString)) {
				throw new GSInvalidFunctionCall(
						"gsSetCharacterKVs: All parameters in the list must be Strings, parameter "+(2*i)+
						" (counting from zero) is of type "+array[2*i].getClass().getSimpleName(),
						true);
			}
			if (!(array[(2*i)+1] instanceof BCString)) {
				throw new GSInvalidFunctionCall(
						"gsSetCharacterKVs: All parameters in the list must be Strings, parameter "+((2*i)+1)+
						" (counting from zero) is of type "+array[(2*i)+1].getClass().getSimpleName(),
						true);
			}
			final String kvName=((BCString)array[2*i]).getContent();
			final String kvValue=((BCString)array[(2*i)+1]).getContent();
			final net.coagulate.GPHUD.Modules.KV kv=Modules.getKVDefinitionNullable(altState,kvName);
			if (kv==null) {
				throw new GSUnknownIdentifier("gsSetCharacterKVs: KV '"+kv+"' does not exist",true);
			}
			if (!kv.appliesTo(character.getContent())) {
				throw new GSInvalidFunctionCall(
						"KV "+kv.fullName()+" of scope "+kv.scope()+" does not apply to characters",true);
			}
			String oldValue=altState.getRawKV(character.getContent(),kvName);
			if (oldValue==null) {
				oldValue="";
			}
			if (!oldValue.equals(kvValue)) {
				altState.setKV(character.getContent(),kvName,kvValue,false);
				Audit.audit(state,
				            Audit.OPERATOR.CHARACTER,
				            null,
				            character.getContent(),
				            "SetCharKV",
				            character.getContent().getName()+"/"+kvName,
				            oldValue,
				            kvValue,
				            "Changed character scope KV configuration (Scripted)");
			}
		}
		character.getContent().considerPushingConveyances();
		return new BCInteger(null,0);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Returns the value of an experience pool",
	                        returns="Integer - the experience value",
	                        parameters="Character character - The character to look the "+
	                                   "experience up for"+
	                                   "<br>String xpName - The name of the experience pool to lookup, e.g. 'Experience.VisitXP' or 'Experience.CustomXP' or 'Events.EventXP'",
	                        notes="See the bottom of your character sheet for the full names of experience pools (E.g. View Events.EventXP History link)",
	                        privileged=false,
	                        category=SCRIPT_CATEGORY.KV)
	public static BCInteger gsGetExperience(@Nonnull final State st,
	                               final GSVM vm,
	                               @Nonnull final BCCharacter character,
	                               @Nonnull final BCString xpName) {
		if (character.getContent().getInstance()!=st.getInstance()) {
			throw new GSInvalidFunctionCall("Character "+character+" belongs to a different instance!");
		}
		final State altState=new State(character.getContent());
		final Attribute attr=st.getAttribute(Modules.extractReference(xpName.getContent()));
		if (attr==null) {
			throw new GSUnknownIdentifier("There was no attribute named '"+xpName.getContent()+"' found.");
		}
		if (attr.getType()!=Attribute.ATTRIBUTETYPE.EXPERIENCE &&attr.getType()!=Attribute.ATTRIBUTETYPE.POOL) {
			throw new GSInvalidFunctionCall("Attribute '"+attr+"' is of type '"+attr.getType()+"', but gsGetExperience only works on Experience/Pool attributes.");
		}
		final Pool pool=Modules.getPool(st,xpName.getContent());
		// that said getPool just dumps the prefix right now
		//if (pool==null) { throw new SystemImplementationException("Retrieved null pool for '"+xpName.getContent()+"' after validation"); }
		return new BCInteger(null,CharacterPool.sumPool(st,pool));
	}
}
