package observatory

import java.io.File
import java.nio.file.Files.createDirectories
import java.nio.file.Paths

import Extraction._
import Interaction._
import com.sksamuel.scrimage.writer
import observatory.Manipulation._
import observatory.Visualization2.visualizeGrid

object Main extends App {

  //small list of colors for testing
  val colors =
    List((60.0, Color(255,255,255)), (32.0, Color(255, 0, 0)),
      (12.0, Color(255, 255, 0)), (0.0, Color(0, 255, 255)),
      (-15.0, Color(0, 0, 255)), (-27.0, Color(255, 0, 255)),
      (-50.0, Color(33, 0, 107)), (-60.0, Color(0, 0, 0)))

  val stations = "/testStations.csv"

  val files = (2020 to 2021).map(year => (year, s"/$year.csv"))

  val locationAvgs = files.toStream
    .map(data => (data._1, locateTemperatures(data._1, stations, data._2)))
    .map(data => (data._1, locationYearlyAverageRecords(data._2)))

  println("data loaded")

  generateTiles[Iterable[(Location, Double)]](
    locationAvgs,
    (year, t, data) => {
      val x = t.x
      val y = t.y
      val z = t.zoom

      val pathForTemperatures = Paths.get(s"target/temperatures/$year/$z/")
      val pathForDeviations = Paths.get(s"target/deviations/$year/$z/")

      createDirectories(pathForTemperatures)
      createDirectories(pathForDeviations)

      val imageFile = new File(pathForTemperatures.toString + s"/$x-$y.png")
      val deviationFile = new File(pathForDeviations.toString + s"/$x-$y.png")

      val start = System.nanoTime()
      println(s"$year $z $x $y")
      val temperatureImage = tile(data, colors, t)
      val deviationImage = visualizeGrid(deviation(data, average(locationAvgs.map(_._2))), colors, t )

      println(s"$year $z $x $y ${(System.nanoTime() - start) / 1000000000}")

      temperatureImage.output(imageFile)
      deviationImage.output(deviationFile)
     }
  )
}
