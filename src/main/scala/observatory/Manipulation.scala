package observatory
import Visualization._

import scala.collection.{GenIterable, GenMap}
/**
  * 4th milestone: value-added information
  */
object Manipulation extends ManipulationInterface {
  private val locations = for {
    lat <- -89 to 90
    lon <- -180 to 179
  } yield Location(lat, lon)

  class TemperatureGrid(temperatures: Iterable[(Location, Double)]) {

    private val limit = 1000.0
    private val p = 2

    private lazy val weightMap: GenMap[Location, GenIterable[Double]] = {

      def getWeights(location: Location) = temperatures.map {
        case (loc, _) =>
          val d = distance(loc, location)
          if (d > limit)
            1.0 / Math.pow(d, p)
          else
            1.0
      }
      locations.par.map(loc => (loc, getWeights(loc))).toMap
    }

    def getTemperatures(l : GridLocation): Double = {
      val location = Location(l.lat, l.lon)
      val weights = weightMap(location)

      val sumOfWeightedTemps = temperatures.zip(weights).map {
        case ((loc, temp), weight) =>
          val d = distance(loc, location)
          if (d > limit) {
            weight * temp
          } else
            temp
      }.sum

      sumOfWeightedTemps / weights.sum
    }
  }
  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Temperature)]): GridLocation => Temperature = {
    val grid = new TemperatureGrid(temperatures)
    grid.getTemperatures
  }

  /**
    * @param temperatures Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperatures: Iterable[Iterable[(Location, Temperature)]]): GridLocation => Temperature = {
    val size = temperatures.size
    val data = temperatures.par
      .map(temperatures => makeGrid(temperatures))
      .flatMap(grid => locations.map(x => ((x.lat.toInt, x.lon.toInt), grid(GridLocation(x.lat.toInt, x.lon.toInt)))))
      .foldLeft(Map.empty[(Int, Int), Double]){(x,y) =>
        x + (y._1 -> (x.getOrElse(y._1, 0D) + y._2))
      }.mapValues(_ / size)
    location => data((location.lat, location.lon))
  }

  /**
    * @param temperatures Known temperatures
    * @param normals A grid containing the “normal” temperatures
    * @return A grid containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Temperature)], normals: GridLocation => Temperature): GridLocation => Temperature = {
    val initialGrid = makeGrid(temperatures)
    val result = locations.map(
      x => {
        val k = GridLocation(x.lat.toInt, x.lon.toInt)
        (k, initialGrid(k) - normals(k))
      }
    ).toMap
    l => result(l)
  }
}

