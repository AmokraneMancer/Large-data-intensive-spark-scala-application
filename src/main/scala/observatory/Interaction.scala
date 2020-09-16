package observatory

import com.sksamuel.scrimage.{Image, Pixel, Color => ScrimageColor}
import observatory.Visualization._

/**
  * 3rd milestone: interactive visualization
  */
object Interaction extends InteractionInterface {

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location = {
    val n = Math.pow(2, tile.zoom)
    val lon = tile.x / n * 360.0 - 180.0
    val latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * tile.y / n)))
    val lat = latRad * 180.0 / Math.PI
    Location(lat, lon)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param tile Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */
  def tile(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)], tile: Tile): Image = {
    val (width, height, transparency) = (256, 256, 127)
    val (xStart, yStart, zoom) = (tile.x * width, tile.y * height, tile.zoom)
    val arrayOfPixels = (
      for {
        i <- yStart until yStart + height
        j <- xStart until xStart + width
      } yield (j, i)
    ).toStream
      .par
      .map(x => tileLocation(Tile(x._1, x._2, zoom + 8)))
      .map(predictTemperature(temperatures, _))
      .map(interpolateColor(colors, _))
      .map(color => Pixel(ScrimageColor(color.red, color.green, color.blue, transparency)))
      .toArray

    Image(width, height,arrayOfPixels)
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit = {
    val zoomLevels = 0 to 3
    yearlyData.toStream.par.foreach(
      {
        case (year, data) => {
          zoomLevels.toStream.par.foreach(
            z => {
              (for {
                i <- 0 until Math.pow(2, z).toInt
                j <- 0 until Math.pow(2, z).toInt
              } yield (i,j))
                .toStream
                .par
                .foreach(
                  {
                    case (x ,y) => generateImage(year, Tile(x, y, z), data)
                  }
                )
            }
          )
        }
      }
    )
  }

}

/*val zoom = 0 to 3

    yearlyData.toStream.par.foreach(
      {
        case (year, data) => {
          zoom.toStream.par.foreach(
            z => {
              val tiles = for {
                j <- 0 until Math.pow(2, z).toInt
                i <- 0 until Math.pow(2, z).toInt
              } yield (i,j)
              tiles.toStream.par.foreach(
                { case (x, y) => generateImage(year, Tile(x, y, z), data) }
              )
            }
          )
        }
      }
    )*/