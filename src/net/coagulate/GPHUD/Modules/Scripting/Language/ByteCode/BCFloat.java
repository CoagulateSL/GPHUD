package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCFloat extends ByteCodeDataType {
	private Float content= 0.0f;

	public BCFloat(final ParseNode node) {
        super(node);
    }

	public BCFloat(final ParseNode n,
                   final Float content) {
		super(n);
		this.content=content;
	}

	public BCFloat(final ParseNode n,
                   @Nonnull final String tokens) {
		super(n);
		content=Float.parseFloat(tokens);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() { return "Float ("+content+")"; }

	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Float.get());
		if (content==null) {
			bytes.add((byte) 0);
			bytes.add((byte) 0);
			bytes.add((byte) 0);
			bytes.add((byte) 0);
			return;
		}
		addFloat(bytes,content);
	}

	@Nonnull
	@Override
	public String htmlDecode() { return "Float</td><td>"+content; }

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		vm.push(this);
	}

	public float getContent() { return content; }

	@Nullable
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		// if the other is a List, we'll just be appending ourselves to them
		if (var.getClass().equals(BCList.class)) {
			final BCList ret=new BCList(node());
			ret.append(this);
			ret.addAll((BCList) var);
			return ret;
		}
		// if the other is a String, we'll just be doing that
		if (var.getClass().equals(BCString.class)) { return toBCString().add(var); }
		// if the other is a Float, we should cast down to it.  but that's not how we do things yet.
		return new BCFloat(node(),toFloat()+var.toFloat());
	}

	@Nullable
	@Override
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		//check float, eventually
		return new BCFloat(node(),toFloat()-var.toFloat());
	}

	@Nullable
	@Override
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		//check float, eventually
		return new BCFloat(node(),toFloat()*var.toFloat());
	}

	@Nullable
	@Override
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		//check float, eventually
		return new BCFloat(node(),toFloat()/var.toFloat());
	}

	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),content.toString());
	}

	@Nonnull
	public BCInteger toBCInteger() {
		return new BCInteger(node(),content.intValue());
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCFloat(node(),content);
	}
}
