# Large data-intensive spark-scala application 

# About this project

This project consists of developing a large data-intensive application using real-world data. this application processes several gigabytes of data. It includes a web user interface for showing interactive visualizations of the evolution of temperatures over time all over the world. 

The development of such an application involves: 
— transforming data provided by weather stations into meaningful information like, for instance, the average temperature of each point of the globe over the last ten years ; 
— then, making images from this information by using spatial and linear interpolation techniques ; 
— finally, implementing how the user interface will react to users’ actions. 

# Load data set

[Get full data](http://alaska.epfl.ch/files/scala-capstone-data.zip)

The stations.csv file contains one row per weather station, with the following columns:
# [![stations.jpg](https://i.postimg.cc/DzJ0t9FD/3.png)](https://postimg.cc/Jy8RXdsc)

The temperature files contain one row per day of the year, with the following columns:
# [![temperatures.jpg](https://i.postimg.cc/jS9dQMNh/4.png)](https://postimg.cc/3kXT3Cty)

We have to load each temperature file in a RDD than transform it to a DataFrame to perform a join between each temperature DF and stations DF.

```
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
```

# Spatial interpolation

This method takes a sequence of known temperatures at the given locations, and a location where we want to guess the temperature, and returns an estimate based on the *** inverse distance weighting algorithm *** . To approximate the distance between two locations, use the *** great-circle distance formula ***.

```
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
```
 
# Visualization of Temperatures

# [![TempVisualization.jpg](https://i.postimg.cc/QCDstP7f/1.png)](https://postimg.cc/WF5xY5XZ)

```
val colors = List(
    (60.0, Color(255,255,255)),
    (32.0, Color(255,0,0)),
    (12.0, Color(255,255,0)),
    (0.0, Color(0,255,255)),
    (-15.0, Color(0,0,255)),
    (-27.0, Color(255,0,255)),
    (-50.0, Color(33,0,107)),
    (-60.0, Color(0,0,0))
)

...

def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
): Unit
```

This method returns a 256×256 image showing the given temperatures, using the given color scale, at the location corresponding to the given zoom, x and y values.

Once we are able to generate tiles, we can embed them in a Web page. To achieve this we first have to generate all the tiles for zoom levels going from 0 to 3. (we used small files for testing : 2021.csv, 2022.csv, stationsTest.csv). 
To each zoom level corresponds tiles partitioning the space. For instance, for the zoom level “0” there is only one tile, whose (x, y) coordinates are (0, 0). For the zoom level “1”, there are four tiles, whose coordinates are (0, 0) (top-left), (0, 1) (bottom-left), (1, 0) (top-right) and (1, 1) (bottom-right) and so on ...

The interaction.html file contains a minimalist Web application displaying a map and a temperature overlay. In order to integrate our tiles with the application, we must generate them in files located according to the following scheme: target/temperatures/year/zoom/x-y.png. Where <zoom> is replaced by the zoom level, and <x> and <y> are replaced by the tile coordinates. For instance, the tile located at coordinates (0, 1), for the zoom level 1 will have to be located in the following file: target/temperatures/2015/1/0-1.png.

Once we have generated the files we want to visualize, we have just to open the interaction.html file in a Web browser.

# Visualization of deviations

[![DevVisualization.png](https://i.postimg.cc/0N7BQNCq/2.png)](https://postimg.cc/Nyf4CBVN)


One of the primary goals of this project is to be able to visualize the evolution of the climate. If we just Visualize the temperatures of different years, it is actually quite hard to really measure how the temperatures have evolved since 1975. So We have to compute and show deviations over the years.

Computing deviations means comparing a value to a previous value which serves as a reference, or a “normal” temperature. We will first compute the average temperatures all over the world between 1975 and 1990. This will constitute our reference temperatures, which we refer to as “normals”. We will then compare the yearly average temperatures, for each year between 1991 and 2015, to the normals.

Generate tiles for zoom levels going from 0 to 3, showing the deviations. We use the output method of Image to write the tiles on our file system, under a location named according to the following scheme: target/deviations/year/zoom/x-y.png.

```
val deviationImage = visualizeGrid(deviation(normal, average(locationAvgs.map(_._2))), colors, tile )
```

# Run !

Execute the following sbt command: 

```
capstoneUI/fastOptJS
```
This will compile part of our Scala code to JavaScript instead of JVM bytecode, using Scala.js. To see it in action, we have just to open the interaction2.html file.
