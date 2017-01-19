package com.dewire.dbdiff;

import java.util.LinkedHashMap;
import java.util.Map;

/** Metadata for table compare */
class TableDiffDescription {

	private Map<String, ColDesc> onlyintable1;
	private Map<String, ColDesc> onlyintable2;
	private Map<String, ColDesc> commonDescription;
	private Map<String, ColDesc> totalDescription;
	private final String schema;
	private final String name;

	public Map<String, ColDesc> getOnlyintable1() {
		return onlyintable1;
	}

	public Map<String, ColDesc> getOnlyintable2() {
		return onlyintable2;
	}

	public Map<String, ColDesc> getCommonDescription() {
		return commonDescription;
	}

	public Map<String, ColDesc> getTotalDescription() {
		return totalDescription;
	}

	public TableDiffDescription(String schema, String name,Map<String, ColDesc> pkDescription1,
			Map<String, ColDesc> tableDescription1,
			Map<String, ColDesc> tableDescription2
			) {
		this.schema = schema;
		this.name = name;
		onlyintable1 = new LinkedHashMap<String, ColDesc>(tableDescription1);
		onlyintable1.keySet().removeAll(tableDescription2.keySet());

		onlyintable2 = new LinkedHashMap<String, ColDesc>(tableDescription2);
		onlyintable2.keySet().removeAll(tableDescription1.keySet());

		commonDescription = new LinkedHashMap<String, ColDesc>(tableDescription1);
		commonDescription.keySet().removeAll(onlyintable1.keySet());

		totalDescription = new LinkedHashMap<String, ColDesc>(tableDescription1);
		totalDescription.putAll(tableDescription2);
	}

	public String getName() {
		return name;
	}

	public String getSchema() {
		return schema;
	}
}