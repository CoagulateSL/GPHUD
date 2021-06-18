package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Groups {
	private Groups() {}

	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Returns true(1) or false(0) depending on if the character is in the group",
							returns="Integer - 0 if the character is not in the group, 1 if they are",
							notes="",
							parameters="Character - character to check<br>String - name of group to check for membership of",
							privileged=false,
							category= SCRIPT_CATEGORY.GROUPS)
	public static BCInteger gsMemberOf(final State st,
											final GSVM vm,
											@Nonnull final BCCharacter target,
											@Nonnull final BCString groupName) {
		final CharacterGroup group=CharacterGroup.resolve(st,groupName.getContent());
		if (group==null) { throw new UserInputLookupFailureException("Could not find group '"+groupName+"'",true); }
		if (group.hasMember(target.getContent())) { return new BCInteger(null,1); }
		return new BCInteger(null,0);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Gets the group name for a given attribute",
	                        returns="String - name of group of appropriate subtype, or the empty string if none",
	                        notes="",
	                        parameters="Character - character to interrogate<br>String - type of group to get",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.GROUPS)
	public static BCString gsGetGroupByType(final State st,
	                                        final GSVM vm,
	                                        @Nonnull final BCCharacter target,
	                                        @Nonnull final BCString grouptype) {
		final CharacterGroup group=CharacterGroup.getGroup(target.getContent(),grouptype.getContent());
		String name="";
		if (group!=null) { name=group.getName(); }
		return new BCString(null,name);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Gets a List of group names for a given group type",
	                        returns="A List of Strings containing all groups of that type",
	                        parameters="String - type of group to list",
	                        notes="",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.GROUPS)
	public static BCList gsGetGroupsByType(final State st,
	                                       final GSVM vm,
	                                       @Nonnull final BCString grouptype) {
		final BCList list=new BCList(null);
		for (final CharacterGroup cg: st.getInstance().getGroupsForKeyword(grouptype.toString())) {
			list.append(new BCString(null,cg.getName()));
		}
		return list;
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Gets a List of open group names for a given group type",
	                        returns="A List of Strings containing all open groups of that type",
	                        parameters="String - type of group to list",
	                        notes="",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.GROUPS)
	public static BCList gsGetOpenGroupsByType(final State st,
	                                           final GSVM vm,
	                                           @Nonnull final BCString grouptype) {
		final BCList list=new BCList(null);
		for (final CharacterGroup cg: st.getInstance().getGroupsForKeyword(grouptype.toString())) {
			if (cg.isOpen()) { list.append(new BCString(null,cg.getName())); }
		}
		return list;
	}

}
