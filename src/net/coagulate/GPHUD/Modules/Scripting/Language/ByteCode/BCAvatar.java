package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCAvatar extends ByteCodeDataType {
	@Nullable
	private User content=null;
	@Nullable
	public User getContentNullable() { return content; }
	@Nonnull
	public User getContent() {
		if (content==null) { throw new SystemException("getContent on null content"); }
		return content;
	}
	public BCAvatar(ParseNode n) { super(n); }
	public BCAvatar(ParseNode node, @Nullable User content) {
		super(node);
		this.content=content; }
	@Nonnull
	public String explain() { return "Avatar ("+content+")"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Avatar.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
	@Nonnull
	@Override public String htmlDecode() { return "Avatar</td><td>"+ getContent().getId(); }

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		// easy
		vm.push(this);
	}
	//<STRING> | <RESPONSE> | <INT> | <CHARACTER> | <AVATAR> | <GROUP> | "List"
	@Nonnull
	@Override
	public ByteCodeDataType add(@Nonnull ByteCodeDataType var) {
		// only makes sense if adding a string to an avatar, in which case we string everything
		if (var.getClass().equals(BCString.class)) { return new BCString(node(),toString()+var.toString()); }
		throw new GSInvalidExpressionException("Can not perform BCAvatar + "+var.getClass().getSimpleName());
	}

	@Nonnull
	@Override
	public ByteCodeDataType subtract(@Nonnull ByteCodeDataType var) {
		// never makes sense
		throw new GSInvalidExpressionException("Can not perform subtraction on a BCAvatar");
	}

	@Nonnull
	@Override
	public ByteCodeDataType multiply(@Nonnull ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not perform multiplication on a BCAvatar");
	}

	@Nonnull
	@Override
	public ByteCodeDataType divide(@Nonnull ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not perform division on a BCAvatar");
	}

	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),getContent().getName());
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCAvatar(node(),getContent());
	}
}
