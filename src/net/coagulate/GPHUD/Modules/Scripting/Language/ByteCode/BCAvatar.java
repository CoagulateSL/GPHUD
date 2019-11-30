package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

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
	private User content=null; @Nullable
	public User getContent() { return content; }
	public BCAvatar(ParseNode n) { super(n); }
	public BCAvatar(ParseNode node,User content) {
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
	@Override public String htmlDecode() { return "Avatar</td><td>"+content.getId(); }

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
	public ByteCodeDataType subtract(ByteCodeDataType var) {
		// never makes sense
		throw new GSInvalidExpressionException("Can not perform subtraction on a BCAvatar");
	}

	@Nonnull
	@Override
	public ByteCodeDataType multiply(ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not perform multiplication on a BCAvatar");
	}

	@Nonnull
	@Override
	public ByteCodeDataType divide(ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not perform division on a BCAvatar");
	}

	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),content.getName());
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCAvatar(node(),content);
	}
}
