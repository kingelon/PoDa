import org.apache.hadoop.hbase.client.{ConnectionFactory, Get, ResultScanner, Scan}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}

def getAllHistoricalData(id: String): Map[Long, String] = {
  val hbaseConfig = HBaseConfiguration.create()
  val connection = ConnectionFactory.createConnection(hbaseConfig)
  val table = connection.getTable(TableName.valueOf("your_table_name"))

  try {
    // Create a Scan object for the row key
    val scan = new Scan()
    scan.setRowPrefixFilter(Bytes.toBytes(id))

    // Filter only the columns starting with "value_"
    scan.setFilter(new ColumnPrefixFilter(Bytes.toBytes("value_")))

    // Execute the scan
    val scanner: ResultScanner = table.getScanner(scan)

    // Process the results and extract timestamp -> value pairs
    val resultIterator = scanner.iterator()
    var results = Map[Long, String]()

    while (resultIterator.hasNext) {
      val result = resultIterator.next()
      result.rawCells().foreach { cell =>
        val columnName = Bytes.toString(cell.getQualifierArray, cell.getQualifierOffset, cell.getQualifierLength)
        if (columnName.startsWith("value_")) {
          val timestamp = columnName.stripPrefix("value_").toLong
          val value = Bytes.toString(cell.getValueArray, cell.getValueOffset, cell.getValueLength)
          results += (timestamp -> value)
        }
      }
    }

    // Return the historical data as a Map
    results
  } catch {
    case e: Exception =>
      e.printStackTrace()
      Map.empty[Long, String]
  } finally {
    table.close()
    connection.close()
  }
}
