package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows a character to be retired.
 *
 * @author Iain Price
 */
public abstract class Retirement {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Command.Context.CHARACTER,
	          description="Retires the current character, if permitted.",
	          permitObject=false)
	public static Response retireMe(@Nonnull final State st) {
		if (!st.getKV("Instance.AllowSelfRetire").boolValue()) {
			return new ErrorResponse("Retirement is not presently permitted");
		}
		if (!st.getCharacter().retired()) {
			st.getCharacter().retire();
			Audit.audit(true,
			            st,
			            Audit.OPERATOR.CHARACTER,
			            st.getAvatarNullable(),
			            st.getCharacter(),
			            "SET",
			            "RETIRED",
			            Boolean.toString(st.getCharacter().retired()),
			            "true",
			            "Character self retired"
			           );
		}
		final JSONObject json=new JSONObject();
		json.put("reboot","Character has retired");
		return new JSONResponse(json);
	}

	@Nonnull
	@Commands(context=Command.Context.AVATAR,
	          description="Force retire a character",
	          requiresPermission="Characters.Retire")
	public static Response retireTarget(@Nonnull final State st,
	                                    @Nullable
	                                    @Arguments(description="Character to retire",
	                                               type=ArgumentType.CHARACTER) final Char target) {
		if (target==null) { return new ErrorResponse("Target character was null"); }
		target.validate(st);
		if (target.retired()) { return new OKResponse("Target character is already retired"); }
		target.retire();
		Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,target,"SET","RETIRED",Boolean.toString(target.retired()),"true","Character retired by administrator");
		target.hudMessage("This character has been retired by Administrator '"+st.getAvatar().getName()+"'");
		target.push("reboot","Restarting due to character being retired.");
		return new OKResponse("Target character is retired");
	}

}
