(defproject geotiff2stl "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :repositories {"osgeo-geotools" "http://download.osgeo.org/webdav/geotools"
                 "local" "file:maven_repository"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.geotools/gt-main "18.0"]
                 ;[org.geotools/gt-shapefile "18.0"]
                 [org.geotools/gt-geotiff "18.0"]
                 [org.geotools/gt-epsg-hsql "18.0"]
                 [com.heightmap/heightmap2stl "0.1.1"]
                 [org.clojure/tools.cli "0.3.5"]]
  ;:resource-paths ["lib/heightmap2stl.jar"]
  :java-source-paths ["java"]
  ;:main hellogeotiff; ^:skip-aot hellogeotiff
  :main ^:skip-aot hellogeotiff
  ;:target-path "target/%s"
  :profiles {:uberjar {:aot [hellogeotiff] }})

