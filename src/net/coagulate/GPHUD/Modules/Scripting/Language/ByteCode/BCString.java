package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCString extends ByteCode {
	private String content="";
	public BCString() {};
	public BCString(String content) { this.content=content; }
	public String explain() { return "String("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.String.get());
		addShort(bytes,content.length());
		for (char c:content.toCharArray()) {
			bytes.add((byte)c);
		}
	}
	@Override public String htmlDecode() { return "String</td><td>"+content; }
}
