package net.coagulate.GPHUD.Modules.Teleportation;

import net.coagulate.GPHUD.Data.Landmark;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Management {
	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/Teleportation", requiresPermission="Teleportation.*")
	public static void configuration(@Nonnull final State st,final SafeMap map) {
		final Form f=st.form();
		f.add(new TextHeader("Teleportation Landmarks"));
		final Table t=new Table();
		f.add(t);
		t.add(new HeaderRow().add("Name").add("Region").add("Co-ordinates").add("Look at"));
		for (final Landmark landmark: Landmark.getAll(st)) {
			t.openRow();
			t.add(landmark.getName());
			t.add(landmark.getRegion(true).getName());
			t.add(landmark.getCoordinates());
			t.add(landmark.getLookAt());
			if (st.hasPermission("Teleportation.DeleteLandmark")) {
				t.add(new Form(st,true,"./Teleportation/DeleteLandmark","Delete","name",landmark.getName()));
			}
		}
	}
}
