package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class PhantomTeleporter extends Teleporter {
	PhantomTeleporter(final State st,@Nonnull final ObjType object) {
		super(st,object);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explainHtml() {
		return "A Phantom Teleporter : Teleport user on collision (intersection) to "+
		       json.optString("teleporttarget","(unset)");
	}
	
	@Nonnull
	public String explainText() {
		return explainHtml();
	}
	
	@Nonnull
	@Override
	public MODE mode() {
		return MODE.PHANTOM;
	}
	
	@Nonnull
	@Override
	public Response collide(@Nonnull final State st,@Nonnull final Char collider) {
		return execute(st,collider);
	}
	
	@Override
	public void editForm(@Nonnull final State st) {
		final Table t=new Table();
		teleportEditForm(st,t);
		t.add(new Cell(new Button("Update"),2));
		st.form().add(t);
	}
	
	@Override
	public void update(@Nonnull final State st) {
		boolean changed=false;
		changed=updateTeleport(st)||changed;
		if (changed) {
			object.setBehaviour(json);
		}
	}
	
}
