

import org.apache.hadoop.hbase.client.{ConnectionFactory, Get, Result}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}

def getLatestValueAndTimestamp(id: String): Option[(String, Long)] = {
  val hbaseConfig = HBaseConfiguration.create()
  val connection = ConnectionFactory.createConnection(hbaseConfig)
  val table = connection.getTable(TableName.valueOf("your_table_name"))

  try {
    // Create Get object to retrieve the row by ID
    val get = new Get(Bytes.toBytes(id))
    
    // Specify the columns to retrieve (latest_value and latest_timestamp)
    get.addColumn(Bytes.toBytes("cf1"), Bytes.toBytes("latest_value"))
    get.addColumn(Bytes.toBytes("cf1"), Bytes.toBytes("latest_timestamp"))

    // Execute the Get and fetch the result
    val result: Result = table.get(get)

    // Extract the latest_value and latest_timestamp
    val latestValue = Bytes.toString(result.getValue(Bytes.toBytes("cf1"), Bytes.toBytes("latest_value")))
    val latestTimestamp = Bytes.toString(result.getValue(Bytes.toBytes("cf1"), Bytes.toBytes("latest_timestamp"))).toLong

    // Return the latest value and timestamp as a tuple
    Some((latestValue, latestTimestamp))
  } catch {
    case e: Exception =>
      e.printStackTrace()
      None
  } finally {
    table.close()
    connection.close()
  }
}
