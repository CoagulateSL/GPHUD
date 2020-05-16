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

	public BCList(final ParseNode n) {super(n);}

	public BCList(final ParseNode n,
	              final int elements) {
		super(n);
		this.elements=elements;
	}

	public BCList(final ParseNode n,
	              final ByteCodeDataType e) {
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
	public String htmlDecode() { return "List</td><td>"+elements; }

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		// pull the list from the stack!
		for (int i=0;i<elements;i++) {
			final ByteCodeDataType data=vm.pop();
			if (data instanceof BCList) {
				throw new GSInvalidExpressionException("You can not nest a List inside a List");
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
	public BCList append(final ByteCodeDataType value) {
		content.add(value);
		elements++;
		return this;
	}

	@Nonnull
	public BCInteger toBCInteger() {
		return new BCInteger(null,elements);
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

	@Nullable
	@Override
	public ByteCodeDataType add(@Nonnull ByteCodeDataType var) {
		BCList newlist=new BCList(node());
		if (var.getClass().equals(BCList.class)) {
			BCList varlist=(BCList) var;
			newlist.addAll(varlist);
		}
		else {
			newlist.append(var);
		}
		newlist.addAll(this);
		return newlist;
	}

	@Nonnull
	@Override
	public String toString() {
		String ret=elements+"[";
		boolean needscomma=false;
		for (int i=0;i<content.size();i++) {
			if (needscomma) { ret+=","; }
			else { needscomma=true; } // not first element only
			ret+=content.get(i);
		}
		ret+="]";
		return ret;
	}

	public void addAll(BCList var) {
		content.addAll(var.content);
		elements=content.size();
	}
}
