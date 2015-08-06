package aeminium.jparcompiler.model;

import java.util.ArrayList;

import spoon.reflect.declaration.CtElement;

public class PermissionSet extends ArrayList<Permission> {

	public boolean isComplete = false;
	private static final long serialVersionUID = 1L;
	
	public PermissionSet merge(PermissionSet a2) {
		PermissionSet b = this.copy();
		if (a2 != null) {
			for (Permission p : a2) {
				if (!b.contains(p))
					b.add(p);
			}
		}
		return b;
	}

	public PermissionSet copy() {
		PermissionSet p2 = new PermissionSet();
		for (Permission p : this)
			p2.add(p);
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
	
	public void clean() {
		PermissionSet rm = new PermissionSet();
		for (Permission p: this) {
			if (p.target.getSignature().startsWith("java.lang.Math")) {
				rm.add(p);
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
			if (p.type == pt || ( pt == PermissionType.WRITE && p.type == PermissionType.READWRITE)) i++;
		}
		return i;
	}
	
}
