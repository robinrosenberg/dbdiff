/**
 * 
 */
package com.dewire.dbdiff;

import java.sql.Date;
import java.util.*;

/**
 * This class ignores differences if next next busingess day matches
 */
public class SimplisticBusinessDayComparator implements Comparator<Date> {

	static Set<Date> holiDays = new HashSet<Date>();
	private final int direction;
	static {
		for (int i = 1990; i < 2037; ++i) {
			holiDays.add(Date.valueOf(i + "-01-01"));
			holiDays.add(Date.valueOf(i + "-12-31"));
			holiDays.add(Date.valueOf(i + "-12-24"));
			holiDays.add(Date.valueOf(i + "-12-25"));
			holiDays.add(Date.valueOf(i + "-12-26"));
			holiDays.add(Date.valueOf(i + "-05-01"));
		}
		holiDays.add(Date.valueOf("2011-06-24"));
		holiDays.add(Date.valueOf("2010-06-25"));
		holiDays.add(Date.valueOf("2009-06-26"));
		holiDays.add(Date.valueOf("2008-06-20"));
		holiDays.add(Date.valueOf("2007-06-22"));
		holiDays.add(Date.valueOf("2006-06-23"));
		holiDays.add(Date.valueOf("2005-06-24"));
		holiDays.add(Date.valueOf("2004-06-25"));
		holiDays.add(Date.valueOf("2003-06-20"));
		holiDays.add(Date.valueOf("2001-06-21"));
		holiDays.add(Date.valueOf("2001-06-22"));
		holiDays.add(Date.valueOf("2000-06-23"));
	}

	/**
	 * Create a SimplisticBusinessDayComparator
	 * 
	 * @param direction
	 *            -1 = adjust to previous business day <br>
	 *            1 = adjust to next business day <br>
	 *            0 = try both cases above. Note however that = this does not
	 *            make a normal friday match = a normal monday
	 */
	public SimplisticBusinessDayComparator(int direction) {
		this.direction = direction;
		assert direction == -1 || direction == 0 || direction == 1;
	}

	public SimplisticBusinessDayComparator(Integer direction) {
		this(direction.intValue());
	}

	public int compare(Date o1, Date o2) {
		if (o1.compareTo(o2) == 0)
			return 0;
		Date d1;
		Date d2;
		int diff = 0;
		if (direction >= 0) {
			d1 = bdtrimForward(o1);
			d2 = bdtrimForward(o2);
			diff = d1.compareTo(d2);
			if (diff == 0)
				return 0;
		}
		if (direction <= 0) {
			d1 = bdtrimBackward(o1);
			d2 = bdtrimBackward(o2);
			diff = d1.compareTo(d2);
		}
		return diff;
	}

	private boolean isHoliday(GregorianCalendar c) {
		int day = c.get(Calendar.DAY_OF_WEEK);
		if (day == Calendar.SATURDAY)
			return true;
		if (day == Calendar.SUNDAY)
			return true;
		return holiDays.contains(new Date(c.getTimeInMillis()));
	}

	private Date bdtrimForward(Date d) {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTime(d);
		while (isHoliday(gregorianCalendar)) {
			gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		return new Date(gregorianCalendar.getTimeInMillis());
	}

	private Date bdtrimBackward(Date d) {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTime(d);

		while (isHoliday(gregorianCalendar)) {
			gregorianCalendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		return new Date(gregorianCalendar.getTimeInMillis());
	}
}