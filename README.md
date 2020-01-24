# GeoTIFF2STL

This converts DEM or SRTM elevation data in GeoTIFF format into an STL 3D model.

I generally make models of watersheds, which you can see at
 [3dwatersheds.com](http://3dwatersheds.com).

run on the command line:
```
java -jar GeoTiff2STL.jar -m 4.0 -z 5.0 ../yuba/yuba-albers-200m.tif
```

or run a repl and then:
```
(-main "../yuba/yuba-albers-200m.tif" "4.0" "5.0")
```


There are two commandline options:

`-m`     multiplier: increases the vertical height of the output model

`-z`     z-lift: alters the overall height of the model, a negative number will lower it

