package aeminium.jparcompiler.model;

import java.util.ArrayList;

import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.declaration.CtElement;

public class PermissionSet extends ArrayList<Permission> {
	
	public static final String PERMISSION_MODEL_KEY = "permission";

	public boolean isComplete = false;
	private static final long serialVersionUID = 1L;
	
	public PermissionSet merge(PermissionSet a2) {
		PermissionSet b = this.copy();
		if (a2 != null) {
			for (Permission p : a2) {
				if (!b.contains(p))
					b.add(p.copy());
			}
		}
		return b;
	}

	public PermissionSet copy() {
		PermissionSet p2 = new PermissionSet();
		for (Permission p : this)
			p2.add(p.copy());
		return p2;
	}

	public void printSet() {
		for (Permission p: this) {
			System.out.print("  ");
			System.out.println(p);
		}
	}

	public void removeTarget(CtElement e) {
		PermissionSet rm = new PermissionSet();
		for (Permission p: this) {
			if (p.target == e) {
				rm.add(p);
			}
		}
		for (Permission p : rm) {
			this.remove(p);
		}
	}

	public void removeReturn() {
		PermissionSet rm = new PermissionSet();
		for (Permission p: this) {
			if (p.control) {
				rm.add(p);
			}
		}
		for (Permission p : rm) {
			this.remove(p);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void clean() {
		PermissionSet rm = new PermissionSet();
		for (Permission p: this) {
			if ( p instanceof CtTargetedExpression) {
				CtTargetedExpression pp = (CtTargetedExpression) p;
				if (pp.getTarget().getType().getQualifiedName().startsWith("java.lang.Math")) {
					rm.add(p);
				}
			}
		}
		for (Permission p : rm) {
			this.remove(p);
		}
	}

	public boolean add(Permission pn) {
		if (!this.contains(pn)) {
			return super.add(pn);
		}
		return false;
	}

	public boolean containsControl() {
		for (Permission p: this) {
			if (p.control) {
				return true;
			}
		}
		return false;
	}

	public int count(PermissionType pt) {
		int i = 0;
		for (Permission p: this) {
			if (p.type == pt) i++;
		}
		return i;
	}
	
	public Permission getTarget(CtElement e) {
		Permission p = null;
		for (Permission pi: this) {
			if (pi.target == e) {
				if (p == null) {
					p = pi;
				} else {
					if (pi.type != p.type) p.type = PermissionType.WRITE;
				}
				if (pi.control) p.control = true;
			}
		}
		return p;
	}
}
