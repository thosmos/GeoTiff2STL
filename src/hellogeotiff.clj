(ns hellogeotiff
  (:gen-class)
  (:import [java.io File]
    ;[java.awt.image BufferedImage]
           [java.awt.image Raster RenderedImage]
    ;[javax.imageio ImageIO ImageReadParam ImageTypeSpecifier]
    ;[com.twelvemonkeys.imageio.plugins.tiff TIFFImageReader TIFFImageReaderSpi]
           [com.heightmap.stl StlObject]
    ;[javax.imageio.stream FileImageInputStream]
    ;[org.geotools.data FileDataStore FileDataStoreFinder]
           (org.geotools.gce.geotiff GeoTiffReader)
           (org.geotools.factory Hints)
           (org.geotools.coverage.grid GridCoverage2D)
           (org.opengis.referencing.crs CoordinateReferenceSystem)
           (org.opengis.geometry Envelope)
           (java.awt.geom Point2D Point2D$Float))
  (:require [clojure.pprint :refer [pprint]]))

;(defn dostuff []
;  (doseq [suffix (ImageIO/getReaderFileSuffixes)]
;    (println "suffix:" suffix))
;
;  (let [readers (ImageIO/getImageReadersByFormatName "TIFF")]
;    (while (. readers hasNext)
;      (println "reader: " (. readers next)))))

(defn min-fn
  ([x y]
   (if (> y 0)
     (min x y)
     (if (> x 0)
       x
       Float/MAX_VALUE)))
  ([x y & more]
   (reduce min-fn (min-fn x y) more)))

(defn -main
  "The Main."
  [& args]
  (println "GeoTIFF2STL!" args)
  ;(dostuff)
  (let [^String filename (first args)
        multiplier (Float/parseFloat (get (vec args) 1))
        z-lift (Float/parseFloat (get (vec args) 2))
        ^File file (File. filename)
        name (.getName file) ;

        ;^FileImageInputStream stream (FileImageInputStream. file)
        ;^TIFFImageReaderSpi TIFFspi (TIFFImageReaderSpi.)
        ;^TIFFImageReader reader (.createReaderInstance TIFFspi ".tif")
        ;_ (.setInput reader stream)
        ;^ImageReadParam param (.getDefaultReadParam reader)
        ;^ImageTypeSpecifier itype (.getRawImageType reader 0)
        ;_ (println "itype" (.toString itype))
        ;^Raster raster (.readRaster reader 0 param)
        ;width (.getWidth raster)
        ;height (.getHeight raster)

        reader (GeoTiffReader. file (Hints. Hints/FORCE_LONGITUDE_FIRST_AXIS_ORDER Boolean/TRUE))
        coverage ^GridCoverage2D (.read reader nil)

        elev (.evaluate coverage (Point2D$Float. 1.0 1.0) nil)
        _ (println "ELEV: " (.toString elev))

        crs ^CoordinateReferenceSystem (.getCoordinateReferenceSystem2D coverage)
        env ^Envelope (.getEnvelope coverage)
        image ^RenderedImage (.getRenderedImage coverage)

        width (.getWidth image)
        height (.getHeight image)
        raster ^Raster (.getData image)

        ;^BufferedImage image (try
        ;                       (ImageIO/read file)
        ;                       (catch Exception ex (print ex)))
        ;width (.getWidth image)
        ;_ (println "width" width)
        ;height (.getHeight image)
        ;_ (println "height" height)
        ;^Raster raster (.getData image)
        hmap (make-array Float/TYPE width height)]

    (println "Calculating samples and min/max (z-lift: " z-lift ", mult: " multiplier ")")
    (let [keys (vec (for [w (range width) h (range height)]
                      [w h]))
          samples (vec (for [[w h] keys]
                         (let [s (.getSampleFloat raster w h 0)
                               ;s2 (.getRGB image w h)
                               s' (if (> s 0)
                                    (+ z-lift (* multiplier (/ s 100.0)))
                                    0)]
                           (println w h s s')
                           s')))
          min (reduce min-fn samples)
          max (reduce max samples)]
      (println "min: " min "max: " max)
      (when (> min 10)
        (println "min is over 10, consider a lower z-lift value"))
      (println "Converting to java array")
      (doseq [i (range (count samples))]
        (let [[w h] (get keys i)
              sample (get samples i)]
          (aset-float hmap (- (- width 1) w) h
            sample))))


    ;(println "Saving STL")
    ;(let [stl (StlObject/fromHeightmap name height width hmap)
    ;      name (if (.contains name ".")
    ;             (.substring name 0 (.lastIndexOf name "."))
    ;             name)]
    ;  (set! (. stl -path) (str name ".stl"))
    ;  (.save stl StlObject/FILE_BINARY))


    ))
