package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;

public class GSVM {
	// GPHUD Scripting Virtual Machine ... smiley face

	byte[] bytecode;

	public GSVM(Byte[] code) {
		bytecode = new byte[code.length];
		{
			for (int i = 0; i < code.length; i++) { bytecode[i] = code[i]; }
		}
	}

	public GSVM(byte[] code) { bytecode = code; }

	public String toHtml() {
		return "<table>" + toHtml(0) + "</table>";
	}

	public String toHtml(int index) {
		String line = "";
		while (index < bytecode.length) {
			line += "<tr><th>" + index + "</th><td>";
			byte instruction = bytecode[index];
			index++;
			ByteCode.InstructionSet decode = ByteCode.get(instruction);
			if (decode == null) {
				throw new SystemException("Unable to decode instruction " + instruction + " at index " + index);
			}
			line += decode + "</td><td>";
			switch (decode) {
				case Character:
				case Group:
				case Integer:
				case BranchIfZero:
				case Avatar:
					line += getInt(index);
					index += 4;
					break;
				case String:
					int length = getInt(index);
					index += 4;
					byte[] string = new byte[length];
					try { System.arraycopy(bytecode, index, string, 0, length); } catch (RuntimeException e)
					//{ return line+"</td></tr>"; }
					{
						throw new SystemException("Failed to arraycopy " + length + " from pos " + index, e);
					}
					index += length;
					String str = new String(string);
					line += str;
					break;
				case Debug:
					int lineno=getShort(index); index+=2;
					int columnno=getShort(index); index+=2;
					line+=lineno+":"+columnno;
					break;
				default:
			}
			line += "</td></tr>";
		}
		return line + "</td></tr>";
	}

	int getInt(int index) {
		return (((int) bytecode[index] & 0xff) << 24) + (((int) bytecode[index + 1] & 0xff) << 16) + (((int) bytecode[index + 2] & 0xff) << 8) + (((int) bytecode[index + 3] & 0xff));
	}

	int getShort(int index) {
		return((((int)bytecode[index]&0xff)<<8)+(((int)bytecode[index+1]&0xff)));
	}
}
