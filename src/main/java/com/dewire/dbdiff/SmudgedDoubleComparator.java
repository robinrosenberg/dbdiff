/**
 * 
 */
package com.dewire.dbdiff;

import java.util.Comparator;

/**
 * This class rounds doubles to a specific significant number of digits. An
 * advantage with this is that is fast.
 */
public class SmudgedDoubleComparator implements Comparator<Double> {

	public final long mask;

	/**
	 * @param bits bits of precision to keep <br>
	 * <b>rough guidelines</b>
	 * 4 = 1 decimal digit
	 * 8 = 2 decimal digit
	 */
	public SmudgedDoubleComparator(int bits) {
		assert bits <= 52;
		long unmask = (1L << (53 - bits)) - 1;
		mask = -1 & ~unmask;
	}

	public int compare(Double o1, Double o2) {
		double d1 = smudge(o1);
		double d2 = smudge(o2);
		return Double.compare(d1, d2);
	}

	public double smudge(double o1) {
		long raw1 = Double.doubleToRawLongBits(o1);
		raw1 &= mask;
		double d1 = Double.longBitsToDouble(raw1);
		return d1;
	}
}