/**
 * 
 */
package com.dewire.dbdiff;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * This class considers all numbers within <code>fuzz</code> of each number and
 * considers.
 */
public class DeltaBigDecimalComparator implements Comparator<BigDecimal> {

	private final BigDecimal fuzz;

	/**
	 * @param d
	 *            considered fuzz
	 */
	public DeltaBigDecimalComparator(double d) {
		assert d >= 0;
		this.fuzz = BigDecimal.valueOf(d);
	}

	/**
	 * @param part
	 *            considered fuzz
	 */
	public DeltaBigDecimalComparator(Double d) {
		this(d.doubleValue());
	}

	public int compare(BigDecimal o1, BigDecimal o2) {
		BigDecimal o1min = lower(o1);
		BigDecimal o2min = lower(o2);
		BigDecimal o1max = upper(o1);
		BigDecimal o2max = upper(o2);
		if (o1.compareTo(o2min) >= 0 && o1.compareTo(o2max) <= 0)
			return 0;
		if (o2.compareTo(o1min) >= 0 && o2.compareTo(o1max) <= 0)
			return 0;
		return o1.compareTo(o2);
	}

	public BigDecimal lower(BigDecimal o1) {
		return o1.subtract(fuzz);
	}

	public BigDecimal upper(BigDecimal o1) {
		return o1.add(fuzz);
	}
}