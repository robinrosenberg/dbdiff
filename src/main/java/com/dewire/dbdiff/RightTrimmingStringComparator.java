/**
 * 
 */
package com.dewire.dbdiff;

import java.util.Comparator;

/**
 * This class ignores trailing spaces
 */
public class RightTrimmingStringComparator implements Comparator<String> {

	public RightTrimmingStringComparator() {
	}

	public int compare(String o1, String o2) {
		String s1 = rtrim(o1);
		String s2 = rtrim(o2);
		return s1.compareTo(s2);
	}

	static String rtrim(String s) {
		for (int i = s.length() - 1; i >= 0; --i) {
			if (s.charAt(i) != ' ')
				return s.substring(0, i+1);
		}
		return "";
	}
}