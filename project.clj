(defproject geotiff2stl "1.0.0-SNAPSHOT"
  :description "Convert GeoTIFF files to STL"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.apache.commons/commons-lang3 "3.6"]
                 [org.gdal/gdal "2.2.0"]]
  :jvm-opts ["-Djava.library.path=.:/usr/local/opt/gdal2/lib"]
  :java-source-paths ["java"]
  :source-paths ["src"]
  :main hellogeotiff; ^:skip-aot hellogeotiff
  :profiles {:uberjar {:aot [hellogeotiff] }})

