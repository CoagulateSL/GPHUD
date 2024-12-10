package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCGroup extends ByteCodeDataType {
	@Nullable private CharacterGroup content;
	
	public BCGroup(final ParseNode node) {
		super(node);
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		if (var instanceof BCString) { return new BCString(null,this+((BCString)var).getContent()); }
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
		if (var instanceof BCGroup) { return toBoolean(content.getId()==((BCGroup)var).content.getId()); }
		if (var instanceof BCString) { return toBoolean(content.getName().equalsIgnoreCase(var.toString())); }
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
	
	public BCGroup(final ParseNode node,@Nullable final CharacterGroup content) {
		super(node);
		this.content=content;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() {
		return "Group ("+content+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Group.get());
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
	public BCInteger toBCInteger() {
		return new BCInteger(null,content.getId());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	@Nonnull
	public CharacterGroup getContent() {
		if (content==null) {
			throw new GSInternalError("Group has no content (group)");
		}
		return content;
	}
	
	@Nullable
	public CharacterGroup getContentNullable() {
		return content;
	}
	
	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCGroup(node(),content);
	}
	
	@Nonnull
	@Override
	public BCFloat toBCFloat() {
		return new BCFloat(null,((float)(content.getId())));
	}
	
	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(null,content.getName());
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */ public boolean strictlyEquals(
			final ByteCodeDataType find) {
		if (!(find instanceof BCGroup)) {
			return false;
		}
		return ((BCGroup)find).content.getId()==content.getId();
	}

	@Override
	public boolean toBoolean() {
		throw fail();
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		return "Group</td><td>"+getContent().getId();
	}
}
