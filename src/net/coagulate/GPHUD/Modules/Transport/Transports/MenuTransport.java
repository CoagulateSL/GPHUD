package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Menu;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

/** Transport menus */
public class MenuTransport extends Transporter {
	@Override
	public String description() {
		return "Menu transport";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Menu.getMenusMap(st).keySet().stream().toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Menu menu=Menu.getMenu(st,element);
		exportTo.put("description",menu.getDescription());
		exportTo.put("json",menu.getJSON());
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final String description=element.getString("description");
		final JSONObject json=element.getJSONObject("json");
		existCheck(state,
		           simulation,
		           report,
		           Menu.getMenuNullable(state,name),
		           name,
		           ()->Menu.create(state,name,description,json));
		final Menu menu=Menu.getMenuNullable(state,name);
		if (menu==null&&simulation) {
			return;
		}
		if (menu==null) {
			throw new UserInputLookupFailureException("Can not find recently created menu "+name);
		}
		importValue(state,
		            simulation,
		            report,
		            name,
		            "description",
		            menu.getDescription(),
		            description,
		            (x)->menu.setDescription((String)x));
		importValue(state,
		            simulation,
		            report,
		            name,
		            "content",
		            menu.getJSON(),
		            json,
		            ()->menu.setJSON(json),
		            (x,y)->((JSONObject)x).similar(y));
	}
}
