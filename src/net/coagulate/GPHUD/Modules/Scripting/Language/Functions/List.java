package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class List {
	@Nonnull
	@GSFunctions.GSFunction(description="Finds an items position in a list",
	                        returns="Integer - The index of the first match, or -1 if not found",
	                        parameters="List list - the list to search inside of<br>"+
	                                   "Any find - The data to search for ; type matching is required",
	                        notes="This just calls gsListFindFrom with the startAt parameter being 0",
	                        privileged=false,
	                        category=GSFunctions.SCRIPT_CATEGORY.UTILITY)
	public static BCInteger gsListFind(@Nonnull final State st,
	                                   @Nonnull final GSVM vm,
	                                   @Nonnull final BCList list,
	                                   @Nonnull final ByteCodeDataType find) {
		return gsListFindFrom(st,vm,list,new BCInteger(null,0),find);
	}
	
	@Nonnull
	@GSFunctions.GSFunction(description="Finds an items position in a list",
	                        returns="Integer - The index of the first match, or -1 if not found",
	                        parameters="List list - the list to search inside of<br>"+
	                                   "Integer startAt - The first index (inclusive) to search from<br>"+
	                                   "Any find - The data to search for ; type matching is required",
	                        notes="This just calls gsListFindFrom with the startAt parameter being 0",
	                        privileged=false,
	                        category=GSFunctions.SCRIPT_CATEGORY.UTILITY)
	public static BCInteger gsListFindFrom(@Nonnull final State st,
	                                       @Nonnull final GSVM vm,
	                                       @Nonnull final BCList list,
	                                       @Nonnull final BCInteger startAt,
	                                       @Nonnull final ByteCodeDataType find) {
		return new BCInteger(null,listFindFrom(st,vm,list,startAt,find));
	}
	
	private static int listFindFrom(@Nonnull final State st,
	                                @Nonnull final GSVM vm,
	                                @Nonnull final BCList list,
	                                @Nonnull final BCInteger startAt,
	                                @Nonnull final ByteCodeDataType find) {
		if (list.isEmpty()) {
			return -1;
		}  // nothing to search in
		if (startAt.getContent()>=list.size()) {
			return -1;
		} // search past end of list
		for (int i=startAt.getContent();i<list.size();i++) {
			if (list.getElement(i).strictlyEquals(find)) {
				return i;
			}
		}
		return -1;
	}
	
	/*** TEST SCRIPT
	 * List l=[1,2,3,"banana",3.0,2];
	 * // TEST, failure to find
	 * Integer test_FailFind=(gsListFind(l,4)==-1);
	 * // TEST find integer
	 * Integer test_FindInteger=(gsListFind(l,2)==1);
	 * // TEST find the float
	 * Integer test_FindFloat=(gsListFind(l,3.0)==4);
	 * // TEST find the string
	 * Integer test_FindString=(gsListFind(l,"banana")==3);
	 * // Test find from to find the 2nd two, starting from the char after the first two
	 * Integer test_FindFrom=(gsListFindFrom(l,gsListFind(l,2)+1,2)==5);
	 * // Test silly use of find from
	 * Integer test_SillyFindFrom=(gsListFindFrom(l,99,2)==-1);
	 * // All the test_ variables should show as ==1 (i.e. is truth) for a pass, true at 2024-10-05 17:27 GMT
	 *
	 */
}
