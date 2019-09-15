/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.Permission;

/**
 * @author iain
 */
public class AttributePermission extends Permission {

	Attribute a;

	public AttributePermission(Attribute a) {
		this.a = a;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String name() {
		return a.getName();
	}

	@Override
	public String description() {
		return "Permission to admin set attribute " + a.getNameSafe();
	}

	@Override
	public boolean grantable() {
		return true;
	}

}
