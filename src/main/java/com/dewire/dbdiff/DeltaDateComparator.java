/**
 * 
 */
package com.dewire.dbdiff;

import java.sql.Date;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;

/**
 * This class considers all dates within <code>fuzz</code> of each other equal.
 */
public class DeltaDateComparator implements Comparator<Date> {

	private final int fuzz;

	/**
	 * @param d
	 *            considered fuzz
	 */
	public DeltaDateComparator(int d) {
		assert d >= 0;
		this.fuzz = d;
	}

	/**
	 * @param part
	 *            considered fuzz
	 */
	public DeltaDateComparator(Integer d) {
		this(d.intValue());
	}

	public int compare(Date d1, Date d2) {
		long o1 = d1.getTime();
		long o2 = d2.getTime();
		long o1min = lower(o1);
		long o2min = lower(o2);
		long o1max = upper(o1);
		long o2max = upper(o2);
		if (o1 >= o2min && o1 <= o2max)
			return 0;
		if (o2 >= o1min && o2 <= o1max)
			return 0;
		return d1.compareTo(d2);
	}

	public long lower(long o1) {
		Calendar instance = GregorianCalendar.getInstance();
		instance.setTimeInMillis(o1);
		instance.add(Calendar.DATE, -fuzz);
		return instance.getTimeInMillis();
	}

	public long upper(long o1) {
		Calendar instance = GregorianCalendar.getInstance();
		instance.setTimeInMillis(o1);
		instance.add(Calendar.DATE, fuzz);
		return instance.getTimeInMillis();
	}
}