package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * An effect - a sort of temporary group membership, or a "buff" if you like.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Effect extends TableRow {

	protected Effect(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An effect representation
	 */
	@Nonnull
	public static Effect get(final int id) {
		return (Effect) factoryPut("Effect",id,new Effect(id));
	}


	@Nonnull
	@Override
	public String getTableName() {
		return "effects";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "id";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/GPHUD/Effects/"+getId();
	}

	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	@Override
	public String getKVTable() {
		return "effectskvstore";
	}

	@Nonnull
	@Override
	public String getKVIdField() {
		return "effectid";
	}


	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Effect / State Instance mismatch");
		}
	}

	protected int getNameCacheTime() { return 60; } // events may become renamable, cache 60 seconds
	// perhaps flush the caches (to do) when this happens...
}

