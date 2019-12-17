package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ByteCode {

	@Nullable
	private ParseNode sourcenode;
	public ByteCode(@Nullable final ParseNode n) { sourcenode=n; }
	@Nonnull
	public ByteCode node(final ParseNode n)
	{ sourcenode=n; return this; }
	@Nullable
	public ParseNode node() { return sourcenode; }


	@Nonnull
	public static ByteCode load(@Nonnull final GSVM vm) {
		final byte instruction = vm.bytecode[vm.PC];
		final InstructionSet decode = ByteCode.get(instruction);
		if (decode == null) {
			throw new GSInternalError("Unable to decode instruction " + instruction + " at index " + vm.PC);
		}
		vm.PC++;
		switch (decode) {
			case Add: return new BCAdd(null);
			case Store: return new BCStore(null);
			case Character: return new BCCharacter(null,Char.get(vm.getInt()));
			case Group: return new BCGroup(null,CharacterGroup.get(vm.getInt()));
			case Integer: return new BCInteger(null,vm.getInt());
			case BranchIfZero: return new BCBranchIfZero(null,vm.getInt());
			case Avatar: return new BCAvatar(null,User.get(vm.getInt()));
			case String:
				final int length = vm.getShort();
				final byte[] string = new byte[length];
				try { System.arraycopy(vm.bytecode, vm.PC, string, 0, length); } catch (@Nonnull final RuntimeException e)
				{
					throw new GSInternalError("Failed to arraycopy " + length + " from pos " + vm.PC, e);
				}
				vm.PC += length;
				final String str = new String(string);
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
			case LessThanEqual: return new BCLessThanEqual(null);
			case GreaterThanEqual: return new BCGreaterThanEqual(null);
		}
		throw new SystemImplementationException("Failed to materialise instruction "+decode);
	}

	@Nonnull
	public abstract String explain();
	public abstract void toByteCode(List<Byte> bytes);
	public static final Map<Byte,InstructionSet> map=new HashMap<>();

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
		LessThan((byte)22),
		GreaterThanEqual((byte)23),
		LessThanEqual((byte)24);
		private final byte value;


		InstructionSet(final byte value) {
			this.value = value;map.put(value,this);
		}
		public byte get() { return value; }
	}
	public static InstructionSet get(final byte b) { return map.get(b); }
	@Nullable
	public String htmlDecode() {
		return getClass().getSimpleName().replaceFirst("BC","")+"</td><td>";
	}

	void addInt(@Nonnull final List<Byte> bytes, final int a) {
		/*System.out.println("Writing "+
				((byte)((a>>24) & 0xff))+" "+
				((byte)((a>>16) & 0xff))+" "+
				((byte)((a>>8) & 0xff))+" "+
				((byte)(a&0xff)));*/
		bytes.add((byte)((a>>24) & 0xff));
		bytes.add((byte)((a>>16) & 0xff));
		bytes.add((byte)((a>>8) & 0xff));
		bytes.add((byte)(a&0xff));
	}
	void addShort(@Nonnull final List<Byte> bytes, final int a) {
		bytes.add((byte)((a>>8) & 0xff));
		bytes.add((byte)(a&0xff));
	}

	public abstract void execute(State st, GSVM vm, boolean simulation);
}
