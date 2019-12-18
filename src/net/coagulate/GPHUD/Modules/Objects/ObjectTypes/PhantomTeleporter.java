package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class PhantomTeleporter extends Teleporter {
	PhantomTeleporter(final State st,
	                  @Nonnull final ObjectTypes object)
	{
		super(st,object);
	}

	@Nonnull
	@Override
	public String explainHtml() {
		return "A Phantom Teleporter : Teleport user on collision (intersection) to "+json.optString("teleporttarget",
		                                                                                             "(unset)"
		                                                                                            );
	}

	@Nonnull
	public String explainText() { return explainHtml(); }

	@Nonnull
	@Override
	public MODE mode() { return MODE.PHANTOM; }

	@Nonnull
	@Override
	public Response collide(@Nonnull final State st,
	                        @Nonnull final Char clicker)
	{
		return execute(st,clicker);
	}

	@Override
	public void editForm(@Nonnull final State st) {
		super.editForm(st);
	}

	@Override
	public void update(@Nonnull final State st) {
		super.update(st);
	}

}
