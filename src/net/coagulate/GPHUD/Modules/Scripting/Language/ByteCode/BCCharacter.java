package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.Char;

import java.util.List;

public class BCCharacter extends ByteCode {
	private Char content=null;
	public BCCharacter() {}
	public BCCharacter(Char content) { this.content=content; }
	public String explain() { return "BCCharacter("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCCharacter.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
}
