package com.dewire.dbdiff;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public abstract class Reporter {
	protected final ComparisonRules comparisonRules;

	public Reporter(ComparisonRules comparisonRules) {
		this.comparisonRules = comparisonRules;
	}

	/**
	 * Get data for a diff from the current result set.
	 * 
	 * @param set
	 *            The JDBC result set row to get data from
	 * @param tableDescription
	 *            A description of the table
	 * @return a tuple
	 * @throws SQLException
	 */
	protected CompareableTuple<Object> getRow(ResultSet set,
			Map<String, ColDesc> tableDescription) throws SQLException {
		CompareableTuple<Object> ret = new CompareableTuple<Object>(
				tableDescription.size(), comparisonRules);
		for (ColDesc colDesc : tableDescription.values()) {
			Object o;
//			if (colDesc.fuzzy) {
//				double d = set.getDouble(colDesc.name);
//				double v;
//				if (d != 0)
//					if (Math.abs(d) < 0.0001)
//						v = 0;
//					else
//						v = Double
//								.longBitsToDouble(Double.doubleToLongBits(d) & 0xfffffffffffff000L);
//				else
//					v = 0; // eliminate -0.0/+0.0 diffs
//				o = Double.valueOf(v);
//			} else
				o = set.getObject(colDesc.name);
			ret.add(o);
		}
		return ret;
	}

	/**
	 * Prepare a divergence report like this:
	 * 
	 * <pre>
	 * Key:[KeyValue1,....]
	 * -SomeField=somevalue
	 * +SomeField=someotheralue
	 * </pre>
	 * 
	 * @param reportTo
	 * @param diffDescription
	 * @param set1
	 *            JDBC result set no 1
	 * @param set2
	 *            JDBC result set no 2
	 * @param key1
	 *            common key values
	 * @param pkDescription
	 * 			  primary key description 
	 * @param row1
	 *            full row1
	 * @param row2
	 *            full row2
	 * @param ignored
	 * 			  true if the user has the option to ignore this row 
	 * @throws SQLException
	 */
	public abstract void reportChanged(PrintStream reportTo,
			TableDiffDescription diffDescription, ResultSet set1,
			ResultSet set2, CompareableTuple<?> key1, boolean matchValues, Map<String, ColDesc> pkDescription) throws SQLException;

	/**
	 * Prepare a part of a difference report when we only have data for one
	 * side, i.e no matching keys on the other side.
	 * @param matchValues 
	 * @param key TODO
	 */
	public abstract void reportAddedOrRemoved(PrintStream reportTo,
			ResultSet set, TableDiffDescription diffDescription,
			String addRemove, boolean matchValues, CompareableTuple<?> key) throws SQLException;

	/**
	 * Prepare header information with report metadata.
	 * 
	 * @param reportTo
	 * @param tableDescription1
	 * @param tableDescription2
	 */
	public abstract void reportSchema(PrintStream reportTo, String dbName1,
			String dbName2, Map<String, ColDesc> tableDescription1,
			Map<String, ColDesc> tableDescription2);

	/**
	 * Report progress
	 * 
	 * @param reportTo
	 * @param progress
	 *            information as a free format string
	 */
	public abstract void reportProgress(PrintStream reportTo, String string);
}
