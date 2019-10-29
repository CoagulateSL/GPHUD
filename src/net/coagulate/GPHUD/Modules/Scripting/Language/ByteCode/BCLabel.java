package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCLabel extends ByteCode {
	final int id;
	public BCLabel(int id) {this.id=id;}
	public String explain() { return "Label:"+id; }
	Integer address=null;
	public void toByteCode(List<Byte> bytes) {
		address=bytes.size();
	}
}
