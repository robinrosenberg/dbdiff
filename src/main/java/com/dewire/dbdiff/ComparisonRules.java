package com.dewire.dbdiff;

import java.util.*;

public class ComparisonRules {

	Map<Class<?>, Comparator> comparators = new HashMap<Class<?>, Comparator>();
	{
		comparators.put(Comparable.class, new ComparableComparator());
	}

	public Comparator<Object> getTypeComparator(Class<?> class1) {
		if (class1.isPrimitive()) {
			class1 = getWrapperClass(class1);
		}
		for (Class c = class1; c != null ; c=c.getSuperclass()) {
			Comparator<Object> ret = comparators.get(c);
			if (ret != null) {
				if (c != class1) {
					// Cache
					comparators.put(c, ret);
				}
				return ret;
			}
		}
		return getTypeComparatorForInterfaces(class1);
	}


	public void addComparator(Class<?> class1, Comparator<?> comparator) {
		if (comparators.get(class1) != null)
			throw new IllegalStateException("Comparator for class " + class1.getClass() + " already added");
		comparators.put(class1, comparator);
	}

	public static Class<?> getWrapperClass(Class<?> class1) {
		if (class1 == Integer.TYPE)
			return Integer.class;
		if (class1 == Long.TYPE)
			return Long.class;
		if (class1 == Short.TYPE)
			return Short.class;
		if (class1 == Byte.TYPE)
			return Byte.class;
		if (class1 == Character.TYPE)
			return Character.class;
		if (class1 == Float.TYPE)
			return Float.class;
		if (class1 == Double.TYPE)
			return Double.class;
		if (class1 == Boolean.TYPE)
			return Boolean.class;
		return null;
	}

	private Comparator<Object> getTypeComparatorForInterfaces(Class<?> class1) {
		List<Class> toExamine = new LinkedList<Class>();
		for (Class<?> c = class1; c!= null; c = c.getSuperclass())
			collectInterfaces(c, toExamine);
		for (Class<?> c : toExamine) {
			Comparator<Object> ret = comparators.get(c);
			if (ret != null) {
				if (c != class1) {
					// Cache
					comparators.put(c, ret);
				}
				return ret;
			}
		}
		return null;
	}

	private void collectInterfaces(Class<?> class1, List<Class> toExamine) {
		toExamine.add(class1);
		for (Class<?> c : class1.getInterfaces())
			collectInterfaces(c, toExamine);
	}
}
