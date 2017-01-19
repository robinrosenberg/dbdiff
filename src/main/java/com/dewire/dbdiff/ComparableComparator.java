/**
 * 
 */
package com.dewire.dbdiff;

import java.util.Comparator;

class ComparableComparator implements Comparator<Comparable> {

	public int compare(Comparable o1, Comparable o2) {
		return o1.compareTo(o2);
	}
}