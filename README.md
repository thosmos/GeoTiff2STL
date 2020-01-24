# GeoTIFF2STL

This converts DEM or SRTM elevation data in GeoTIFF format into an STL 3D model.

I generally make models of watersheds, which you can see at
 [3dwatersheds.com](http://3dwatersheds.com).

run on the command line:
```
clojure -m hellogeotiff -l 10 -s 27.514 ../yuba/DEM-yuba-27m-albers.tif
```

There are some commandline options:
```
Options:
  -m, --multiplier MULT  4    Height Multiplier exagerate the height of the model by this multiple
  -l, --lift LIFT        0    Lift/Reduce the overall height of the model
  -s, --size SIZE        100  Cell Size in height units (=> 100 means 100m resolution cells with elevation in meters)
  -h, --help
```

