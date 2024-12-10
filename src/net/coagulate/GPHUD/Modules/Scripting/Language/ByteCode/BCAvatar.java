package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCAvatar extends ByteCodeDataType {
	@Nullable private User content;
	
	public BCAvatar(final ParseNode node) {
		super(node);
	}
	
	public BCAvatar(final ParseNode node,@Nullable final User content) {
		super(node);
		this.content=content;
	}
	
	// ---------- INSTANCE ----------
	@Nullable
	public User getContentNullable() {
		return content;
	}
	
	@Nonnull
	public String explain() {
		return "Avatar ("+content+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Avatar.get());
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
		return "Avatar</td><td>"+getContent().getId();
	}
	
	@Nonnull
	public User getContent() {
		if (content==null) {
			throw new GSInternalError("getContent on null content");
		}
		return content;
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		// easy
		vm.push(this);
	}
	
	//<STRING> | <RESPONSE> | <INT> | <CHARACTER> | <AVATAR> | <GROUP> | "List"
	@Nonnull
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		// only makes sense if adding a string to an avatar, in which case we string everything
		if (var.getClass().equals(BCString.class)) {
			return new BCString(node(),toString()+var);
		}
		throw fail(var);
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Override
	public ByteCodeDataType unaryMinus() {
		throw fail();
	}
	
	@Nonnull
	@Override
	public BCInteger valueEquals(@Nonnull final ByteCodeDataType var) {
		if (var instanceof BCAvatar) {
			return toBoolean(getContent().getId()==((BCAvatar)var).getContent().getId());
		}
		if (var instanceof BCString) {
			return toBoolean(getContent().getName().equalsIgnoreCase(((BCString)var).getContent()));
		}
		if (var instanceof BCInteger) {
			return toBoolean(getContent().getId()==((BCInteger)var).getContent());
		}
		throw fail(var);
	}
	
	@Nonnull
	@Override
	public BCInteger lessThan(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	@Override
	public BCInteger greaterThan(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Override
	public BCInteger not() {
		throw fail();
	}
	
	@Nonnull
	@Override
	public BCInteger toBCInteger() {
		return new BCInteger(null,getContent().getId());
	}
	
	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),getContent().getName());
	}
	
	@Nonnull
	@Override
	public BCFloat toBCFloat() {
		return new BCFloat(null,((float)getContent().getId()));
	}
	
	@Override
	public boolean toBoolean() {
		throw fail();
	}
	
	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCAvatar(node(),getContent());
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */ public boolean strictlyEquals(
			final ByteCodeDataType find) {
		if (!(find instanceof BCAvatar)) {
			return false;
		}
		return ((BCAvatar)find).getContent().getId()==getContent().getId();
	}
}
