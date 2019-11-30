package net.coagulate.GPHUD.Modules.Teleportation;

import net.coagulate.GPHUD.Data.Landmarks;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Management {
	@URL.URLs(url = "/configuration/Teleportation")
	public static void configuration(@Nonnull State st, SafeMap map) {
		Form f=st.form;
		f.add(new TextHeader("Teleportation Landmarks"));
		Table t=new Table(); f.add(t);
		t.add(new HeaderRow().add("Name").add("Region").add("Co-ordinates").add("Look at"));
		for (Landmarks landmark:st.getInstance().getLandmarks()) {
			t.openRow();
			t.add(landmark.getName());
			t.add(landmark.getRegion(true).getName());
			t.add(landmark.getCoordinates());
			t.add(landmark.getLookAt());
		}
	}
}
