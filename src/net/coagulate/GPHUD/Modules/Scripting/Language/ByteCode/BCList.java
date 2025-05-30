package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BCList extends ByteCodeDataType {
	
	final List<ByteCodeDataType> content=new ArrayList<>(); // used by the VM
	int elements; // used by the compiler
	
	public BCList(final ParseNode node) {
		super(node);
	}
	
	public BCList(final ParseNode n,final int elements) {
		super(n);
		this.elements=elements;
	}
	
	public BCList(final ParseNode n,final ByteCodeDataType e) {
		super(n);
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
		sb.append("List</td><td>").append(elements);
		sb.append(" [");
		boolean isfirst=true;
		for (final ByteCodeDataType e:content) {
			if (isfirst) { isfirst=false; } else { sb.append(", "); }
			sb.append(e.htmlDecode());
		}
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
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
	
	@Nullable
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
	public BCInteger toBCInteger() {
		return new BCInteger(null,elements);
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
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */
	public boolean strictlyEquals(final ByteCodeDataType find) {
		if (!(find instanceof final BCList findl)) {
			return false;
		}
		if (this==findl) { return true; } // trivial case ; exact same object so no need to go into depth
		if (findl.size()!=size()) { return false; } // size mismatch
		if (size()==0) { return true; } // must be same sizes, both empty, easy match
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
	
	public ByteCodeDataType getElement(final int i) {
		return content.get(i);
	}
}
