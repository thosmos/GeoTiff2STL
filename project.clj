(defproject geotiff2stl "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  ;:repositories { ;"osgeo-geotools" "http://download.osgeo.org/webdav/geotools"
  ;               ;"local" "file:maven_repository"
  ;               }
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;[org.geotools/gt-main "14.3"]
                 ;[org.geotools/gt-shapefile "14.3"]
                 ;[com.twelvemonkeys.imageio/imageio-tiff "3.3.2"]
                 ;[com.heightmap/heightmap2stl "0.1.1"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.apache.commons/commons-lang3 "3.6"]
                 ;[im.chit/lucid.core.java "1.3.13"]
                 [org.gdal/gdal "2.2.0"]]
  :jvm-opts ["-Djava.library.path=.:/usr/local/opt/gdal2/lib"]
  :java-source-paths ["java"]
  :source-paths ["src"]
  :main hellogeotiff; ^:skip-aot hellogeotiff
  ;:target-path "target/%s"
  :profiles {:uberjar {:aot [hellogeotiff] }})

