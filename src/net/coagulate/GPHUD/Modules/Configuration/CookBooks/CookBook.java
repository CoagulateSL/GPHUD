package net.coagulate.GPHUD.Modules.Configuration.CookBooks;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Data.Menus;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * @author Iain Price
 */
public abstract class CookBook {
	protected static void charAttribute(@Nonnull final State st,
	                                    final boolean act,
	                                    @Nonnull final Table t,
	                                    final String attribute,
	                                    final String selfmodify,
	                                    final String attributetype,
	                                    final String grouptype,
	                                    final String useabilitypoints,
	                                    final String required,
	                                    final String defaultvalue)
	{
		t.openRow();
		t.add("Create attribute "+attribute);
		try {
			st.getKVDefinition("Characters."+attribute);
			t.add("Already Exists?");
		} catch (@Nonnull final SystemException e) { t.add("OK"); }
		if (!act) {
			t.add("Create a character attribute "+attribute);
			return;
		}
		// act
		t.add(Modules.run(st,
		                  "Characters.CreateAttribute",
		                  new String[]{attribute,selfmodify,attributetype,grouptype,useabilitypoints,required,defaultvalue}
		                 ).asText(st));
		st.purgeAttributeCache();
	}

	protected static void setKV(@Nonnull final State st,
	                            final boolean act,
	                            @Nonnull final Table t,
	                            @Nonnull final TableRow object,
	                            @Nonnull final String attribute,
	                            final String newvalue)
	{
		t.openRow();
		t.add("Set KV "+attribute);
		t.add("OK");
		if (!act) {
			t.add("Set KV on "+object.asText(st)+" to '"+newvalue+"'");
			return;
		}
		try {
			st.setKV(object,attribute,newvalue);
			t.add("OK");
		} catch (@Nonnull final Exception e) {
			SL.report("Cookbook setKV "+object+"/"+attribute+"="+newvalue,e,st);
			GPHUD.getLogger("CookBook").log(Level.INFO,"Exception in setKV "+object+"/"+attribute+"="+newvalue,e);
			t.add("Error: "+e.getLocalizedMessage());
		}
	}

	protected static void createAlias(@Nonnull final State st,
	                                  final boolean act,
	                                  @Nonnull final Table t,
	                                  @Nonnull final String aliasname,
	                                  final String target,
	                                  @Nonnull final JSONObject template)
	{
		t.openRow();
		t.add("Create Alias "+aliasname);
		try {
			Modules.getCommandNullable(st,"alias."+aliasname);
			t.add("Already Exists?");
		} catch (@Nonnull final UserException e) { t.add("OK"); }
		if (!act) {
			t.add("Create Alias "+aliasname+" around command "+target);
			return;
		}
		try {
			template.put("invoke",target);
			Alias.create(st,aliasname,template);
			t.add("Created alias");
		} catch (@Nonnull final Exception e) {
			SL.report("Cookbook createAlias "+aliasname+"->"+target+"="+template,e,st);
			GPHUD.getLogger("CookBook").log(Level.INFO,"Exception in alias "+aliasname+"->"+target+"="+template,e);
			t.add("Error: "+e.getLocalizedMessage());
		}
	}

	protected static void createMenu(@Nonnull final State st,
	                                 final boolean act,
	                                 @Nonnull final Table t,
	                                 @Nonnull final String name,
	                                 final String description)
	{
		t.openRow();
		t.add("Create menu '"+name+"'");
		final Menus existing=Menus.getMenuNullable(st,name);
		if (existing!=null) { t.add("AlreadyExists"); } else { t.add("OK"); }
		if (!act) {
			t.add("Create a blank menu: "+description);
			return;
		}
		if (existing!=null) {
			t.add("Can't complete");
			return;
		}
		Menus.create(st,name,description,new JSONObject());
		t.add("OK");
	}

	protected static void menu(@Nonnull final State st,
	                           final boolean act,
	                           @Nonnull final Table t,
	                           final String label,
	                           final String command)
	{
		menu(st,act,t,"Main",label,command);
	}

	protected static void menu(@Nonnull final State st,
	                           final boolean act,
	                           @Nonnull final Table t,
	                           @Nonnull final String menuname,
	                           final String label,
	                           final String command)
	{
		t.openRow();
		t.add("Add menu item '"+label+"'");
		final Menus mainmenu=Menus.getMenuNullable(st,menuname);
		JSONObject menu=null;
		if (mainmenu!=null) { menu=mainmenu.getJSON(); }
		int empty=-1;
		boolean full=true;
		boolean already=false;
		if (menu==null) {
			full=false;
			already=false;
		} else {
			for (int i=1;i<=12;i++) {
				if (!menu.has("button"+i)) {
					full=false;
					if (empty==-1) { empty=i; }
				} else {
					if (menu.getString("button"+i).equals(label)) { already=true; }
				}
			}
		}
		if (already) { t.add("AlreadyExists"); } else {
			if (!full) { t.add("OK"); } else { t.add("MenuFull"); }
		}
		if (!act) {
			t.add("Create menu item '"+label+"' to run "+command);
			return;
		}
		if (menu==null) {
			t.add("Can't add menu item, menu "+menuname+" does not exist");
			return;
		}
		//
		if (already || full) {
			t.add("Can't complete");
			return;
		}
		menu.put("button"+empty,label);
		menu.put("command"+empty,command);
		mainmenu.setJSON(menu);
		t.add("OK");
	}

	protected static void confirmButton(@Nonnull final State st,
	                                    @Nonnull final Form f)
	{
		f.add("");
		if (st.hasPermission("Instance.CookBooks")) {
			f.add(new TextSubHeader("You may click here to enact the cookbook"));
			f.add(new Button("ACTIVATE COOKBOOK"));
		} else { f.add("Only the instance owner may run cookbooks"); }
	}

}
