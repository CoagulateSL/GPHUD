/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.Faction;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.GROUP;

/**
 * The 'faction' group attribute.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class FactionAttribute extends CharacterAttribute {
	public FactionAttribute(int id) { super(id); }

	@Override
	public boolean readOnly() {
		return true;
	}

	@Nonnull
	@Override
	public String getSubType() {
		return "Faction";
	}

	@Override
	public boolean usesAbilityPoints() {
		return false;
	}

	@Override
	public boolean getRequired() {
		return false;
	}

	@Nonnull
	@Override
	public String getDefaultValue() {
		return "";
	}

	@Override
	public boolean getSelfModify() {
		return false;
	}

	@Override
	public boolean isKV() {
		return false;
	}

	@Nonnull
	@Override
	public KV.KVTYPE getKVType() {
		throw new SystemException("Invalid KV call to faction group attribute");
	}

	@Nonnull
	@Override
	public String getKVDefaultValue() {
		throw new SystemException("Invalid KV call to faction group attribute");
	}

	@Nonnull
	@Override
	public KV.KVHIERARCHY getKVHierarchy() {
		throw new SystemException("Invalid KV call to faction group attribute");
	}

	@Nonnull
	@Override
	public String getName() {
		return "Faction";
	}

	@Nonnull
	@Override
	public ATTRIBUTETYPE getType() {
		return GROUP;
	}

}
