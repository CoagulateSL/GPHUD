/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.External;

import net.coagulate.GPHUD.Data.InstanceDevelopers;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;


/**
 * Configure attributes.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Config {
	
	// ---------- STATICS ----------
	@URLs(url="/configuration/external", requiresPermission="External.*")
	public static void configPage(@Nonnull final State st,final SafeMap values) {
		configPage(st,values,st.simulate(st.getCharacterNullable()));
	}
	
	public static void configPage(@Nonnull final State st,final SafeMap values,final State simulated) {
		final Form f=st.form();
		f.noForm();
		f.add("<h1>External API access</h1><br>");
		f.add("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin-top:0.5em; border:1px #d33 solid; padding:0.5em; background-color:#fee7e6\"><tr><td nowrap=\"true\" valign=\"top\"><b>Caution:</b> </td><td valign=\"top\" style=\"padding-left:0.5em\">External access allows the developer to execute any command against your instance, regardless of their own personal permissions ; enabling a developer for external access requires trust and is granting a large set of permissions.  While commands like 'delete' are usually disabled to External access, a malicious actor could make quite the mess of your data and configuration.</td></tr></table>");
		f.add("<br><br>");
		f.add("<p>List of active developers:");
		f.add("<table border=0>");
		for (final User user: User.getDevelopers()) {
			//if (!user.isSuperAdmin()) {
			final boolean enabled=InstanceDevelopers.isDeveloper(st.getInstance(),user);
			String col="#ffe0e0";
			if (enabled) {
				col="#e0ffe0";
			}
			f.add("<tr bgcolor="+col+">");
			f.add("<td>"+user.getName()+"</td><td>");
			if (enabled) {
				f.add(new Form(st,true,"./external/deauthorise","Disable Developer Access","developer",user.getName()));
			} else {
				f.add(new Form(st,true,"./external/authorise","Enable Developer Access","developer",user.getName()));
			}
			f.add("</td></tr>");
			//}
		}
		f.add("</table></p>");
	}
	
	
	@URLs(url="/configuration/external/authorise", requiresPermission="External.Authorise")
	public static void authoriseForm(@Nonnull final State st,@Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"External.Authorise",values);
	}
	
	@Commands(description="Authorise a developer at this instance",
	          permitExternal=false,
	          permitObject=false,
	          permitScripting=false,
	          requiresPermission="External.Authorise",
	          context=Context.AVATAR)
	@Nonnull
	public static Response authorise(@Nonnull final State state,
	                                 @Arguments(name="developer",
	                                            description="Developer to authorise",
	                                            type=ArgumentType.AVATAR) @Nonnull final User developer) {
		if (!developer.hasDeveloperKey()) {
			return new ErrorResponse("That user is not a developer, please have them register with Iain Maltz");
		}
		if (InstanceDevelopers.isDeveloper(state.getInstance(),developer)) {
			return new ErrorResponse("That user is already a developer at this instance");
		}
		InstanceDevelopers.authorise(state,state.getInstance(),developer);
		return new OKResponse("Developer "+developer+" is now authorised for external access at "+state.getInstance());
	}
	
	@URLs(url="/configuration/external/deauthorise", requiresPermission="External.DeAuthorise")
	public static void deAuthoriseForm(@Nonnull final State st,@Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"External.DeAuthorise",values);
	}
	
	@Commands(description="Authorise a developer at this instance",
	          permitExternal=false,
	          permitObject=false,
	          permitScripting=false,
	          requiresPermission="External.Authorise",
	          context=Context.AVATAR)
	@Nonnull
	public static Response deAuthorise(@Nonnull final State state,
	                                   @Arguments(name="developer",
	                                              description="Developer to deauthorise",
	                                              type=ArgumentType.AVATAR) @Nonnull final User developer) {
		if (!InstanceDevelopers.isDeveloper(state.getInstance(),developer)) {
			return new ErrorResponse("That user not a developer at this instance");
		}
		InstanceDevelopers.deAuthorise(state,state.getInstance(),developer);
		return new OKResponse("Developer "+developer+" is now deauthorised at "+state.getInstance());
	}
	
}
