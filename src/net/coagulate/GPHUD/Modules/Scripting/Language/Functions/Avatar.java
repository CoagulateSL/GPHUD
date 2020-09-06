package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Avatar {

	// ---------- STATICS ----------
	@GSFunction(description="Checks if the controller of the current character has a particular permission",
	            privileged=false,
	            returns="Integer - 0 if they do not have the permission, 1 if they do",
	            parameters="String - name of the permission to check",
	            notes="",
	            category= SCRIPT_CATEGORY.AVATAR)
	public static BCInteger gsHasPermission(@Nonnull final State st,
	                                        @Nonnull final GSVM vm,
	                                        @Nonnull final BCString permission) {
		final String check=permission.getContent();
		if (st.hasPermission(check)) { return new BCInteger(null,1); }
		return new BCInteger(null,0);
	}

}
