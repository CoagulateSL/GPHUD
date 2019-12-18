package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Interfaces.Outputs.Separator;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


public class PublishingConfig {
	@URL.URLs(url="/configuration/publishing")
	public static void configPage(@Nonnull final State st,
	                              final SafeMap values)
	{
		final Form f=st.form();
		f.add(new TextHeader("Links to published pages"));
		f.add(new Text(
				"<p><i><b>Note:</b> This feature is in early release, please feel free to submit ideas for published pages, layouts, options, formatting etc</i></p>"));
		f.noForm();
		boolean publishedany=false;
		if (st.getKV("Publishing.PublishGroups").boolValue()) {
			publishedany=true;
			f.add("<b>Group Pages:</b> "+"<a href=\"/GPHUD/publishing/allgroups\">All Groups And Members</a>");
			final List<String> grouptypes=new ArrayList<>();
			for (final CharacterGroup group: st.getInstance().getCharacterGroups()) {
				f.add(" <a href=\"/GPHUD/publishing/group/"+group.getId()+"\">Group:"+group.getName()+"</a>");
				final String gt=group.getType();
				if (gt!=null) {
					if (!grouptypes.contains(gt)) grouptypes.add(gt);
				}
			}
			for (final String type: grouptypes) {
				f.add(" <a href=\"/GPHUD/publishing/grouptype/"+st.getInstance()
				                                                  .getId()+"/"+type+"\">GroupType:"+type+"</a>");
			}
			f.add("<br>");
		}
		if (st.getKV("Publishing.PublishStatus").boolValue()) {
			publishedany=true;
			f.add("<b>Status Pages:</b> "+"<a href=\"/GPHUD/publishing/status\">Short Status</a>");
			f.add("<br>");
		}
		if (st.getKV("Publishing.PublishStatusAndPlayers").boolValue()) {
			publishedany=true;
			f.add("<b>Status Pages:</b> "+"<a href=\"/GPHUD/publishing/statusfull\">Full Status</a>");
			f.add("<br>");
		}
		if (!publishedany) { f.add("No pages are published ; enable them below to see them listed here"); }
		f.add(new Separator());
		Modules.get(st,"Publishing").kvConfigPage(st);
		//GenericConfiguration.page(st, values, st.getInstance(),simulated);
	}


}
