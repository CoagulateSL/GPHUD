package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCInteger extends ByteCode {
	private Integer content=0xffffffff;
	public BCInteger() {}
	public BCInteger(Integer content) { this.content=content; }

	public BCInteger(String tokens) { this.content=Integer.parseInt(tokens); }

	public String explain() { return "Integer("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Integer.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content);
	}
	@Override public String htmlDecode() { return "Integer</td><td>"+content; }
}
