/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules;

import javax.annotation.Nonnull;

/**
 * @author iain
 */
public class KVValue {
	private final String value;
	private final String path;
	
	public KVValue(final String value,final String path) {
		this.value=value;
		this.path=path;
	}
	
	public String path() {
		return path;
	}
	
	public String toString() {
		return value();
	}
	
	// ---------- INSTANCE ----------
	public String value() {
		return value;
	}
	
	@Nonnull
	public Integer intValue() {
		String value=value();
		if (value==null || value.isBlank()) { return 0; }
		return Integer.valueOf(value);
	}
	
	@Nonnull
	public Float floatValue() {
		return Float.valueOf(value());
	}
	
	public boolean boolValue() {
		return "1".equals(value)||"true".equalsIgnoreCase(value)||"yes".equalsIgnoreCase(value)||
		       "t".equalsIgnoreCase(value)||"y".equalsIgnoreCase(value)||"on".equalsIgnoreCase(value);
	}
}
