package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCDebug extends ByteCode {
	private int line=0;
	private int column=0;
	public BCDebug(int line,int column) { this.line=line; this.column=column; }

	@Override
	public String explain() {
		return "Debug (Line "+line+" column "+column+")";
	}

	@Override
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Debug.get());
		addShort(bytes,line);
		addShort(bytes,column);
	}
	@Override public String htmlDecode() { return "Debug</td><td>"+line+":"+column; }
}
