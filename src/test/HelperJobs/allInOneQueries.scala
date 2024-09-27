import org.apache.hadoop.hbase.client.{ConnectionFactory, Get, Scan, ResultScanner}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}

object HBaseQueryApp {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: HBaseQueryApp <mode> [<id> <start_ts> <end_ts>]")
      System.exit(1)
    }

    val mode = args(0)
    val id = if (args.length > 1) args(1) else ""
    val startTs = if (args.length > 2) args(2).toLong else 0L
    val endTs = if (args.length > 3) args(3).toLong else Long.MaxValue

    mode match {
      case "latest" => getLatestValueAndTimestamp(id)
      case "historical" => getHistoricalDataInRange(id, startTs, endTs)
      case _ => println("Unknown mode. Use 'latest' or 'historical'.")
    }
  }

  def getLatestValueAndTimestamp(id: String): Unit = {
    val hbaseConfig = HBaseConfiguration.create()
    val connection = ConnectionFactory.createConnection(hbaseConfig)
    val table = connection.getTable(TableName.valueOf("your_table_name"))

    try {
      val get = new Get(Bytes.toBytes(id))
      get.addColumn(Bytes.toBytes("cf1"), Bytes.toBytes("latest_value"))
      get.addColumn(Bytes.toBytes("cf1"), Bytes.toBytes("latest_timestamp"))

      val result = table.get(get)
      val latestValue = Bytes.toString(result.getValue(Bytes.toBytes("cf1"), Bytes.toBytes("latest_value")))
      val latestTimestamp = Bytes.toString(result.getValue(Bytes.toBytes("cf1"), Bytes.toBytes("latest_timestamp"))).toLong

      println(s"Latest value: $latestValue, Latest timestamp: $latestTimestamp")
    } finally {
      table.close()
      connection.close()
    }
  }

  def getHistoricalDataInRange(id: String, startTs: Long, endTs: Long): Unit = {
    val hbaseConfig = HBaseConfiguration.create()
    val connection = ConnectionFactory.createConnection(hbaseConfig)
    val table = connection.getTable(TableName.valueOf("your_table_name"))

    try {
      val scan = new Scan()
      scan.setRowPrefixFilter(Bytes.toBytes(id))
      scan.setFilter(new ColumnPrefixFilter(Bytes.toBytes("value_")))

      val scanner = table.getScanner(scan)
      val iterator = scanner.iterator()

      while (iterator.hasNext) {
        val result = iterator.next()
        result.rawCells().foreach { cell =>
          val columnName = Bytes.toString(cell.getQualifierArray, cell.getQualifierOffset, cell.getQualifierLength)
          if (columnName.startsWith("value_")) {
            val timestamp = columnName.stripPrefix("value_").toLong
            if (timestamp >= startTs && timestamp <= endTs) {
              val value = Bytes.toString(cell.getValueArray, cell.getValueOffset, cell.getValueLength)
              println(s"Value at $timestamp: $value")
            }
          }
        }
      }
    } finally {
      table.close()
      connection.close()
    }
  }
}
