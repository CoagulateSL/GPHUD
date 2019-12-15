package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCCharacter extends ByteCodeDataType {
	public BCCharacter(final ParseNode n) { super(n); }
	@Nullable
	private Char content;
	@Nonnull
	public Char getContent() {
		if (content==null) { throw new SystemException("Getting an uninitialised BCCharacter's contents"); }
		return content;
	}
	public BCCharacter(final ParseNode n, @Nonnull final Char content) { super(n); this.content=content; }
	@Nonnull
	public String explain() { return "Character ("+content+")"; }
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Character.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
	@Nonnull
	@Override public String htmlDecode() { return "Character</td><td>"+getContent().getId(); }

	@Override
	public void execute(final State st, @Nonnull final GSVM vm, final boolean simulation) {
		vm.push(this);
	}

	@Nullable
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		if (var.getClass().equals(BCString.class)) { return new BCString(node(),
				toString() + var) ; }
		throw new GSInvalidExpressionException("Can't add BCCharacter + "+var.getClass().getSimpleName());
	}

	@Nonnull
	@Override
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can't subtract with BCCharacter");
	}

	@Nonnull
	@Override
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can't multiply with BCCharacter");
	}

	@Nonnull
	@Override
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can't divide with BCCharacter");
	}

	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),getContent().getName());
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCCharacter(node(),getContent());
	}

	public boolean isOnline() {
		return getContent().isOnline();
	}
}
