/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules;

/**
 * @author iain
 */
public class KVValue {
	private String value;
	private String path;

	public KVValue(String value, String path) {
		this.value = value;
		this.path = path;
	}

	public String value() { return value; }

	public String path() { return path; }

	public String toString() { return value(); }

	public Integer intValue() {
		return new Integer(value());
	}

	public Float floatValue() {
		return new Float(value());
	}

	public boolean boolValue() {
		if ("1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "t".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
			return true;
		}
		return false;
	}
}
