package com.dewire.dbdiff;
import java.util.ArrayList;
import java.util.Comparator;

/*
 * Created on 2006-apr-18
 */

public class CompareableTuple<T> extends ArrayList<T> implements Comparable<T> {

	private final ComparisonRules comparators;

	public CompareableTuple(int i,ComparisonRules comparators) {
		super(i);
		this.comparators = comparators;
	}

	public CompareableTuple(ComparisonRules comparators) {
		super();
		this.comparators = comparators;
	}

	public CompareableTuple() {
		super();
		comparators = null;
	}

	public int compareTo(T o) {
		CompareableTuple ot=(CompareableTuple)o;
		if (ot == null || size() != ot.size())
			throw new IllegalArgumentException("Only another compareble of the same size is acceptable");
		for (int i=0; i<size(); ++i) {
			T o1 = get(i);
			Object o2 = ot.get(i);
			if (o1 == o2) // quick null check
				continue;
			if (o1 == null)
				return -1;
			if (o2 == null)
				return 1;
			int c;
			if (comparators != null) {
				Comparator comparable = comparators.getTypeComparator(o1.getClass());
				c = comparable.compare(o1, o2);
			} else
				c = ((Comparable)o1).compareTo(o2);
			if (c!=0)
				return c;
		}
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		return compareTo((T) o) == 0;
	}

}
