(defproject geotiff2stl "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :repositories {"osgeo-geotools" "http://download.osgeo.org/webdav/geotools"
                 "local" "file:maven_repository"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.geotools/gt-main "14.3"]
                 [org.geotools/gt-shapefile "14.3"]
                 [com.twelvemonkeys.imageio/imageio-tiff "3.2.1"]
                 [com.heightmap/heightmap2stl "0.1.1"]]
  ;:resource-paths ["lib/heightmap2stl.jar"]
  :main hellogeotiff; ^:skip-aot hellogeotiff
  ;:target-path "target/%s"
  :profiles {:uberjar {:aot [hellogeotiff] ;:all
                       }})

