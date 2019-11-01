package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.SL.Data.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ByteCode {

	private ParseNode sourcenode=null;
	public ByteCode(ParseNode n) { sourcenode=n; }
	public ByteCode node(ParseNode n)
	{ sourcenode=n; return this; }
	public ParseNode node() { return sourcenode; }


	public static ByteCode load(GSVM vm) {
		byte instruction = vm.bytecode[vm.PC];
		vm.PC++;
		InstructionSet decode = ByteCode.get(instruction);
		if (decode == null) {
			throw new SystemException("Unable to decode instruction " + instruction + " at index " + vm.PC);
		}
		switch (decode) {
			case Add: return new BCAdd(null);
			case Store: return new BCStore(null);
			case Character: return new BCCharacter(null,Char.get(vm.getInt()));
			case Group: return new BCGroup(null,CharacterGroup.get(vm.getInt()));
			case Integer: return new BCInteger(null,vm.getInt());
			case BranchIfZero: return new BCBranchIfZero(null,vm.getInt());
			case Avatar: return new BCAvatar(null,User.get(vm.getInt()));
			case String:
				int length = vm.getShort();
				byte[] string = new byte[length];
				try { System.arraycopy(vm.bytecode, vm.PC, string, 0, length); } catch (RuntimeException e)
				{
					throw new SystemException("Failed to arraycopy " + length + " from pos " + vm.PC, e);
				}
				vm.PC += length;
				String str = new String(string);
				return new BCString(null,str);
			case Debug: return new BCDebug(null,vm.getShort(),vm.getShort());
			case Divide: return new BCDivide(null);
			case Equality: return new BCEquality(null);
			case Inequality: return new BCInequality(null);
			case Initialise: return new BCInitialise(null);
			case Invoke: return new BCInvoke(null);
			case Load: return new BCLoad(null);
			case Multiply: return new BCMultiply(null);
			case Response: return new BCResponse(null);
			case Subtract: return new BCSubtract(null);
			case List: return new BCList(null,vm.getShort());
			case LoadIndexed: return new BCLoadIndexed(null);
			case StoreIndexed: return new BCStoreIndexed(null);
			case LessThan: return new BCLessThan(null);
			case GreaterThan: return new BCGreaterThan(null);
		}
		throw new SystemException("Failed to materialise instruction "+decode);
	}

	public abstract String explain();
	public abstract void toByteCode(List<Byte> bytes);
	public static Map<Byte,InstructionSet> map=new HashMap<>();

	public enum InstructionSet { // max 255 instructions (haha)
		Debug((byte)0),
		Add((byte)1),
		Store((byte)2),
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
		Load((byte)13),
		Multiply((byte)14),
		Response((byte)15),
		String((byte)16),
		Subtract((byte)17),
		List((byte)18),
		LoadIndexed((byte)19),
		StoreIndexed((byte)20),
		GreaterThan((byte)21),
		LessThan((byte)22);
		private byte value;


		private InstructionSet(byte value) {
			this.value = value;map.put(value,this);
		}
		public byte get() { return value; }
	}
	public static InstructionSet get(byte b) { return map.get(b); }
	public String htmlDecode() {
		return this.getClass().getSimpleName().replaceFirst("BC","")+"</td><td>";
	}

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

	public abstract void execute(GSVM vm);
}
