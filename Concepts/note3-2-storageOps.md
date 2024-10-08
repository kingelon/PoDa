In our system, we always maintain two tables:

Lookup Table: Provides a mapping between GUID and cust_xref_id for efficient cross-referencing.
Row Key: GUID
Column: cust_xref_id
The focus of our design is on the main events table, which stores customer and event data, and can be structured in various ways depending on retrieval requirements. Below, we explore four possible designs for the main events table, along with examples and the rationale for choosing the optimal option.

Option 1: Composite Row Key (cust_xref_id + GUID), Reverse Timestamp Columns
In this design, the row key is a composite of cust_xref_id and GUID, and events are stored in columns with a reverse timestamp.

Row Key: cust_xref_id + GUID
Columns: Each event is stored with a reverse timestamp as the column qualifier, which helps to fetch the most recent events for each GUID.
Example:
Row Key (cust_xref_id + GUID)	Columns
cust_123 + guid_abc	rev_ts_1: {Event Data 1}
rev_ts_2: {Event Data 2}
cust_123 + guid_xyz	rev_ts_1: {Event Data 1}
Pros:
Efficient retrieval of recent events for each GUID under a customer.
Cons:
If querying by customer, you need to scan across multiple composite keys.
Retrieving events for all GUIDs together under a customer is more complex.
Option 2: cust_xref_id as Row Key, Reverse Timestamp Columns (Chosen Option)
In this design, the row key is cust_xref_id and each event is stored as a column, with reverse timestamps used to ensure the most recent events are easily retrieved.

Row Key: cust_xref_id
Columns: Each event is stored with the reverse timestamp as the column qualifier, and the event data includes the GUID and other event details.
Example:
Row Key (cust_xref_id)	Columns
cust_123	rev_ts_1: {GUID: guid_abc, Event Data}
rev_ts_2: {GUID: guid_abc, Event Data}
rev_ts_3: {GUID: guid_xyz, Event Data}
Pros:
Efficient retrieval of recent events across all GUIDs for a specific customer.
Keeps data organized by customer, making it easy to retrieve all events related to that customer.
Cons:
If querying by GUID, you need to first perform a lookup in the lookup table to find the cust_xref_id.
Option 3: cust_xref_id as Row Key, Composite Columns (GUID + Timestamp)
Here, the row key is still cust_xref_id, but each column qualifier is a composite of the GUID and the event timestamp.

Row Key: cust_xref_id
Columns: GUID + timestamp as the column qualifier, with the event data stored in the column value.
Example:
Row Key (cust_xref_id)	Columns
cust_123	guid_abc + ts_1: {Event Data}
guid_abc + ts_2: {Event Data}
guid_xyz + ts_1: {Event Data}
Pros:
Keeps GUID-specific events separated in the columns, which could be useful for scenarios where GUID-based retrieval is needed.
Cons:
Events for different GUIDs are not grouped together by timestamp, making it harder to retrieve the most recent events across all GUIDs for a customer.
Sorting by GUID first may complicate customer-centric queries.
Option 4: GUID as Row Key, JSON as Event Data
In this design, the GUID is the row key, and the event data (including customer information) is stored in JSON format within the columns.

Row Key: GUID
Columns: timestamp as the column qualifier, with the event data stored in JSON.
Example:
Row Key (GUID)	Columns
guid_abc	ts_1: {cust_xref_id: cust_123, Event Data}
ts_2: {cust_xref_id: cust_123, Event Data}
Pros:
Simple design if GUID-based lookups are a priority, as events are directly tied to the GUID.
Cons:
Requires a lookup in the guid_index table for customer-centric queries, which adds complexity and slows down retrieval when querying by cust_xref_id.
Not as efficient for customer-based retrievals.
Final Decision: cust_xref_id with Reverse Timestamp Columns (Option 2)
We chose Option 2 (cust_xref_id as row key, reverse timestamp as columns) because:

Efficient Retrieval by Customer: Most of our queries will focus on retrieving events based on a customer (cust_xref_id), and this design makes it easy to fetch the most recent events across all GUIDs for that customer.
Scalable and Maintainable: By storing events based on reverse timestamps, we ensure that rows are kept manageable while still allowing efficient retrieval.
Balanced Flexibility: While GUID-based lookups require a quick reference to the lookup table, this design strikes the best balance for our primary use case of customer-centric queries.
Conclusion
The chosen design efficiently meets our requirements for fast event retrieval by customer while maintaining scalability and simplicity. With a separate lookup table for GUID to cust_xref_id mapping, we ensure flexibility for both customer and GUID-centric queries when necessary.