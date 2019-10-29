package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ByteCode {

	public abstract String explain();
	public abstract void toByteCode(List<Byte> bytes);
	public static Map<Byte,InstructionSet> map=new HashMap<>();

	public enum InstructionSet {
		Debug((byte)0),
		Add((byte)1),
		Assign((byte)2),
		Avatar((byte)3),
		BranchIfZero((byte)4),
		Character((byte)5),
		Divide((byte)6),
		Equality((byte)7),
		Group((byte)8),
		Inequality((byte)9),
		Initialise((byte)10),
		Integer((byte)11),
		Invoke((byte)12),
		LoadVariable((byte)13),
		Multiply((byte)14),
		Response((byte)15),
		String((byte)16),
		Subtract((byte)17);
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
	void addShort(List<Byte> bytes,int a) {
		bytes.add((byte)((a>>8) & 0xff));
		bytes.add((byte)(a&0xff));
	}
}
