package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSMathsError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCInteger extends ByteCodeDataType {
	private Integer content=0;
	
	public BCInteger(final ParseNode node) {
		super(node);
	}
	
	public BCInteger(final ParseNode n,final Integer content) {
		super(n);
		this.content=content;
	}
	
	public BCInteger(final ParseNode n,@Nonnull final String tokens) {
		super(n);
		content=Integer.parseInt(tokens);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() {
		return "Integer ("+content+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Integer.get());
		if (content==null) {
			bytes.add((byte)0);
			bytes.add((byte)0);
			bytes.add((byte)0);
			bytes.add((byte)0);
			return;
		}
		addInt(bytes,content);
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		return "Integer</td><td>"+content;
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	public int getContent() {
		return content;
	}
	
	@Nullable
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		// if the other is a List, we'll just be appending ourselves to them
		if (var.getClass().equals(BCList.class)) {
			final BCList ret=new BCList(node());
			ret.append(this);
			ret.addAll((BCList)var);
			return ret;
		}
		// if the other is a String, we'll just be doing that
		if (var.getClass().equals(BCString.class)) {
			return toBCString().add(var);
		}
		// if the other is a Float, we should cast down to it.  but that's not how we do things yet.
		return new BCInteger(node(),toInteger()+var.toInteger());
	}
	
	@Nullable
	@Override
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(node(),toInteger()-var.toInteger());
	}
	
	@Nullable
	@Override
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(node(),toInteger()*var.toInteger());
	}
	
	@Nullable
	@Override
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		//check float, eventually
		if (var.toInteger()==0) {
			throw new GSMathsError("Division by zero");
		}
		return new BCInteger(node(),toInteger()/var.toInteger());
	}
	
	@Override
	public ByteCodeDataType unaryMinus() {
		return new BCInteger(node(),-content);
	}
	
	@Nonnull
	@Override
	public BCInteger valueEquals(@Nonnull final ByteCodeDataType var) {
		return toBoolean(toInteger()==var.toInteger());
	}
	
	@Nonnull
	@Override
	public BCInteger lessThan(@Nonnull final ByteCodeDataType var) {
		return toBoolean(toInteger()<var.toInteger());
	}
	
	@Nonnull
	@Override
	public BCInteger greaterThan(@Nonnull final ByteCodeDataType var) {
		return toBoolean(toInteger()>var.toInteger());
	}
	
	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(node(),content.toString());
	}
	
	@Override
	public BCInteger not() {
		return toBoolean(!toBoolean());
	}
	
	@Nonnull
	@Override
	public BCFloat toBCFloat() {
		return new BCFloat(node(),content.floatValue());
	}
	
	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCInteger(node(),content);
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */ public boolean strictlyEquals(
			final ByteCodeDataType find) {
		if (!(find instanceof BCInteger)) {
			return false;
		}
		return ((BCInteger)find).content.intValue()==content.intValue();
	}

	@Nonnull
	@Override
	public BCInteger toBCInteger() {
		return this;
	}
	
	@Override
	public boolean toBoolean() {
		return getContent()!=0;
	}
}
