# DBDiff

A database comparison utility

DBDiff is a somple tool to compare data sets. Specifically it was created
to compare data stored in wildly different formats, not necessarily on
a table by table format.

DBDdiff compares the result of two SQL queries according to a set of "keys"
defined by the user. Rows from the data sets with the same keys are examined
to find which columns in the rows are different. If no match is found by keys then
rows are considered added or removed.

By default columns are compared using the default methods for the datatype, but
that can be ovverridden using special functions to allow for some small changes.

Builtin fuzziness operations exists for integers, doubles, dates and business dates. The
current operator are quite arbitrary from existing use cases.

- DeltaBigDecimalComparator
- DeltaDateComparator
- DeltaIntegerComparator
  Consider equal if values are with the specified absolute delta, e.g delta=5 means
  1 and 5 are equal (diff=4), but 1 and 10 are not (diff=9)

- RoundingBigDecimalComparator
- RoundingDoubleComparator
  Consider equal if values are within the specified relative delra, e.g. 0.01 for one percent

- RightTrimmingStringComparator
  Trims trailing spaces before comparing values
  
- SmudgedDoubleComparator
  Compares values after rounding to the specified number of significant digits

- SimplisticBusinessDayComparator
  Compares dates by converting to the next or previous business day before comparison.
  This comparison consider saturday and sunday, and only those days, as non-business days.  

- StaticBusinessDayComparator
  Compares dates by converting to the next or previous business day according to a specified currency
  before comparison.
  This comparison is based on s static table of business days that may or
  may not be up-to-date. The file `holidays.txt` must be updated with
  holiday dates. Besides those dates, saturdays and sundays are considered
  non-business dates

# Configuration

There is a `comparison.xsd` that describes the schema for the configuration file. The schema
can be used with and XML editor to assist in writing comparison specifications.

The general format is like this:

```
<comparison>
  <datasource – A JDBC data source
  <datasource – An additional datasource for comparing data from another database
  <schema>
     <table name="a name"> – The "table" name to report, doesn't have to be an actual table name
        <query>SELECT ... FROM </query>  An SQL query without ORDER BY or LIMIT
        <query>SQL query 2</query>  The SQL query to compare with.
        <key name="a key column name"/> One or more key columns
        <column name="somecolum"> optional rules for columns
           <ignore> – Specification to ignore differences if some other column contains certain values
              <ignorekey>
              <name>keyname</name>
              <value>value</value>
              </ignorekey>
           </ignore>
           <comparator className="com.dewire.dbdiff.DeltaIntegerComparator">
              <argument className="int" value="3"/>
           </comparator>
        </column>
    </table>
  </schema>
</comparison>
```

The SQL queries can be quite different, but they must return the data in the same format
and with the same column names. This may require you to use CAST or other functions to convert
the type and to use AS to specify the name of the returned column

# Peformance considerations

All data from the two queries will be read from the data sources, even if the rows are identical
and data is sorted by the key specifications on the server side. The two queries are run in
parallel.

# Output formats

`UnififfLike` is inspired by the unified diff format used by standard unix tools. If keys match a
intra-row diff is presented, but otherwise added/removed rows are presented in full.

`Sql` generates SQL code that would transform the first database into the second, if applied on
a real table

# Copyright

See the LICENSE file. 

# Contributing

Create pull requests. Make sure you have the right to contribute the
work according to the license that applies to this work.

# Finally

Have fun :).
