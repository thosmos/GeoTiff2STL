# GeoTIFF2STL

This is an attempt to ease the work flow of converting DEM or SRTM elevation data in GeoTIFF
(or other similar raster formats supported by GDAL) into a tangible 3D model.

I generally make models of watersheds, which you can see at
 [3dwatersheds.com](http://3dwatersheds.com).

Currently this app depends on [GDAL](http://gdal.org), which is a native library.

On a Mac I did:
```
brew tap osgeo/osgeo4mac
brew install osgeo/osgeo4mac/gdal2 --with-java --with-swig-java
```
This will be different for different platforms.

You'll need to tell java where your GDAL is like so:
```
 java -Djava.library.path=.:/usr/local/opt/gdal2/lib -jar geotiff2stl-1.0.0-SNAPSHOT-standalone.jar -m 2 -z -10 ./dem-data-1arc.tif
```
There are two commandline options:

`-m`     multiplier: increases the vertical height of the output model

`-z`     z-lift: alters the overall height of the model, a negative number will lower it

