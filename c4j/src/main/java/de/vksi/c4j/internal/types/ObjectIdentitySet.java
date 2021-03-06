package de.vksi.c4j.internal.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Set containing Objects. Instead of relying on hashCode() for equality, this set relies on the == operator.
 */
public class ObjectIdentitySet implements Set<Object> {
	private Map<Integer, List<Object>> map = new HashMap<Integer, List<Object>>();

	@Override
	public int size() {
		int size = 0;
		for (List<Object> list : map.values()) {
			size += list.size();
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		Integer hashCode = getHashCode(o);
		if (!map.containsKey(hashCode)) {
			return false;
		}
		for (Object entry : map.get(hashCode)) {
			if (entry == o) {
				return true;
			}
		}
		return false;
	}

	private Integer getHashCode(Object o) {
		return Integer.valueOf(System.identityHashCode(o));
	}

	@Override
	public Iterator<Object> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	private Object getEntry(Object o) {
		Integer hashCode = getHashCode(o);
		if (!map.containsKey(hashCode)) {
			return null;
		}
		for (Object entry : map.get(hashCode)) {
			if (entry == o) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public boolean add(Object e) {
		if (e == null) {
			return false;
		}
		Object entry = getEntry(e);
		if (entry != null) {
			return false;
		}
		Integer hashCode = getHashCode(e);
		if (!map.containsKey(hashCode)) {
			map.put(hashCode, new ArrayList<Object>());
		}
		map.get(hashCode).add(e);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if (!contains(o)) {
			return false;
		}
		Integer hashCode = getHashCode(o);
		for (Iterator<Object> iterator = map.get(hashCode).iterator(); iterator.hasNext();) {
			Object entry = iterator.next();
			if (entry == o) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Object> c) {
		boolean changed = false;
		for (Object o : c) {
			if (add(o)) {
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			if (remove(o)) {
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public void clear() {
		map.clear();
	}
}
