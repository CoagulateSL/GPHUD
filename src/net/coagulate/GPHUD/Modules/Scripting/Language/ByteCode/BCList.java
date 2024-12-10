package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSArrayIndexOutOfBoundsException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BCList extends ByteCodeDataType {
	
	final List<ByteCodeDataType> content=new ArrayList<>(); // used by the VM
	int elements; // used by the compiler
	
	public BCList(final ParseNode node) {
		super(node);
	}
	
	public BCList(final ParseNode node,final int elements) {
		super(node);
		this.elements=elements;
	}
	
	public BCList(final ParseNode node,final ByteCodeDataType e) {
		super(node);
		content.add(e);
		elements++;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explain() {
		return "List (#"+elements+")";
	}
	
	@Override
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.List.get());
		addShort(bytes,elements);
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		final StringBuilder sb=new StringBuilder();
		sb.append("List</td><td>").append(elements).append("[");
		boolean comma=false;
		for (final ByteCodeDataType element:content) {
			if (comma) { sb.append(","); } else { comma=true; }
			sb.append(element.toString());
		}
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		// pull the list from the stack!
		for (int i=0;i<elements;i++) {
			final ByteCodeDataType data=vm.pop();
			if (data instanceof BCList) {
				throw new GSInvalidExpressionException("You can not nest a List inside a List",true);
			}
			content.add(data);
		}
		elements=content.size();
		vm.push(this);
	}
	
	@Nonnull
	public List<ByteCodeDataType> getContent() {
		return content;
	}
	
	@Nonnull
	@Override
	/** Performs mathematical addition upon a list, that is, it takes two lists and adds them together to produce a third list for assignment.
	 * NOT TO BE CONFUSED WITH APPEND.  Would I ever.
	 */ public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		final BCList newlist=new BCList(node());
		newlist.addAll(this);
		if (var.getClass().equals(BCList.class)) {
			final BCList varlist=(BCList)var;
			newlist.addAll(varlist);
		} else {
			newlist.append(var);
		}
		return newlist;
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
		if (var instanceof final BCList v) {
			if (v.size()!=size()) {
				return falseness();
			}
			for (int i=0;i<v.size();i++) {
				if (!v.getElement(i).valueEquals(getElement(i)).toBoolean()) {
					return falseness();
				}
			}
			return truth();
		}
		if (var instanceof BCInteger) {
			return toBoolean(((BCInteger)var).getContent()==size());
		}
		throw fail(var);
	}
	
	@Nonnull
	@Override
	public BCInteger lessThan(@Nonnull final ByteCodeDataType var) {
		if (var instanceof BCInteger) {
			return toBoolean(((BCInteger)var).getContent()<size());
		}
		throw fail(var);
	}
	
	@Nonnull
	public BCInteger toBCInteger() {
		return new BCInteger(null,elements);
	}
	
	@Nonnull
	@Override
	public BCInteger greaterThan(@Nonnull final ByteCodeDataType var) {
		if (var instanceof BCInteger) {
			return toBoolean(((BCInteger)var).getContent()>size());
		}
		throw fail(var);
	}
	
	@Nonnull
	@Override
	public String toString() {
		final StringBuilder ret=new StringBuilder(elements+"[");
		boolean needscomma=false;
		for (final ByteCodeDataType byteCodeDataType: content) {
			if (needscomma) {
				ret.append(",");
			} else {
				needscomma=true;
			} // not first element only
			ret.append(byteCodeDataType);
		}
		ret.append("]");
		return ret.toString();
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType clone() {
		final BCList clone=new BCList(node());
		clone.elements=elements;
		for (final ByteCodeDataType element: content) {
			clone.content.add(element.clone());
		}
		return clone;
	}
	
	@Override
	public BCInteger not() {
		throw fail();
	}
	
	@Nonnull
	@Override
	public BCFloat toBCFloat() {
		return new BCFloat(null,((float)(size())));
	}
	
	@Nonnull
	/** Appends a BCDT to the existing list */ public BCList append(final ByteCodeDataType value) {
		content.add(value);
		elements++;
		return this;
	}
	
	public void addAll(final BCList var) {
		content.addAll(var.content);
		elements=content.size();
	}
	
	public boolean isEmpty() {
		return elements==0;
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */ public boolean strictlyEquals(
			final ByteCodeDataType find) {
		if (!(find instanceof final BCList findl)) {
			return false;
		}
		if (this==findl) {
			return true;
		} // trivial case ; exact same object so no need to go into depth
		if (findl.size()!=size()) {
			return false;
		} // size mismatch
		if (size()==0) {
			return true;
		} // must be same sizes, both empty, easy match
		for (int i=0;i<size();i++) {
			if (!findl.getElement(i).strictlyEquals(getElement(i))) {
				// element mismatch
				return false;
			}
		}
		return true;
	}
	
	public int size() {
		return elements;
	}
	
	@Nonnull
	@Override
	public BCString toBCString() {
		final StringBuilder sb=new StringBuilder();
		sb.append("[");
		boolean commait=false;
		for (final ByteCodeDataType data: getContent()) {
			if (commait) {
				sb.append(",");
			} else {
				commait=true;
			}
			sb.append(data.toString());
		}
		sb.append("]");
		return new BCString(null,sb.toString());
	}

	@Override
	public boolean toBoolean() {
		return size()>0;
	}
	
	public ByteCodeDataType getElement(final int i) {
		if (i>=content.size()) { throw new GSArrayIndexOutOfBoundsException("Index "+i+" is greater than or equal to the size "+content.size()); }
		return content.get(i);
	}
	
}
