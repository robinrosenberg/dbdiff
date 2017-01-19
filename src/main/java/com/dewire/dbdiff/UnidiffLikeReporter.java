package com.dewire.dbdiff;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;

public class UnidiffLikeReporter extends Reporter {

	public UnidiffLikeReporter(ComparisonRules comparisonRules) {
		super(comparisonRules);
	}

	/**
	 * Prepare a divergence report like this
	 * <pre>
	 * -[ROWX.COL1,....]
	 * </pre>
	 * or
	 * <pre>
	 * +[ROWY.COL1,....
	 * </pre>
	 * @param reportTo
	 * @param diffDescription 
	 * @param addRemove
	 * @param set2 
	 * @param row1
	 * @throws SQLException 
	 */
	@Override
	public void reportAddedOrRemoved(PrintStream reportTo, ResultSet set, TableDiffDescription diffDescription,
			String addRemove, boolean ignored, CompareableTuple<?> key) throws SQLException {
		if (!ignored) {
			CompareableTuple<Object> row = getRow(set,diffDescription.getCommonDescription());
			reportTo.println(addRemove + row);
		} else {
			reportTo.println("Key:"+key);
		}
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
	 * @param row1
	 *            full row1
	 * @param row2
	 *            full row2
	 * @param ignored
	 * 			  true if the user has the option to ignore this row 
	 * @throws SQLException
	 */
	@Override
	public void reportChanged(PrintStream reportTo,
			TableDiffDescription diffDescription, ResultSet set1,
			ResultSet set2, CompareableTuple<?> key1, boolean ignored, Map<String, ColDesc> pkDescription)
			throws SQLException {
		CompareableTuple<Object> row1 = getRow(set1, diffDescription.getCommonDescription());
		CompareableTuple<Object> row2 = getRow(set2, diffDescription.getCommonDescription());
		if (row1.equals(row2)) {
			assert false;
			return;
		}
		Object[] columns =  diffDescription.getCommonDescription().keySet().toArray();
		Object[] objects1 = row1.toArray();
		Object[] objects2 = row2.toArray();
		if (!ignored) {
			reportTo.println("Key:"+key1);
			NEXTVALUE:
			for (int i = 0; i < objects2.length; i++) {
				Object o1 = objects1[i];
				Object o2 = objects2[i];
				if (o1 == o2)
					continue;
				if (o1!=null && o1.equals(o2))
					continue;
				ColDesc colDesc = diffDescription.getCommonDescription().get(columns[i]);
				if (colDesc != null && colDesc.getComparator() != null) {
					Comparator comparator = TableDiff.getComparator(set1, set2, colDesc, comparisonRules);
					if (comparator.compare(o1, o2) == 0)
						continue NEXTVALUE;
				}
				if (colDesc.getIgnoreIfKey() != null) {
					if (TableDiff.matchValues(set1, set2, colDesc.getIgnoreIfKey()))
						continue NEXTVALUE;
				}
				reportTo.println("-" + columns[i] + "="  + objects1[i]);
				reportTo.println("+" + columns[i] + "="  + objects2[i]);
			}
		}
		Object[] onlycolumns1 = diffDescription.getOnlyintable1().keySet().toArray();
		Object[] onlyobjects1 = getRow(set1, diffDescription.getOnlyintable1()).toArray();
		for (int i=0; i < onlyobjects1.length; i++) {
			if (onlyobjects1[i] != null)
				reportTo.println("-[" + onlycolumns1[i] + "=" + onlyobjects1[i] + "]"); 
		}
		Object[] onlycolumns2 = diffDescription.getOnlyintable2().keySet().toArray();
		Object[] onlyobjects2 = getRow(set2,diffDescription.getOnlyintable2()).toArray();
		for (int i=0; i < onlyobjects2.length; i++) {
			if (onlyobjects2[i] != null)
				reportTo.println("+[" + onlycolumns2[i] + "=" + onlyobjects2[i] + "]"); 
		}
		reportTo.println();
	}

	@Override
	public void reportSchema(PrintStream reportTo, String dbName1,
			String dbName2, Map<String, ColDesc> tableDescription1,
			Map<String, ColDesc> tableDescription2) {
		reportTo.println("Schema "+dbName1+":-:"+tableDescription1);
		reportTo.println("Schema "+dbName2+":+:"+tableDescription2);
	}

	@Override
	public void reportProgress(PrintStream reportTo, String string) {
		reportTo.println(string);
	}
}
