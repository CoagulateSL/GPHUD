package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCInteger extends ByteCodeDataType {
	private Integer content=0;
	public BCInteger(ParseNode n) { super(n); }
	public BCInteger(ParseNode n,Integer content) { super(n); this.content=content; }
	public BCInteger(ParseNode n, @Nonnull String tokens) { super(n); this.content=Integer.parseInt(tokens); }

	@Nonnull
	public String explain() { return "Integer ("+content+")"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Integer.get());
		if (content==null) { bytes.add((byte)0); bytes.add((byte)0); bytes.add((byte)0); bytes.add((byte)0);return; }
		addInt(bytes,content);
	}
	@Nonnull
	@Override public String htmlDecode() { return "Integer</td><td>"+content; }

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		vm.push(this);
	}

	public int getContent() { return content; }

	@Nullable
	@Override
	public ByteCodeDataType add(@Nonnull ByteCodeDataType var) {
		// if the other is a String, we'll just be doing that
		if (var.getClass().equals(BCString.class)) { return toBCString().add(var); }
		// if the other is a Float, we should cast down to it.  but that's not how we do things yet.
		return new BCInteger(node(),toInteger()+var.toInteger());
	}

	@Nullable
	@Override
	public ByteCodeDataType subtract(@Nonnull ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(node(),toInteger()-var.toInteger());
	}

	@Nullable
	@Override
	public ByteCodeDataType multiply(@Nonnull ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(node(),toInteger()*var.toInteger());
	}

	@Nullable
	@Override
	public ByteCodeDataType divide(@Nonnull ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(node(),toInteger()/var.toInteger());
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCInteger(node(),content);
	}

	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),content.toString());
	}
}
