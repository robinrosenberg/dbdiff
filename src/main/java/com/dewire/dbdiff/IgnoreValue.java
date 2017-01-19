package com.dewire.dbdiff;

public class IgnoreValue {
	private final String oldValue;
	private final String newValue;

	public IgnoreValue(String oldValue,String newValue) {
		this.oldValue = oldValue;
		this.newValue = newValue;
		assert oldValue != null;
		assert newValue != null;
	}

	public IgnoreValue(String value) {
		this.oldValue = value;
		this.newValue = value;
		assert value != null;
	}

	public String getNewValue() {
		return newValue;
	}

	public String getOldValue() {
		return oldValue;
	}

	boolean equalsValues(String oldValueToCompare, String newValueToCompare) {
		if (oldValue != null)
			if (!oldValue.equals(oldValueToCompare))
				return false;
		if (newValue != null)
			if (!newValue.equals(newValueToCompare))
				return false;
		return true;
	}
}
