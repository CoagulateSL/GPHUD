/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules;

import javax.annotation.Nonnull;

/**
 * Wrapper for comparing objects by name in the modules workarea (DBObject handles database sorting).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class NameComparable implements Comparable<NameComparable> {

	/**
	 * Provide a name.
	 *
	 * @return String name, which will be used for sorting
	 */
	@Nonnull
	public abstract String name();

	/**
	 * Provide a sorting order based on names.
	 * Implements the comparison operator for sorting (TreeSet etc)
	 * We rely on the names as the sorting order, and pass the buck to String.compareTo()
	 */
	@Override
	public int compareTo(@Nonnull NameComparable t) {
		String ours = name();
		String theirs = t.name();
		return ours.compareTo(theirs);
	}
}
