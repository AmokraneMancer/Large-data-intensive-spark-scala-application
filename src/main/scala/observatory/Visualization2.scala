package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import Interaction._, Visualization._

/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 extends Visualization2Interface {

  /**
    * @param point (x, y) coordinates of a point in the grid cell
    * @param d00 Top-left value
    * @param d01 Bottom-left value
    * @param d10 Top-right value
    * @param d11 Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    */
  def bilinearInterpolation(
    point: CellPoint,
    d00: Temperature,
    d01: Temperature,
    d10: Temperature,
    d11: Temperature
  ): Temperature = {
    (d00 * (1 - point.x) * (1-point.y)) + (d10 * point.x * (1-point.y))+
      (d01 * (1-point.x) * point.y) + (d11 * point.x * point.y)
  }

  def interpolationProcess(x: Double, z0: Double, z1: Double) =  z0 * x + z1 * (1 - x)

  /**
    * @param grid Grid to visualize
    * @param colors Color scale to use
    * @param tile Tile coordinates to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
    grid: GridLocation => Temperature,
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): Image = {

    val coordinates = for {
      j <- 0  until 256
      i <- 0  until 256
    } yield (256 * tile.x + i, 256 * tile.y + j)

    val pixels = coordinates.map(
      location => {
        val tLocation = tileLocation(Tile(location._1, location._2, tile.zoom + 8))
        val minLat = normalizeBounds(math.floor(tLocation.lat).toInt, -88, 89)
        val minLon = normalizeBounds(math.floor(tLocation.lon).toInt, -179, 178)
        val temp = bilinearInterpolation(
          CellPoint(tLocation.lat - minLat, tLocation.lon - minLon),
          grid(GridLocation(minLat, minLon)),
          grid(GridLocation(minLat, minLon + 1)),
          grid(GridLocation(minLat + 1, minLon)),
          grid(GridLocation(minLat + 1, minLon + 1))
        )
        val color = interpolateColor(colors, temp)
        Pixel(color.red, color.green, color.blue, 127)
      }
    ).toArray
    Image(256, 256, pixels)
  }

  def normalizeBounds(value: Int, min: Int, max: Int): Int = {
    math.max(math.min(value, max), min)
  }

}
