package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ByteCode {

	public abstract String explain();
	public abstract void toByteCode(List<Byte> bytes);
	public static Map<Byte,InstructionSet> map=new HashMap<>();

	public enum InstructionSet {
		BCAdd((byte)1),
		BCAssign((byte)2),
		BCAvatar((byte)3),
		BCBranchIfZero((byte)4),
		BCCharacter((byte)5),
		BCDivide((byte)6),
		BCEquality((byte)7),
		BCGroup((byte)8),
		BCInequality((byte)9),
		BCInitialise((byte)10),
		BCInteger((byte)11),
		BCInvoke((byte)12),
		BCLoadVariable((byte)13),
		BCMultiply((byte)14),
		BCResponse((byte)15),
		BCString((byte)16),
		BCSubtract((byte)17);
		private byte value;


		private InstructionSet(byte value) {
			this.value = value;map.put(value,this);
		}
		public byte get() { return value; }
	}
	public static InstructionSet get(byte b) { return map.get(b); }

	void addInt(List<Byte> bytes,int a) {
		bytes.add((byte)(a>>24));
		bytes.add((byte)((a>>16) & 0xff));
		bytes.add((byte)((a>>8) & 0xff));
		bytes.add((byte)(a&0xff));
	}
}
