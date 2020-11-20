package net.coagulate.GPHUD.Modules.Alias;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Configure your aliases here.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Configuration {
	// ---------- STATICS ----------
	@URLs(url="/configuration/alias",
	      requiresPermission="Alias.Config")
	public static void aliasesList(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		final Form f=st.form();
		f.noForm();
		f.add(new TextSubHeader("Alias Configuration"));

		if (values.containsKey("deletealias") && st.hasPermission("alias.config")) {
			final Alias alias=Alias.getAlias(st,values.get("deletealias"));
			if (alias==null) {
				f.add("<p color=red>Alias '"+values.get("deletealias")+"' was not fount</p>");
			}
			else {
				alias.delete();
				f.add("<p color=green>Alias '"+values.get("deletealias")+"' has been deleted</p>");
			}
		}

		final Map<String,Alias> aliases=Alias.getAliasMap(st);
		int counter=0;
		for (final Map.Entry<String,Alias> entry: aliases.entrySet()) {
			final String name=entry.getKey();
			String innercontent="";
			if (st.hasPermission("alias.config")) {
				innercontent+="<button style=\"border: 0;\" onclick=\"document.getElementById('delete-"+counter+"').style.display='inline';\">Delete</button>";
				innercontent+="<div id=\"delete-"+counter+"\" style=\"display: none;\">";
				innercontent+="&nbsp;&nbsp;&nbsp;";
				innercontent+="CONFIRM DELETE? ";
				innercontent+="<form style=\"display: inline;\" method=post>";
				innercontent+="<input type=hidden name=deletealias value=\""+name+"\">";
				innercontent+="<button style=\"border:0; background-color: #ffc0c0;\" type=submit>Yes, Delete!</button>";
				innercontent+="</form>";
				innercontent+="</div>";
				innercontent+="&nbsp;&nbsp;&nbsp;";
			}
			innercontent+="<a href=\"./alias/view/"+entry.getValue().getId()+"\">"+name+"</a>";
			innercontent+="<br>";
			f.add(innercontent); // bleugh
			counter++;
		}
		f.add("<br>");
		f.add(new Form(st,false,"./alias/create","Create"));
	}

	@URLs(url="/configuration/alias/create",
	      requiresPermission="Alias.Config")
	public static void createAlias(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		if ("Submit".equals(values.get("Submit")) && !values.get("name").isEmpty() && !values.get("command").isEmpty()) {
			final JSONObject template=new JSONObject();
			template.put("invoke",values.get("command"));
			try {
				final Alias newalias=Alias.create(st,values.get("name"),template);
				Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create","Alias",null,values.get("command"),"Avatar created new alias");
				throw new RedirectionException("./view/"+newalias.getId());
			}
			catch (@Nonnull final UserException e) {
				st.form().add(new Paragraph(new TextError("Creation failed : "+e.getMessage())));
			}
		}
		final Form f=st.form();
		final Table t=new Table();
		f.add(t);
		t.openRow().add("Alias Name").add(new TextInput("name"));
		t.openRow().add("Base Command").add(DropDownList.getCommandsList(st,"command",true));
		t.openRow().add(new Cell(new Button("Submit"),2));
	}

	@URLs(url="/configuration/alias/view/*")
	public static void viewAlias(@Nonnull final State st,
	                             @Nonnull final SafeMap values) {
		final String[] split=st.getDebasedURL().split("/");
		final String id=split[split.length-1];
		final Alias a=Alias.get(Integer.parseInt(id));
		viewAlias(st,values,a);
	}

	public static void viewAlias(@Nonnull final State st,
	                             @Nonnull final SafeMap values,
	                             @Nonnull final Alias a) {
		a.validate(st);
		final Form f=st.form();
		if ("Update".equals(values.get("Update"))) {
			if (st.hasPermissionOrAnnotateForm("Alias.Config")) {
				final JSONObject old=a.getTemplate();
				final JSONObject template=new JSONObject();
				for (final String k: values.keySet()) {
					if (!"Update".equals(k) && !"okreturnurl".equals(k) && !values.get(k).isEmpty()) {
						template.put(k,values.get(k));
					}
				}
				template.put("invoke",old.get("invoke"));
				a.setTemplate(template);
				Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Updated",a.getName(),old.toString(),template.toString(),"Avatar updated command alias");
				f.add(new TextOK("Template Updated"));
			}
		}
		final Table t=new Table();
		f.add(new TextHeader("Alias Configuration : "+a.getName()));
		final JSONObject template=a.getTemplate();
		f.add(new Paragraph("Invokes command "+template.getString("invoke")));
		f.add(new Paragraph(new TextSubHeader("Template")));
		f.add(t);
		t.add(new HeaderRow().add("Argument Name").add("Templated Value").add("Originating Type").add("Originating Description").add("Replaced Description"));
		final Command c=Modules.getCommand(st,template.getString("invoke"));
		for (final Argument arg: c.getArguments()) {
			if (!template.has(arg.name())) { template.put(arg.name(),""); }
		}

		for (final String name: template.keySet()) {
			if (!"invoke".equals(name) && !name.endsWith("-desc")) {
				t.openRow().add(name).add(new TextInput(name,template.getString(name)));
				Argument arg=null;
				for (final Argument anarg: c.getArguments()) { if (anarg.name().equals(name)) { arg=anarg; }}
				if (arg!=null) {
					t.add(arg.type().toString());
					t.add(arg.description());
					final String desc=template.optString(name+"-desc","");
					t.add(new TextInput(name+"-desc",desc));
					if (arg.delayTemplating()) { t.add("  <i> ( This parameter uses delayed templating ) </i>"); }
				}
			}
		}
		if (st.hasPermission("Alias.Config")) { f.add(new Button("Update","Update")); }
	}


}
