package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCString extends ByteCodeDataType {
	@Nonnull private String content="";
	
	public BCString(final ParseNode node) {
		super(node);
	}
	
	public BCString(final ParseNode n,@Nonnull final String content) {
		super(n);
		if (content.length()>65535) {
			throw new GSResourceLimitExceededException("Attempt to make string longer than 65535 characters");
		}
		this.content=content;
	}
	
	@Nonnull
	public String explain() {
		return "String ("+content+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.String.get());
		addShort(bytes,content.length());
		for (final char c: content.toCharArray()) {
			bytes.add((byte)c);
		}
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		return "String</td><td>"+content;
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	@Nonnull
	@Override
	public BCInteger toBCInteger() {
		try {
			return new BCInteger(null,Integer.parseInt(getContent()));
		} catch (@Nonnull final NumberFormatException e) {
			throw new GSCastException("Can not cast the String '"+getContent()+"' to an Integer",e,true);
		}
	}
	
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		final Class<? extends ByteCodeDataType> type=var.getClass();
		// <STRING> | <RESPONSE> | <INT> | <CHARACTER> | <AVATAR> | <GROUP> | "List"
		if (type.equals(BCList.class)) { // special case, return a list
			return new BCList(node(),this).add(var);
		}
		// for everything else
		return new BCString(node(),content+(var.toBCString()));
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		// if the 2nd type is a number then we'll do number stuff :P
		if (var.getClass().equals(BCInteger.class)) {
			toBCInteger().subtract(var);
		}
		throw new GSInvalidExpressionException("Can not perform BCString - "+var.getClass().getSimpleName(),true);
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		// if the 2nd type is a number then we'll do number stuff :P
		if (var.getClass().equals(BCInteger.class)) {
			toBCInteger().subtract(var);
		}
		throw new GSInvalidExpressionException("Can not perform BCString * "+var.getClass().getSimpleName(),true);
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		// if the 2nd type is a number then we'll do number stuff :P
		if (var.getClass().equals(BCInteger.class)) {
			toBCInteger().subtract(var);
		}
		throw new GSInvalidExpressionException("Can not perform BCString / "+var.getClass().getSimpleName(),true);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String getContent() {
		return content;
	}
	
	@Nonnull
	@Override
	public BCFloat toBCFloat() {
		try {
			return new BCFloat(null,Float.parseFloat(getContent()));
		} catch (@Nonnull final NumberFormatException e) {
			throw new GSCastException("Can not cast the String '"+getContent()+"' to a Float",e,true);
		}
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType clone() {
		return new BCString(node(),content);
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */
	public boolean strictlyEquals(final ByteCodeDataType find) {
		if (!(find instanceof BCString)) {
			return false;
		}
		return ((BCString)find).content.equals(content);
	}
}
