/**
 * 
 */
package com.dewire.dbdiff;

import java.util.Comparator;

/**
 * This class considers all numbers within <code>fuzz</code> of each number and
 * considers.
 */
public class DeltaIntegerComparator implements Comparator<Integer> {

	private final int fuzz;

	/**
	 * @param d
	 *            considered fuzz
	 */
	public DeltaIntegerComparator(int d) {
		assert d >= 0;
		this.fuzz = d;
	}

	/**
	 * @param part
	 *            considered fuzz
	 */
	public DeltaIntegerComparator(Integer d) {
		this(d.intValue());
	}

	public int compare(Integer o1, Integer o2) {
		int o1min = lower(o1);
		int o2min = lower(o2);
		int o1max = upper(o1);
		int o2max = upper(o2);
		if (o1 >= o2min && o1 <= o2max)
			return 0;
		if (o2 >= o1min && o2 <= o1max)
			return 0;
		return o1.compareTo(o2);
	}

	public int lower(int o1) {
		return o1 - fuzz;
	}

	public int upper(int o1) {
		return o1 + fuzz;
	}
}