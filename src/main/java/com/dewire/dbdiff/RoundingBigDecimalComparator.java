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
public class RoundingBigDecimalComparator implements Comparator<BigDecimal> {

	private final double fuzz;

	/**
	 * @param d
	 *            considered fuzz
	 */
	public RoundingBigDecimalComparator(double d) {
		assert d < 1;
		assert d >= 0;
		this.fuzz = d;
	}

	/**
	 * @param d
	 *            considered fuzz
	 */
	public RoundingBigDecimalComparator(Double d) {
		this(d.doubleValue());
	}

	public int compare(BigDecimal d1, BigDecimal d2) {
		Double o1 = d1.doubleValue();
		Double o2 = d2.doubleValue();
		double o1min = lower(o1);
		double o2min = lower(o2);
		double o1max = upper(o1);
		double o2max = upper(o2);
		if (o1 >= o2min && o1 <= o2max)
			return 0;
		if (o2 >= o1min && o2 <= o1max)
			return 0;
		return o1.compareTo(o2);
	}

	public double lower(double o1) {
		double oabs = Math.abs(o1);
		double lower = o1 - oabs * fuzz;
		return lower;
	}

	public double upper(double o1) {
		double oabs = Math.abs(o1);
		double lower = o1 + oabs * fuzz;
		return lower;
	}
}