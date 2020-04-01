package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Groups {
	private Groups() {}

	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Gets the group name for a given attribute",
	                        returns="String - name of group of appropriate subtype, or the empty string if none",
	                        notes="",
	                        parameters="Character - character to interrogate<br>String - type of group to get",
	                        privileged=false)
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
	                        privileged=false)
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
	                        privileged=false)
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
