(ns hellogeotiff
  (:gen-class)
  (:import [com.heightmap.stl StlObject ModelObject]
           [java.io File]
           [java.awt.image Raster RenderedImage]
           (org.geotools.gce.geotiff GeoTiffReader)
           (org.geotools.coverage.grid GridCoverage2D))
           ;(org.opengis.referencing.crs CoordinateReferenceSystem)
           ;(org.opengis.geometry Envelope)
           ;(java.awt.geom Point2D Point2D$Float))
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn exit [status msg]
  (println msg)
  (println "EXITING" status))
  ;(System/exit status)


(defn ^String getDataType [v]
  (case v
    0 "Unknown"
    1 "Byte"
    2 "UInt16"
    3 "Int16"
    4 "UInt32"
    5 "Int32"
    6 "Float32"
    7 "Float64"
    8 "CInt16"
    9 "CInt32"
    10 "CFloat32"
    11 "CFloat64"
    12 "CFloat64"))

(def cli-options
  ;; An option with a required argument
  [["-m" "--multiplier MULT" "Height Multiplier"
    :id :multiplier
    :default 4.0
    :parse-fn #(Float/parseFloat %)
    :validate [#(< 0.0 % 20.0) "Must be a number between 0 and 20"]]
   ;; A non-idempotent option
   ["-z" "--zlift ZLIFT" "Z Lift/Reduce"
    :id :z-lift
    :default 0.0
    :parse-fn #(Float/parseFloat %)]
   ;["-s" "--slice" "Slice into multiple sections"
   ; :id :slice]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Welcome to GeoTIFF2STL. May it be of use to you."
        ""
        "Usage: java -jar GeoTIFF2STL.jar [options] path-to-geotiff-file"
        ""
        "Options:"
        options-summary]
    (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
    (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (println "Options: " options)
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (or (< (count arguments) 1)
        (not (.exists (io/as-file (first arguments)))))
      {:exit-message (str (usage summary) \newline \newline "Where's the GeoTIFF file?" \newline options)}
      (and (= 1 (count arguments))
        (.exists (io/as-file (first arguments))))
      {:filename (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn min-fn
  ([x y]
   (if (> y 0)
     (min x y)
     (if (> x 0)
       x
       Float/MAX_VALUE)))
  ([x y & more]
   (reduce min-fn (min-fn x y) more)))

(defn get-slice-size [pixels slice slice-count]
  (let [val (int (/ pixels slice-count))
        last-val (int (- pixels (* val (- slice-count 1))))]
    (if (< slice (- slice-count 1))
      val
      last-val)))

(defn get-slice-offset [pixels slice slice-count]
  (* (int (/ pixels slice-count)) slice))

(defn section-it [pixels sections]
  (println "section-it" pixels sections)
  (let [val (int (/ pixels sections))
        last-val (int (- pixels (* val (- sections 1))))]
    (vec (for [i (range sections)]
          (if (= i (- sections 1))
            last-val
            val)))))

(defn -main
  "The Main."
  [& args]
  (println "GeoTIFF2STL!" args)
  (let [^String filename (first args)
        multiplier (Float/parseFloat (get (vec args) 1 "4.0"))
        z-lift (Float/parseFloat (get (vec args) 2 "0.0"))
        ^File file (File. filename)
        name (.getName file) ;
        pixel-size-in-height-units 100.0 ;27.514 DEM albers 27.51444296347608898,-27.51444296347608898

        name (if (.contains name ".")
               (.substring name 0 (.lastIndexOf name "."))
               name)
        name (str name
               "-" (int pixel-size-in-height-units) "size"
               "-" (int multiplier) "mult"
               "-" (int z-lift) "lift"
               ".stl")

        _ (println "Making" name)

        reader (GeoTiffReader. file)
        coverage ^GridCoverage2D (.read reader nil)

        _ (comment
            (def filename "../yuba/DEM-yuba-27m-albers.tif")
            (def file (File. filename))
            (def multiplier 4.0)
            (def z-lift 5.0)
            (def reader (GeoTiffReader. file))
            (def coverage ^GridCoverage2D (.read reader nil)))

        ;crs ^CoordinateReferenceSystem (.getCoordinateReferenceSystem2D coverage)
        ;env ^Envelope (.getEnvelope coverage)
        image ^RenderedImage (.getRenderedImage coverage)

        width (.getWidth image)
        height (.getHeight image)
        raster ^Raster (.getData image)

        hmap (make-array Float/TYPE width height)]

    (println "Calculating samples and min/max (z-lift: " z-lift ", mult: " multiplier ")")
    (let [keys (vec (for [w (range width) h (range height)]
                      [w h]))
          samples (vec (for [[w h] keys]
                         (let [s (.getSampleFloat raster w h 0)
                               ;s2 (.getRGB image w h)
                               s' (if (> s 0)
                                    (+ z-lift (* multiplier (/ s pixel-size-in-height-units)))
                                    0)]
                           ;(println w h s s')
                           s')))
          min (reduce min-fn samples)
          max (reduce max samples)]
      (println "min:" min "max:" max "width:" width "height:" height)
      (when (> min 10)
        (println "min is over 10, consider a lower z-lift value"))
      (println "Converting to java array")
      (doseq [i (range (count samples))]
        (let [[w h] (get keys i)
              sample (get samples i)]
          (aset-float hmap (- (- width 1) w) h
                      sample))))


    (println "Saving STL")
    (let [stl (StlObject/fromHeightmap name height width hmap)]
      (set! (. stl -path) name)
      (.save stl StlObject/FILE_BINARY))))


;; TODO incorporate GDAL slicing back into the new GeoTiff code above

;(defn -main
;  "The Main."
;  [& args]
;
;  (println "GeoTIFF2STL!")
;
;  (let [{:keys [filename options exit-message ok?]} (validate-args args)]
;    (if exit-message
;      (exit (if ok? 0 1) exit-message)
;      (let [^File file        (io/file filename)
;            ^String name      (.getName file)
;            ^String path      (.getParent file)
;            ^float multiplier (:multiplier options)
;            ^float z-lift     (:z-lift options)
;            ^Boolean slice    (:slice options)
;            slices            (if slice 2 1)
;
;            ;^Dataset ds       (gdal/Open filename gdalconstConstants/GA_ReadOnly)
;            ;^Driver drv       (.GetDriver ds)
;            ;rx                (.getRasterXSize ds)
;            ;ry                (.getRasterYSize ds)
;            ;^Band band        (.GetRasterBand ds 1)
;            ;^doubles minMax   (double-array 2)
;            ;_                 (.ComputeRasterMinMax band minMax)
;            ;r-min             (aget minMax 0)
;            ;r-max             (aget minMax 1)
;
;            sum-start         {:min Float/MAX_VALUE :max Float/MIN_VALUE}
;            sum-fn            (fn [sum next]
;                                {:min (min-fn (:min sum) next) :max (max (:max sum) next)})]
;            ;no-data-arr (into-array (make-array Double/TYPE 1)) ;; a hack to make a [Ljava.lang.Double type
;            ;_ (.GetNoDataValue band no-data-arr) ;; requires a [Ljava.lang.Double not a [D
;            ;no-data-val (aget no-data-arr 0)
;
;
;        ;(println "GDAL Ver. " (gdal/VersionInfo))
;        ;(println (str "Driver: " (.getShortName drv) "/" (.getLongName drv)))
;
;        (println (str "Width: " rx ", Height:" ry "\nMin: " r-min ", Max: " r-max))
;        ;(println (str "DataType: " (getDataType (.getDataType band)) "\n"
;        ;           "NoData Value: " no-data-val))
;        (println "Slice?" slice)
;
;        (for [xs (range slices)
;              ys (range slices)]
;          (let [x (get-slice-size rx xs slices)
;                xi (get-slice-offset rx xs slices)
;                ;_ (println "x: " x ", xi: " xi)
;                y (get-slice-size ry ys slices)
;                yi (get-slice-offset ry ys slices)
;                ;_ (println "y: " y ", yi: " yi)
;                hmap (make-array Float/TYPE x y)]
;            (println "Reading raster slice [" xs "," ys "], width: " x ", height:" y)
;            (doseq [^int i (range x)]
;              (let [^floats arr (float-array y)
;                    xii (- x 1 i)] ; flip the order of columns (workaround for StlObject)
;                (.ReadRaster band (+ i xi) yi 1 y arr)
;                (aset hmap xii arr)))
;            (print "Parsing slice ")
;            (let [sum (reduce sum-fn sum-start
;                              (for [i (range x)
;                                    j (range y)]
;                                (let [v (float (+ z-lift (* (aget hmap i j) multiplier 0.01)))
;                                      v' (if (> v r-max) 0.0 v) ; fix for nodata values that are Float/MAX_VALUE
;                                      v' (float (max 0.0 v'))]
;                                  ;; use the zeroed value for the build
;                                  (aset hmap i j v')
;                                  (when (= (mod i 10) j 0)
;                                    (print ".") (flush))
;                                  v')))]
;              (println "\nMin: " (:min sum) ", Max: " (:max sum)))
;            (print "Saving STL: ")
;            (let [name (if (.contains name ".")
;                         (.substring name 0 (.lastIndexOf name "."))
;                         name)
;                  name (str name "-" xs "-" ys ".stl")
;                  fname (str path File/separator name)
;                  _ (println fname)
;                  stl (StlObject/fromHeightmap name y x hmap)]
;              (set! (. stl -path) fname)
;              (.save stl StlObject/FILE_BINARY))))))))
