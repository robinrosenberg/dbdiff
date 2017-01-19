package com.dewire.dbdiff;

import com.dewire.dbdiff.schema.*;
import com.dewire.dbdiff.schema.Table.Column;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

/*
 * Created on 2006-apr-18
 *
 */

public class DbDiff {

	static class ConnectorThread extends Thread {
		private final String url;
		private final String user;
		private final String pass;
		private Connection connection;
		private SQLException exception;
		private RuntimeException runtimeException;
		private Error error;

		ConnectorThread(String url, String user, String pass) {
			this.url = url;
			this.user = user;
			this.pass = pass;
		}
		@Override
		public void run() {
			try {
				connection = DriverManager.getConnection(url,user,pass);
			} catch (SQLException e) {
				exception = e;
			} catch (RuntimeException e) {
				runtimeException = e;
			} catch (Error e) {
				error = e;
			}
		}
		public Connection getConnection() throws SQLException {
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
			return connection;
		}
	}

	static private ComparisonRules comparisonRules = new ComparisonRules();

	@Option(usage="jdbc_url#1", name = "--url1")
	String url1;

	@Option(usage="username#1", name = "--user1")
	String user1;

	@Option(usage="passwrd#1", name = "--pass1")
	String pass1;

	@Option(usage="jdbc_url#1", name = "--url2")
	String url2;

	@Option(usage="username#2", name = "--user2")
	String user2;

	@Option(usage="passwrd#2", name = "--pass2")
	String pass2;

	@Option(usage="[--driver driverclass]...", multiValued=true, name="--driver")
	List<String> drivers;

	@Option(usage="[--schema schema]", multiValued=true, name="--schema")
	String schema;

	@Option(usage="[--table table]...", multiValued=true, name="--table")
	List<String> tables;

	@Option(usage="[--out filename]", name="--out")
	String outputFile;

	@Option(usage="[--config dbdiffconfig]", name="--config")
	String configFile;

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws JAXBException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JAXBException {
		DbDiff dbDiff = new DbDiff();
		CmdLineParser cmdLineParser = new CmdLineParser(dbDiff);
		try {
			cmdLineParser.parseArgument(args);
			dbDiff.run();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.print("Usage:");
			cmdLineParser.printUsage(System.err);
		}
	}
	
	void run() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, JAXBException {
		Comparison comparison;
		Map<String, Map<String, ColDesc>> tableColumnDescriptors = new LinkedHashMap<String, Map<String, ColDesc>>();
		Map<String, TableDiff> tableDiffs = new LinkedHashMap<String, TableDiff>();
		PrintStream reportTo;
		if (outputFile != null)
			reportTo = new PrintStream(new FileOutputStream(outputFile));
		else
			reportTo = System.out;
		FormatStyle style = FormatStyle.UNIDIFF_LIKE;
		if (configFile != null) {
			JAXBContext context = JAXBContext.newInstance("com.dewire.dbdiff.schema");
			Unmarshaller unmarshaller = context.createUnmarshaller();
			comparison = (Comparison) unmarshaller
					.unmarshal(new File(configFile));
			drivers = new ArrayList<String>();
			for (com.dewire.dbdiff.schema.Comparison.Driver driver : comparison.getDriver()) {
				drivers.add(driver.getClassName());
			}

			for (String driver : drivers) {
				DriverManager.registerDriver((Driver)Class.forName(driver).newInstance());
			}

			List<Datasource> datasources = comparison.getDatasource();
			Datasource datasource1 = datasources.get(0);
			Datasource datasource2 = datasources.get(1);
			url1 = datasource1.getUrl();
			url2 = datasource2.getUrl();
			user1 = datasource1.getUser();
			user2 = datasource2.getUser();
			pass1 = datasource1.getPassword();
			pass2 = datasource2.getPassword();
			if (user2 == null) {
				user2 = user1;
				pass2 = pass1;
			}
			ConnectorThread connectorThread1 = new ConnectorThread(url1,user1,pass1);
			connectorThread1.start();
			ConnectorThread connectorThread2 = new ConnectorThread(url2,user2,pass2);
			connectorThread2.start();

			Connection c1 = connectorThread1.getConnection();
			Connection c2 = connectorThread2.getConnection();

			style = comparison.getFormat().getStyle();
			tables = new ArrayList<String>();

			for (Schema schema : comparison.getSchema()) {
				for (Table table : schema.getTable()) {
					Map<String, ColDesc> columnDescriptors = new LinkedHashMap<String, ColDesc>();
					for (Column value : table.getColumn()) {
						ColDesc colDesc = mapColumnRule(value);
						if (colDesc != null)
							columnDescriptors.put(value.getName(), colDesc);
					}
					String fullTableName = schema.getName() + "." + table.getName();
					tableColumnDescriptors.put(fullTableName, columnDescriptors);
					String selectList = table.getSelect().size() > 0 ? table.getSelect().get(0) : null;
					String query1 = table.getQuery().get(0);
					String query2;
					if (table.getQuery().size() == 2)
						query2 = table.getQuery().get(1);
					else
						query2 = query1;
					List<String> orderBy = new ArrayList<String>();
					for (int i = 0; i< table.getKey().size(); ++i) {
						orderBy.add(table.getKey().get(i).getName());
					}
					List<Map<String, List<IgnoreValue>>> ignoreRowCondition = parseIgnore(table.getIgnore());
					TableDiff diff = new TableDiff(schema.getName(), table.getName(), query1, query2, selectList, orderBy, url1, c1, url2, c2, comparisonRules, tableColumnDescriptors.get(fullTableName), ignoreRowCondition);
					tableDiffs.put(fullTableName, diff);
				}
			}
			style = comparison.getFormat().getStyle();
		} else {
			for (String driver : drivers) {
				DriverManager.registerDriver((Driver)Class.forName(driver).newInstance());
			}
			if (user2 == null) {
				user2 = user1;
				pass2 = pass1;
			}
			ConnectorThread connectorThread1 = new ConnectorThread(url1,user1,pass1);
			connectorThread1.start();
			ConnectorThread connectorThread2 = new ConnectorThread(url2,user2,pass2);
			connectorThread2.start();

			Connection c1 = connectorThread1.getConnection();
			Connection c2 = connectorThread2.getConnection();

			for (String table : tables) {
				String[] parts = table.split(":");
				String tableName = parts[0];
				String selectList = null;
				List<String> orderBy = null;
				for (int i=1; i<parts.length; ++i) {
					String[] option=parts[i].split("=");
					if (option[0].equals("select")) {
						selectList = option[1];
					}
					if (option[0].equals("order")) {
						orderBy = Arrays.asList(option[1].split(","));
					}
				}
				Map<String, ColDesc> columnDescriptors = new LinkedHashMap<String, ColDesc>();
				tableColumnDescriptors.put(tableName, columnDescriptors);
				TableDiff diff = new TableDiff(schema, tableName, "select * from " + tableName, "select * from " + tableName, null, orderBy, url1, c1, url2, c2, comparisonRules, tableColumnDescriptors.get(tableName), null); 
				tableDiffs.put(tableName, diff);
			}
		}
		Reporter reporter;
		switch (style) {
		case UNIDIFF_LIKE:
			reporter = new UnidiffLikeReporter(comparisonRules);
			break;
		case SQL:
			reporter = new SQLDiffReporter(comparisonRules);
			break;
		case TABULAR_OVER_UNDER:
		default:
			throw new IllegalStateException("Not yet supported");
		}

		reporter.reportProgress(reportTo,"Comparing "+url1+" with "+url2);

		for (TableDiff diff : tableDiffs.values()) {
			try {
				diff.compare(reporter, reportTo);
			} catch (SQLException exc) {
				System.err.println("Problem with " + diff.getTableName());
				exc.printStackTrace();
				exc.printStackTrace(reportTo);
			}
		}
		reportTo.close();
	}

	private ColDesc mapColumnRule(Column value) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		TableDiff.ConditionalComparator comparators = new TableDiff.ConditionalComparator();
		for (com.dewire.dbdiff.schema.Table.Column.Comparator cmp : value.getComparator()) {
			Comparator<?> comparator = null;
			String className = cmp.getClassName();
			Class<?> forName = Class.forName(className);
			if (cmp.getArgument() != null) {
				String argClassName = cmp.getArgument().getClassName();
				Class<?> forArgName = getWrapperForName(argClassName); 
				if (forArgName == null)
					forArgName = Class.forName(argClassName);
				Constructor<?> constructor = null;
				Object comparatorArg = null;
				try {
					constructor = forArgName.getConstructor(String.class);
					comparatorArg = constructor.newInstance(cmp.getArgument().getValue());
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				} catch (NoSuchMethodException e) {
					// ok, go on
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				}
				if (constructor == null) {
					try {
						Method method = forArgName.getMethod("valueOf", String.class);
						comparatorArg = method.invoke(null, cmp.getArgument().getValue());
					} catch (SecurityException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					}
				}
				try {
					Constructor<?> constructor2 = forName.getConstructor(forArgName);
					comparator = (Comparator<?>) constructor2.newInstance(comparatorArg);
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			} else {
				try {
					Constructor<?> constructor2 = forName.getConstructor();
					comparator = (Comparator<?>) constructor2.newInstance();
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
			Map<String, List<IgnoreValue>> useCmpFor = parseIgnoreKeys(cmp.getIgnorekey());
			comparators.add(useCmpFor, comparator);
		}
		
		List<Map<String, List<IgnoreValue>>> ignoreList = parseIgnore(value.getIgnore());
		ColDesc colDesc = new ColDesc(value.getName(), comparators, ignoreList);
		return colDesc;
	}

	private List<Map<String, List<IgnoreValue>>> parseIgnore(
			List<Ignore> ignoreConditionList) {
		List<Map<String, List<IgnoreValue>>> ignoreList = new ArrayList<Map<String, List<IgnoreValue>>>();
		for (Ignore ignore : ignoreConditionList) {
			Map<String, List<IgnoreValue>> ignoreMap = parseIgnoreKeys(ignore
					.getIgnorekey());
			ignoreList.add(ignoreMap);
		}
		return ignoreList;
	}

	/**
	 * @return a map from column name to a set of values to match
	 */
	private Map<String, List<IgnoreValue>> parseIgnoreKeys(
			List<Ignorekey> ignorekeys) {
		if (ignorekeys.size() == 0)
			return null;
		Map<String, List<IgnoreValue>> ignoreMap = new HashMap<String, List<IgnoreValue>>();
		for (Ignorekey ignoreKey : ignorekeys) {
			String iname = ignoreKey.getName();
			List<IgnoreValue> ivalues = new ArrayList<IgnoreValue>();
			for (Value ivalue : ignoreKey.getValue()) {
				IgnoreValue iv;
				if (!ivalue.getContent().equals("")) {
					iv = new IgnoreValue(ivalue.getContent());
				} else {
					iv = new IgnoreValue(ivalue.getOld(), ivalue.getNew());
				}
				ivalues.add(iv);
			}
			ignoreMap.put(iname,ivalues);
		}
		return ignoreMap;
	}

	private Class<?> getWrapperForName(String class1) {
		if (class1.equals("int"))
			return Integer.class;
		if (class1.equals("long"))
			return Long.class;
		if (class1.equals("short"))
			return Short.class;
		if (class1.equals("byte"))
			return Byte.class;
		if (class1.equals("char"))
			return Character.class;
		if (class1.equals("float"))
			return Float.class;
		if (class1.equals("double"))
			return Double.class;
		if (class1.equals("boolean"))
			return Boolean.class;
		return null;
	}

	private Class<?> getPrimitiveClass(String class1) {
		if (class1.equals("int"))
			return int.class;
		if (class1.equals("long"))
			return long.class;
		if (class1.equals("short"))
			return short.class;
		if (class1.equals("byte"))
			return byte.class;
		if (class1.equals("char"))
			return char.class;
		if (class1.equals("float"))
			return float.class;
		if (class1.equals("double"))
			return double.class;
		if (class1.equals("boolean"))
			return boolean.class;
		return null;
	}

	static Set<String> getTables(DatabaseMetaData m) throws SQLException {
		ResultSet r= m.getTables("%","%","%", null);
		Set<String> ret=new TreeSet<String>();
		while (r.next()) {
			String schema = r.getString("TABLE_SCHEM");
			String table = r.getString("TABLE_NAME");

			if (!r.getString("TABLE_TYPE").equals("TABLE"))
				continue;

			if (schema.toUpperCase().startsWith("SYS"))
				continue;
			ret.add(schema.trim() + "." + table.trim());
		}
		return ret;
	}
	
	private static void report(PrintStream reportTo, String string) {
		reportTo.println(string);
	}

	static boolean ignoreTime=true;
	static boolean fuzzy=true;
}
