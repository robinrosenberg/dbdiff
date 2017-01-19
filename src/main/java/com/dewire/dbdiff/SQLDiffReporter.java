package com.dewire.dbdiff;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;

public class SQLDiffReporter extends Reporter {

	public SQLDiffReporter(ComparisonRules comparisonRules) {
		super(comparisonRules);
	}

	/**
	 * Prepare a divergence report like this
	 * 
	 * <pre>
	 * DELETE FROM ...
	 * </pre>
	 * 
	 * or
	 * 
	 * <pre>
	 * INSERT INTO...
	 * </pre>
	 * 
	 * @param reportTo
	 * @param diffDescription
	 * @param addRemove
	 * @param set2
	 * @param row1
	 * @throws SQLException
	 */
	@Override
	public void reportAddedOrRemoved(PrintStream reportTo, ResultSet set,
			TableDiffDescription diffDescription, String addRemove,
			boolean ignored, CompareableTuple<?> key) throws SQLException {
		if (!ignored) {
			CompareableTuple<Object> row = getRow(set, diffDescription
					.getCommonDescription());
			if (addRemove.equals("+")) {
				StringBuilder names = new StringBuilder();
				StringBuilder values = new StringBuilder();
				int i = 0;
				for (Map.Entry<String, ColDesc> e : diffDescription
						.getCommonDescription().entrySet()) {
					if (i > 0) {
						names.append(", ");
						values.append(", ");
					}
					names.append(SQLKeywordHelper.quoteIfNecessary(e.getKey()));
					Object value = row.get(i);
					String qvalue = quoteNonNumeric(value);
					values.append(qvalue);
					++i;
				}
				reportTo.format("INSERT INTO %s.%s (%s) VALUES (%s);\n",
						diffDescription.getSchema(), SQLKeywordHelper.quoteIfNecessary(diffDescription.getName()) , names, values);
			} else {
				StringBuilder conditions = new StringBuilder();
				int i = 0;
				for (Map.Entry<String, ColDesc> e : diffDescription
						.getCommonDescription().entrySet()) {
					if (i > 0) {
						conditions.append(" AND ");
					}
					String name = e.getKey();
					Object value = row.get(i);
					conditions.append(SQLKeywordHelper.quoteIfNecessary(name));
					if (value == null) {
						conditions.append(" is NULL");
					} else {
						conditions.append(" = ");
						conditions.append(quoteNonNumeric(value));
					}
					++i;
				}
				reportTo.format("DELETE from %s WHERE %s;\n", SQLKeywordHelper.quoteIfNecessary(diffDescription
						.getName()), conditions);
			}
		} else {
			// Nothing here
		}
	}

	private String quoteNonNumeric(Object value) {
		String qvalue;
		if (value instanceof Number) {
			qvalue = value.toString();
		} else {
			if (value != null) {
				String stringValue = value.toString();
				stringValue = stringValue.replaceAll("'", "''");
				stringValue = "'" + stringValue + "'";
				qvalue = stringValue;
			} else
				qvalue = "NULL";
		}
		return qvalue;
	}

	@Override
	public void reportChanged(PrintStream reportTo,
			TableDiffDescription diffDescription, ResultSet set1,
			ResultSet set2, CompareableTuple<?> key1, boolean ignored, Map<String, ColDesc> pkDescription)
			throws SQLException {
		CompareableTuple<Object> row1 = getRow(set1, diffDescription
				.getCommonDescription());
		CompareableTuple<Object> row2 = getRow(set2, diffDescription
				.getCommonDescription());
		if (row1.equals(row2)) {
			assert false;
			return;
		}
		reportTo.print("UPDATE " + SQLKeywordHelper.quoteIfNecessary(diffDescription.getName()) + " SET ");
		Object[] columns = diffDescription.getCommonDescription().keySet()
				.toArray();
		Object[] objects1 = row1.toArray();
		Object[] objects2 = row2.toArray();
		boolean firstSet = true;
		if (!ignored) {
			NEXTVALUE: for (int i = 0; i < objects2.length; i++) {
				Object o1 = objects1[i];
				Object o2 = objects2[i];
				if (o1 == o2)
					continue;
				if (o1 != null && o1.equals(o2))
					continue;
				ColDesc colDesc = diffDescription.getCommonDescription().get(
						columns[i]);
				if (colDesc != null && colDesc.getComparator() != null) {
					Comparator comparator = TableDiff.getComparator(set1, set2,
							colDesc, comparisonRules);
					if (comparator.compare(o1, o2) == 0)
						continue NEXTVALUE;
				}
				if (colDesc.getIgnoreIfKey() != null) {
					if (TableDiff.matchValues(set1, set2, colDesc
							.getIgnoreIfKey()))
						continue NEXTVALUE;
				}
				if (!firstSet) {
					reportTo.print(", ");
				}
				firstSet = false;
				reportTo.print(SQLKeywordHelper.quoteIfNecessary((String)columns[i]) + " = "  + quoteNonNumeric(objects2[i]));
			}
		}
		Object[] onlycolumns2 = diffDescription.getOnlyintable2().keySet()
				.toArray();
		Object[] onlyobjects2 = getRow(set2, diffDescription.getOnlyintable2())
				.toArray();
		for (int i = 0; i < onlyobjects2.length; i++) {
			if (onlyobjects2[i] != null) {
				if (!firstSet) {
					reportTo.print(", ");
					firstSet = false;
				}
				reportTo.print("SET " + onlycolumns2[i] + " = "  + quoteNonNumeric(onlyobjects2[i]));
			}
		}
		reportTo.print(" WHERE ");
		CompareableTuple<Object> key = getRow(set1, pkDescription);
		boolean firstCondition = true;
		for (Map.Entry<String, ColDesc> e : pkDescription.entrySet()) {
			Object value = set1.getObject(e.getKey());
			if (!firstCondition)
				reportTo.append(" AND ");
			firstCondition = false;
			reportTo.print(e.getKey());
			reportTo.print(" = ");
			reportTo.print(quoteNonNumeric(value));
		}
		reportTo.println(";");
	}

	@Override
	public void reportSchema(PrintStream reportTo, String dbName1,
			String dbName2, Map<String, ColDesc> tableDescription1,
			Map<String, ColDesc> tableDescription2) {
		reportTo.println("-- Schema " + dbName1 + ":-:" + tableDescription1);
		reportTo.println("-- Schema " + dbName2 + ":+:" + tableDescription2);
	}

	@Override
	public void reportProgress(PrintStream reportTo, String string) {
		reportTo.println("-- " + string);
	}
}
