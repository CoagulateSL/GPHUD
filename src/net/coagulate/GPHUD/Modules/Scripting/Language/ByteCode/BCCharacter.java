package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCCharacter extends ByteCodeDataType {
	@Nullable private Char content;
	
	public BCCharacter(final ParseNode node) {
		super(node);
	}
	
	public BCCharacter(final ParseNode n,@Nonnull final Char content) {
		super(n);
		this.content=content;
	}
	
	@Nonnull
	public String explain() {
		return "Character ("+content+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Character.get());
		if (content==null) {
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			return;
		}
		addInt(bytes,content.getId());
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		return "Character</td><td>"+getContent().getId();
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public Char getContent() {
		if (content==null) {
			throw new GSInternalError("Getting an uninitialised BCCharacter's contents");
		}
		return content;
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	@Nullable
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		if (var.getClass().equals(BCString.class)) {
			return new BCString(node(),toString()+var);
		}
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
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */ public boolean strictlyEquals(
			final ByteCodeDataType find) {
		if (!(find instanceof BCCharacter)) {
			return false;
		}
		return ((BCCharacter)find).content.getId()==content.getId();
	}
}
