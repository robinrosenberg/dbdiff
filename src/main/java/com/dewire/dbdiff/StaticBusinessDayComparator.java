/**
 * 
 */
package com.dewire.dbdiff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.util.*;

/**
 * This class ignores differences if next next busingess day matches
 */
public class StaticBusinessDayComparator implements Comparator<Date> {

	static Map<String, Set<Date>> holiDays = new HashMap<String, Set<Date>>();
	static Set<Date> allHoliDays = new HashSet<Date>();
	private final int direction;
	private final String currency;
	static {

		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(StaticBusinessDayComparator.class
						.getResourceAsStream("holidays.txt")));
		String line;
		try {
			bufferedReader.readLine();
			bufferedReader.readLine();
			while ((line = bufferedReader.readLine()) != null && line.length() > 0) {
				String currency = line.substring(0, 3);
				Date date = Date.valueOf(line.substring(4));
				Set<Date> dates = holiDays.get(currency);
				if (dates == null) {
					dates = new HashSet<Date>();
					holiDays.put(currency, dates);
				}
				dates.add(date);
				allHoliDays.add(date);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a SimplisticBusinessDayComparator
	 * 
	 * @param calendar
	 */
	public StaticBusinessDayComparator(String currency) {
		this.currency = currency;
		this.direction = 0;
	}

	public StaticBusinessDayComparator(String currency, int direction) {
		this.currency = currency;
		this.direction = direction;
	}

	public StaticBusinessDayComparator(int direction) {
		this.currency = null;
		this.direction = direction;
	}

	public StaticBusinessDayComparator(Integer direction) {
		this.currency = null;
		this.direction = direction;
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
		if (currency != null)
			return holiDays.get(currency).contains(new Date(c.getTimeInMillis()));
		return allHoliDays.contains(new Date(c.getTimeInMillis()));
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