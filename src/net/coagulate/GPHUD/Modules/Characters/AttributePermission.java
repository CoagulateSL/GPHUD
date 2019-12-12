/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.State;

/**
 * @author iain
 */
public class AttributePermission extends Permission {

	final Attribute a;

	public AttributePermission(Attribute a) {
		this.a = a;
	}

	@Override
	public Module getModule(State st) {
		return Modules.get(st,"Characters");
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String name() {
		return "Set"+a.getName();
	}

	@Override
	public String description() {
		return "Permission to admin set attribute " + a.getNameSafe();
	}

	@Override
	public POWER power() {
		return POWER.LOW;
	}

	@Override
	public boolean grantable() {
		return true;
	}

}
