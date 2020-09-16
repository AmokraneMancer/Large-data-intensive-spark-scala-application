package observatory

import com.sksamuel.scrimage.{Image, Pixel, Color => ScrimageColor}

/**
  * 2nd milestone: basic visualization
  */
object Visualization extends VisualizationInterface {

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Temperature)], location: Location): Temperature = {
    val parTemperatures = temperatures.toStream.par
    val filtered = parTemperatures.filter(x => distance(x._1, location) <= 1)
    if(filtered.isEmpty) {
      val (tmp, count) = parTemperatures.map(x => {
        (invertedDistance(x._1, location) * x._2, invertedDistance(x._1, location))
      }).aggregate((0.0, 0.0))(
        (x, y) => (x._1 + y._1, x._2 + y._2),
        (acc1, acc2) => (acc1._1 + acc2._1, acc1._2 + acc2._2)
      )
      tmp / count
    } else {
      filtered.take(1).head._2
    }
  }

  def distance(l1 : Location, l2: Location) : Double = {
    if(l1.equals(l2)) 0.0
    else {
      val phi1 = math.toRadians(l1.lat)
      val phi2 = math.toRadians(l2.lat)
      val lambda = math.toRadians(l1.lon) - math.toRadians(l2.lon)
      math.toDegrees(6371.toRadians * math.acos(math.sin(phi1) * math.sin(phi2)
        + math.cos(phi1) * math.cos(phi2) * math.cos(lambda))) //6371
    }
  }

  def invertedDistance(l1: Location, l2: Location) : Double = {
    math.pow(1 / distance(l1, l2), 2)
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Temperature, Color)], value: Temperature): Color = {
    val listOfPoints = points.toList.sortBy(_._1)
    if (listOfPoints(0)._1 >= value) listOfPoints(0)._2
    else if (listOfPoints.last._1 <= value) listOfPoints.last._2
    else {
      val t0 = listOfPoints(listOfPoints.lastIndexWhere{case (temp, _) => temp <= value})
      val t1 = listOfPoints(listOfPoints.indexWhere{case (temp, _) => temp >= value})
      if(t0._1 != t1._1) {
        val r = linearInterpolation((t0._1, t0._2.red), (t1._1, t1._2.red), value)
        val g = linearInterpolation((t0._1, t0._2.green), (t1._1, t1._2.green), value)
        val b = linearInterpolation((t0._1, t0._2.blue), (t1._1, t1._2.blue), value)
        Color(r,g,b)
      } else {
        t0._2
      }
    }
  }

  def linearInterpolation(t0: (Double, Int), t1: (Double, Int), x: Double) = {
    val (x0, y0) = t0
    val (x1, y1) = t1
    Math.round(((y0 * (x1 - x)) + (y1 * (x - x0))) / (x1 - x0)).toInt
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)]): Image = {
    val (width, height) = (360, 180)
    val pixels: Array[Pixel] = new Array[Pixel](width * height)

    for {
      i <- 0 until height
      j <- 0 until width
    } yield {
      val predictedTemperature = predictTemperature(temperatures, Location(90 - i, j - 180))
      val color = interpolateColor(colors, predictedTemperature)
      val pixel: Pixel = Pixel(ScrimageColor(color.red, color.green, color.blue))
      pixels(i * width + j) = pixel
    }
    Image(width, height, pixels)
  }
}

