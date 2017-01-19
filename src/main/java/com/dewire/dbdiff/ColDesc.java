package com.dewire.dbdiff;

import com.dewire.dbdiff.TableDiff.ConditionalComparator;

import java.util.List;
import java.util.Map;

public class ColDesc implements Comparable<ColDesc> {
	final String name;
	private final ConditionalComparator comparator;
	private final List<Map<String, List<IgnoreValue>>> ignoreIfKey;

	ColDesc(String name, ConditionalComparator comparator, List<Map<String, List<IgnoreValue>>> ignoreList) {
		this.name = name;
		this.comparator = comparator;
		this.ignoreIfKey = ignoreList;

	}

	@Override
	public boolean equals(Object obj) {
		ColDesc o = (ColDesc) obj;
		return name.equals(o.name); // && fuzzy == o.fuzzy;
	}

	public int compareTo(ColDesc o) {
		return name.compareTo(o.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name + ((getComparator() != null) ? "~" : "");
	}

	public ConditionalComparator getComparator() {
		return comparator;
	}

	public List<Map<String, List<IgnoreValue>>> getIgnoreIfKey() {
		return ignoreIfKey;
	}
}
