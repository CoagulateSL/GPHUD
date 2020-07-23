package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Enable or disable modules.  Instance owner only.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ModuleControl {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Disable the specified module",
	          requiresPermission="instance.ModuleEnablement",
	          permitExternal=false,
	          permitScripting=false,
	          permitObject=false)
	public static Response disableModule(@Nonnull final State st,
	                                     @Nonnull @Arguments(type=ArgumentType.MODULE,
	                                                         name="module",description="Module to disable") final Module module) {
		if (!module.canDisable()) {
			return new ErrorResponse("The module "+module.getName()+" does not allow its self to be disabled, it is probably critical to system functionality");
		}
		final String k=module.getName()+".Enabled";
		st.setKV(st.getInstance(),module.getName()+".Enabled","false");
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Disable Module",module.getName(),"","","Module disabled");
		return new OKResponse("Module "+module.getName()+" has been disabled");
	}

	@URLs(url="/configuration/disablemodule",
	      requiresPermission="instance.ModuleEnablement")
	public static void disableModuleForm(@Nonnull final State st,
	                                     @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Configuration.DisableModule",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Enabled the specified module",
	          requiresPermission="instance.ModuleEnablement",
	          permitScripting=false,
	          permitExternal=false,
	          permitObject=false)
	public static Response enableModule(@Nonnull final State st,
	                                    @Nonnull @Arguments(type=ArgumentType.MODULE,
	                                                        name="module",description="Module to enable") final Module module) {
		st.setKV(st.getInstance(),module.getName()+".Enabled","true");
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Enable Module",module.getName(),"","","Module enabled");
		return new OKResponse("Module "+module.getName()+" has been enabled");
	}

	@URLs(url="/configuration/enablemodule",
	      requiresPermission="instance.ModuleEnablement")
	public static void enableModuleForm(@Nonnull final State st,
	                                    @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Configuration.EnableModule",values);
	}
}
