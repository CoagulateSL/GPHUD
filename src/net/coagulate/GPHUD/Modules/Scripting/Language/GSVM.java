package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;

public class GSVM {
	// GPHUD Scripting Virtual Machine ... smiley face

	public byte[] bytecode;
	public int PC=0;

	public GSVM(Byte[] code) {
		bytecode = new byte[code.length];
		{
			for (int i = 0; i < code.length; i++) { bytecode[i] = code[i]; }
		}
	}

	public GSVM(byte[] code) { bytecode = code; }

	public String toHtml() {
		PC=0;
		String line = "<table>";
		while (PC < bytecode.length) {
			line += "<tr><th>" + PC + "</th><td>";
			ByteCode instruction=ByteCode.load(this);
			line+=instruction.htmlDecode();
			line += "</td></tr>";
		}
		return line + "</td></tr></table>";
	}

	public int getInt() {
		int ret=(((int) this.bytecode[this.PC] & 0xff) << 24) + (((int) this.bytecode[this.PC + 1] & 0xff) << 16) + (((int) this.bytecode[this.PC + 2] & 0xff) << 8) + (((int) this.bytecode[this.PC + 3] & 0xff));
		this.PC+=4;
		return ret;
	}

	public int getShort()
	{
		int ret=((((int)this.bytecode[this.PC]&0xff)<<8)+(((int)this.bytecode[this.PC+1]&0xff)));
		this.PC+=2;
		return ret;
	}

}
