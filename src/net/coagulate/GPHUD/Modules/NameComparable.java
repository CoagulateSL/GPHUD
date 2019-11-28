/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules;

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
	public abstract String name();

	/**
	 * Provide a sorting order based on names.
	 * Implements the comparison operator for sorting (TreeSet etc)
	 * We rely on the names as the sorting order, and pass the buck to String.compareTo()
	 */
	@Override
	public int compareTo(NameComparable t) {
		String ours = name();
		NameComparable them = t;
		String theirs = them.name();
		return ours.compareTo(theirs);
	}
}
