package observatory

import java.time.LocalDate

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.io.Source

/**
  * 1st milestone: data extraction
  */
object Extraction extends ExtractionInterface {
  private val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Observatory")
      .master("local[*]")
      .getOrCreate()

  import spark.implicits._


  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Temperature)] = {

    val stationsFileData = stationsCsvToDataFrame(stationsFile).toDF("stn", "wban", "lt", "lg")
      .persist()

    val temperatureFileData = temperaturesCsvToDataFrame(temperaturesFile)
      .toDF("stn", "wban", "month", "day", "temp")
      .persist()

    val data = stationsFileData.join(
      temperatureFileData,
      stationsFileData("stn") === (temperatureFileData("stn")) &&
        stationsFileData("wban") === (temperatureFileData("wban"))
    ).persist()

    data.collect().map(row =>
      (LocalDate.of(year, row.getAs[Int]("month"), row.getAs[Int]("day")),
        Location(row.getAs[Double]("lt"), row.getAs[Double]("lg")),
        toCelsius(row.getAs[Double]("temp"))
        )
    )
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] = {
    records.groupBy(x => x._2).mapValues(v => v.map(x => (x._3, 1)).foldLeft(0.0, 0.0)((x,y) => (x._1 + y._1 , x._2 + y._2))).map(x => (x._1, x._2._1 / x._2._2))
  }

  private def toCelsius(f: Double): Double = ((f + 40) / 1.8) - 40

  def stationsCsvToDataFrame(resource: String) : RDD[(String, String, Double, Double)] = {
    val stationsFileData = readCsv(resource)
    var data = List[(String, String, Double, Double)]()
    for (line <- stationsFileData.getLines) {
      val cols = line.split(",")
      if(cols.length == 4 &&
        cols(2) != "" && cols(3) != "" && (cols(0) != "" || cols(1) != "")){
          data = data :+ (cols(0), cols(1), cols(2).toDouble, cols(3).toDouble)
      }
    }
    stationsFileData.close
    spark.sparkContext.parallelize(data).persist()
  }

  def temperaturesCsvToDataFrame(resource: String) : RDD[(String, String, Int, Int, Double)] = {
    val temperaturesFileData = readCsv(resource)
    var data = List[(String, String, Int, Int, Double)]()
    for (line <- temperaturesFileData.getLines) {
      val cols = line.split(",")
      if(cols.length == 5 &&
        cols(2) != "" && cols(3) != "" && cols(4) != "" &&
        (cols(0) != "" || cols(1) != ""))
      data = data :+ (cols(0), cols(1), cols(2).toInt, cols(3).toInt, cols(4).toDouble)
    }
    temperaturesFileData.close
    spark.sparkContext.parallelize(data).persist()
  }

  def readCsv(resource : String): Source = {
    Source.fromInputStream(getClass.getResourceAsStream(resource), "utf-8")
  }

}
