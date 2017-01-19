package com.dewire.dbdiff;

import java.io.PrintStream;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

public class TableDiff {

	static class QueryThread extends Thread {
		private ResultSet resultSet;
		private SQLException exception;
		private RuntimeException runtimeException;
		private Error error;
		private final Statement stmt;
		private final String query;

		QueryThread(Statement stmt, String query) {
			this.stmt = stmt;
			this.query = query;
		}

		@Override
		public void run() {
			try {
				System.out.println("--SQL: " + query);
				resultSet = stmt.executeQuery(query);
			} catch (SQLException e) {
				exception = e;
			} catch (RuntimeException e) {
				runtimeException = e;
			} catch (Error e) {
				error = e;
			}
		}
		public ResultSet getResultSet() throws SQLException {
			try {
				join();
			} catch (InterruptedException e) {
				// FIXME what now?
			}
			if (exception != null)
				throw exception;
			if (error != null)
				throw error;
			if (runtimeException != null)
				throw runtimeException;
			return resultSet;
		}
	}
	private Connection c1;
	private Connection c2;
	private String schema;
	private String table;
	private DatabaseMetaData m1;
	private DatabaseMetaData m2;
	private final String dbName1;
	private final String dbName2;
	private ComparisonRules comparisonRules;
	private final String selectList;
	private final List<String> orderBy;
	private final Map<String, ColDesc> tableColumnDescriptors;
	private final String query1;
	private final String query2;
	private final List<Map<String, List<IgnoreValue>>> ignoreRowCondition;

	public TableDiff(String schema, String table, String query1, String query2, String selectList, List<String> orderBy2, String dbName1, Connection c1, String dbName2, Connection c2, ComparisonRules comparisonRules, Map<String, ColDesc> map, List<Map<String, List<IgnoreValue>>> ignoreRowCondition) throws SQLException {
		this.query1 = query1;
		this.query2 = query2;
		this.orderBy = orderBy2;
		this.selectList = selectList;
		this.dbName1 = dbName1;
		this.c1 = c1;
		this.dbName2 = dbName2;
		this.c2 = c2;
		this.schema = schema;
		this.table = table;
		this.comparisonRules = comparisonRules;
		this.tableColumnDescriptors = map;
		this.ignoreRowCondition = ignoreRowCondition;
		m1 = c1.getMetaData();
		m2 = c2.getMetaData();
	}

	private void constructSelectAll(Reporter reporter, PrintStream reportTo, String schema, String name, Map<String, ColDesc> tableDescription1, Map<String,ColDesc> pkDescription1, Map<String, ColDesc> tableDescription2, Map<String, ColDesc> pkDescription2) throws SQLException {
		String orderByColumnlist1=orderBy != null ? commaquote(orderBy) : makecolumnlist(pkDescription1);
		String selectColumnlist1=selectList != null && !selectList.equals("*") ? selectList : makecolumnlist(tableDescription1);
		String order1 = orderByColumnlist1 != null ? orderByColumnlist1 : selectColumnlist1;
		String sql1 = makeSql(selectColumnlist1, query1, order1);
		Statement statement1 = c1.createStatement();

		String orderByColumnlist2=orderBy != null ? commaquote(orderBy) : makecolumnlist(pkDescription2);
		String selectColumnlist2=selectList != null ? selectList : makecolumnlist(tableDescription2);
		String order2 = orderByColumnlist2 != null ? orderByColumnlist2 : selectColumnlist2;
		String sql2 = makeSql(selectColumnlist2, query2, order2);
		Statement statement2 = c2.createStatement();

		QueryThread queryThread1 = new QueryThread(statement1,sql1);
		queryThread1.start();
		QueryThread queryThread2 = new QueryThread(statement2,sql2);
		queryThread2.start();
		
		ResultSet set1 = queryThread1.getResultSet();
		ResultSet set2 = queryThread2.getResultSet();

		tableDescription1 = getDynamicPrimaryKeyDescription(set1, orderBy, tableDescription1);
		tableDescription2 = tableDescription1;
		
		if (pkDescription1.size()==0) {
			pkDescription1 = new LinkedHashMap<String, ColDesc>();
			for (ColDesc colDesc : tableDescription1.values()) {
				pkDescription1.put(colDesc.name, colDesc);
			}
		}

		Map<String, ColDesc> filteredPkDescription;
		if (orderBy == null) {
			filteredPkDescription = pkDescription1;
		} else {
			filteredPkDescription = new LinkedHashMap<String, ColDesc>();
			for (String k : orderBy) {
				k = k.trim();
				ColDesc colDesc = tableDescription1.get(k);
				if (colDesc == null)
					System.out.println("table has no " + k);
				filteredPkDescription.put(k, colDesc);
			}
		}

		Map<String, ColDesc> filteredTableDescription1;
		Map<String, ColDesc> filteredTableDescription2;
		if (selectList == null) {
			filteredTableDescription1 = tableDescription1;
			filteredTableDescription2 = tableDescription2;
		} else {
			filteredTableDescription1 = new LinkedHashMap<String, ColDesc>();
			filteredTableDescription2 = filteredTableDescription1;
			for (String k : selectList.split(",")) {
				k = k.trim();
				ColDesc colDesc = tableDescription1.get(k);
				if (colDesc == null)
					System.out.println("table has no " + k);
				filteredTableDescription1.put(k, colDesc);
			}
		}
		
		compareByMerge(reporter, reportTo, schema, name, set1,set2,filteredPkDescription,filteredTableDescription1,filteredTableDescription2);

		set1.close();
		set2.close();
	}

	private String commaquote(List<String> list) {
		if (list.size() == 0)
			return "";
		if (list.size() == 1)
			return list.get(0);
		StringBuilder ret = new StringBuilder();
		for (String s : list) {
			if (ret.length() > 0)
				ret.append(",");
			ret.append(SQLKeywordHelper.quoteIfNecessary(s));
		}
		return ret.toString();
	}

	private String makeSql(String selectColumnlist, String query, String order) {
		if (query != null)
			return query + " order by "  + order;
		return "select "+selectColumnlist+" from "+schema + "." + table+" order by " + order;
	}

	private String makecolumnlist(Map<String, ColDesc> pkDescription1) {
		if (pkDescription1.size()==0)
			return null;
		StringBuffer b=new StringBuffer();
		for (ColDesc colDesc : pkDescription1.values()) {
			if (b.length()>0)
				b.append(",");
			b.append(quoteColumn(colDesc.name));
		}
		return b.toString();
	}

	private void compareByMerge(Reporter reporter, PrintStream reportTo, String schema, String name, ResultSet set1, ResultSet set2, Map<String, ColDesc> pkDescription1, Map<String, ColDesc> tableDescription1,Map<String, ColDesc> tableDescription2) throws SQLException {
		boolean end1 = !set1.next();
		CompareableTuple<?> key1 = end1 ? null : getKey(set1,pkDescription1);

		boolean end2 = !set2.next();
		CompareableTuple<Object> key2 = end2 ? null : getKey(set2,pkDescription1);

		TableDiffDescription diffDescription = new TableDiffDescription(schema, name, pkDescription1, tableDescription1, tableDescription2);
		
		reporter.reportSchema(reportTo, dbName1, dbName2, tableDescription1,
				tableDescription2);
		
		while (!end1 || !end2) {
			int c;
			if (key2 == null)
				c = -1;
			else if (key1 == null)
				c = 1;
			else {
				Comparator cmp = mapComparators(comparisonRules, set1, set2, pkDescription1);
				c = cmp.compare(key1, key2);
			}
			if (c==0) {
				boolean matchValues = matchValues(set1, set2, ignoreRowCondition);
				reporter.reportChanged(reportTo,
						diffDescription, set1, set2, key1, matchValues, pkDescription1);

				end1 = !set1.next();
				key1 = end1 ? null : getKey(set1,pkDescription1);

				end2 = !set2.next();
				key2 = end2 ? null : getKey(set2,pkDescription1);
			} else if (c < 0) {
				boolean matchValues = matchValues(set1, null, ignoreRowCondition);
				reporter.reportAddedOrRemoved(reportTo, set1, diffDescription, "-", matchValues, key1);

				end1 = !set1.next();
				key1 = end1 ? null : getKey(set1,pkDescription1);
			} else {
				boolean matchValues = matchValues(null, set2, ignoreRowCondition);
				reporter.reportAddedOrRemoved(reportTo, set2, diffDescription, "+", matchValues, key2);

				end2 = !set2.next();
				key2 = end2 ? null : getKey(set2,pkDescription1);
			}
		}
	}

	private CompareableTuple<Object> getKey(ResultSet set1, Map<String, ColDesc> pkDescription1) throws SQLException {
		CompareableTuple<Object> ret=new CompareableTuple<Object>(pkDescription1.size(), comparisonRules);
		for (ColDesc colDesc : pkDescription1.values()) {
			ret.add(set1.getObject(colDesc.name));
		}
		return ret;
	}

	static Comparator mapComparators(final ComparisonRules rules, final ResultSet set1, final ResultSet set2, final Map<String, ColDesc> pkDescription1) {
		Comparator<CompareableTuple<Object>> ret = new Comparator<CompareableTuple<Object>>() {
			List<Comparator> comparators = new ArrayList<Comparator>();
			{
				for (ColDesc colDesc : pkDescription1.values()) {
					Comparator comparator = getComparator(set1, set2, colDesc, rules);
					comparators.add(comparator);
				}				
			}
			public int compare(CompareableTuple<Object> o1,
					CompareableTuple<Object> o2) {
				assert o1.size() == o2.size();
				for (int i = 0; i < o1.size(); i++) {
					Comparator comparator = comparators.get(i);
					int c = comparator.compare(o1.get(i), o2.get(i));
					if (c != 0)
						return c;
				}
				return 0;
			}
		};
		return ret;
	}
	
	private Map<String, ColDesc> getDynamicPrimaryKeyDescription(ResultSet set, List<String> orderBy, Map<String, ColDesc> tableColumnDescriptors2) throws SQLException {
		ResultSetMetaData metaData = set.getMetaData();
		Map<String, ColDesc> columns=new HashMap<String, ColDesc>();
		for (int i = 1; i <= metaData.getColumnCount(); ++i) {
			String columnLabel = metaData.getColumnLabel(i);
			ColDesc colDesc = tableColumnDescriptors2.get(columnLabel);
			if (colDesc == null)
				colDesc = new ColDesc(columnLabel, null,null);
			columns.put(columnLabel, colDesc);
		}
		Map<String, ColDesc> ret = new LinkedHashMap<String, ColDesc>();
		for (String order : orderBy) {
			ret.put(order, columns.get(order));
		}
		ret.putAll(columns);
		return ret;
	}

	private Map<String, ColDesc> getStaticPrimaryKeyDescription(DatabaseMetaData m, Map<String, ColDesc> tableColumnDescriptors2) throws SQLException {
		String schemaName = table.substring(0, table.indexOf('.'));
		String tableName = table.substring(table.indexOf('.') + 1);
		Map<String, ColDesc> columns=new LinkedHashMap<String, ColDesc>();
		ResultSet t1 = m.getPrimaryKeys("",schemaName,tableName);
		while (t1.next()) {
			String column=t1.getString("COLUMN_NAME");
			ColDesc colDesc = tableColumnDescriptors2.get(column);
			if (colDesc == null)
				colDesc = new ColDesc(column, null,null);
			columns.put(column, colDesc);
		}
		t1.close();
		ResultSet t2 = m.getColumns("",escapeLike(schemaName),escapeLike(tableName),"%");
		while (t2.next()) {
			String column=t2.getString("COLUMN_NAME");
//			if (ignorableKey(t2))
//				columns.remove(column);
		}
		t2.close();
		return columns;
	}

	private Map<String, ColDesc> getStaticTableDescription(DatabaseMetaData m,Map<String, ColDesc> pkDescription1, Map<String, ColDesc> tableColumnDescriptors2) throws SQLException {
		Map<String, ColDesc> columns=new LinkedHashMap<String, ColDesc>();
		ResultSet t1 = m.getColumns(null,"dbo","BATCHPROGRAM","%"); // TODO: ?????
		while (t1.next()) {
			String columnName = t1.getString("COLUMN_NAME");
			ColDesc colDesc = tableColumnDescriptors2.get(columnName);
			if (colDesc == null) {
				ConditionalComparator conditionalComparator = new ConditionalComparator();
				conditionalComparator.add(null, isFuzzy(t1));
				colDesc = new ColDesc(columnName, conditionalComparator, null);
			}
			columns.put(columnName, colDesc);
		}
		t1.close();
		return columns;
	}

	static class ConditionalComparator {	
		
		List<Map<String, List<IgnoreValue>>> conditions = new ArrayList<Map<String,List<IgnoreValue>>>();
		List<Comparator<?>> comparators = new ArrayList<Comparator<?>>();
		void add(Map<String, List<IgnoreValue>> condition, Comparator<?> comparator) {
			conditions.add(condition);
			comparators.add(comparator);
		}
	}
	
//	private boolean ignorableData(ResultSet t1) throws SQLException {
//		if (DbDiff.ignoreTime) {
//			short type=t1.getShort("DATA_TYPE");
//			switch (type) {
//			case Types.TIMESTAMP:
//			case Types.TIME:
//			case Types.DATE:
//				return true;
//			}
//			return false;
//		} else {
//			return false;
//		}
//	}
//
//	private boolean ignorableKey(ResultSet t1) throws SQLException {
//		if (DbDiff.ignoreTime) {
//			short type=t1.getShort("DATA_TYPE");
//			switch (type) {
//			case Types.TIMESTAMP:
//			case Types.TIME:
//				return true;
//			}
//			return false;
//		} else {
//			return false;
//		}
//	}
//
	private Comparator<?> isFuzzy(ResultSet t1) throws SQLException {
		if (DbDiff.fuzzy) {
			short type=t1.getShort("DATA_TYPE");
			switch (type) {
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.REAL:
//			case Types.NUMERIC:
				return new SmudgedDoubleComparator(40);
			}
			return null;
		} else {
			return null;
		}
	}

	public void compare(Reporter reporter, PrintStream reportTo) throws SQLException {
		reporter.reportProgress(reportTo, "Comparisons of "+schema + "." + table);

		Map<String,ColDesc> pkDescription1 = tableColumnDescriptors; // getPrimaryKeyDescription(m1, tableColumnDescriptors);
		Map<String,ColDesc> pkDescription2 = tableColumnDescriptors; // getPrimaryKeyDescription(m2, tableColumnDescriptors);
		Map<String, ColDesc> tableDescription1;
		try {
			tableDescription1 = getStaticTableDescription(m1, pkDescription1, tableColumnDescriptors);
		} catch (SQLException e) {
			throw new SQLException("In first database connection", e);
		}
		Map<String, ColDesc> tableDescription2;
		try {
			tableDescription2 = getStaticTableDescription(m2, pkDescription2, tableColumnDescriptors);
		} catch (SQLException e) {
			throw new SQLException("In second database connection", e);
		}
		
		// TODO: handle pkdescription diffs if all columns are present
		if (!tableDescription1.equals(tableDescription2)) {
			reportTo.println("Difference detected in schema");
			for (String columnName : tableDescription1.keySet()) {
				if (!tableDescription2.containsKey(columnName)) {
					reportTo.println("-"+columnName);
				}
			}
			for (String columnName : tableDescription2.keySet()) {
				if (!tableDescription1.containsKey(columnName)) {
					reportTo.println("+"+columnName);
				}
			}
		}
		
		constructSelectAll(reporter, reportTo, schema, table, tableColumnDescriptors, pkDescription1, tableColumnDescriptors, pkDescription2);
	}

	private String escapeLike(String pattern) throws SQLException {
		String esc = m1.getSearchStringEscape();
		if (esc.length()!=1)
			throw new Error("Unexpected escape string length for "+esc);
		StringBuffer b=new StringBuffer();
		for (int i=0; i<pattern.length(); ++i) {
			char c=pattern.charAt(i);
			if (c=='_') {
				b.append(esc);
				b.append(c);
			} else if (c=='%') {
				b.append(esc);
				b.append(c);
			} else if (c==esc.charAt(0)) {
				b.append(esc);
				b.append(esc);
			} else
				b.append(c);
		}
		return b.toString();
	}

	private Object quoteColumn(String string) {
		return '"' + string + '"';
	}

	public String getTableName() {
		return table;
	}

	/**
	 * @return true if data matches a condition
	 */
	static boolean matchValues(ResultSet set1, ResultSet set2, List<Map<String, List<IgnoreValue>>> ignoreIfKey) {
		NEXTIGNORE:
		for (Map<String, List<IgnoreValue>> object : ignoreIfKey) {
			NEXTPOSSIBLEKEYVALUE:
			for (Entry<String, List<IgnoreValue>> entry : object.entrySet()) {
				try {
					String actualOldValue = set1!=null ? set1.getString(entry.getKey()) : null;
					String actualNewValue = set2!=null ? set2.getString(entry.getKey()) : null;
					for (IgnoreValue ignoreValue : entry.getValue()) {
						if (ignoreValue.equalsValues(actualOldValue, actualNewValue))
							continue NEXTPOSSIBLEKEYVALUE; // a value found, try next key part
					}
					continue NEXTIGNORE; // no matching value found
				} catch (SQLException e) {
					throw new RuntimeException("Failed to get value for column " + entry.getKey());
				}
			}
			return true;
		}
		return false;
	}

	static Comparator getComparator(ResultSet set1, ResultSet set2, ColDesc colDesc, ComparisonRules rules) {
		if (colDesc.getComparator() != null) {
			for (int j = 0; j < colDesc.getComparator().comparators.size(); ++j) {
				Comparator comparator = colDesc.getComparator().comparators.get(j);
				Map<String, List<IgnoreValue>> conditions = colDesc.getComparator().conditions.get(j);
				if (conditions == null) {
					return comparator;
				}
				if (TableDiff.matchValues(set1, set2, Collections.singletonList(conditions))) {
					return comparator;
				}
			}
		}
		Object obj;
		try {
			obj = set1.getObject(colDesc.name);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		if (obj == null)
			return null;			
		Comparator comparable = rules.getTypeComparator(obj.getClass());
		return comparable;
	}

}
