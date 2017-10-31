(ns hellogeotiff
  (:gen-class)
  ;(:import [org.geotools.data CachingFeatureSource FeatureSource FileDataStore FileDataStoreFinder])
  (:import
    ;[java.io File]
    ;[java.awt.image BufferedImage]
    ;[java.awt.image Raster]
    ;[javax.imageio ImageIO ImageReader]
    [com.heightmap.stl StlObject ModelObject]
    [org.gdal.gdalconst gdalconstConstants]
    [org.gdal.gdal gdal Dataset Driver Band])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn reader-init []

  (try (gdal/AllRegister)
       (catch Exception ex (println "Is GDAL installed and working?\n" (.getMessage ex))))

  ;(doseq [suffix (ImageIO/getReaderFileSuffixes)]
  ;  (println "suffix:" suffix))
  ;
  ;(let [readers (ImageIO/getImageReadersByFormatName "TIFF")]
  ;  (while (. readers hasNext)
  ;    (println "reader: " (. readers next))))
  )

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

(defn min-fn
  ([x y]
   (if (> y 0)
     (min x y)
     (if (> x 0)
       x
       Float/MAX_VALUE)))
  ([x y & more]
   (reduce min-fn (min-fn x y) more)))

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
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (or (< (count arguments) 1)
        (not (.exists (io/as-file (first arguments)))))
      {:exit-message (str (usage summary) \newline \newline "Where's the GeoTIFF file?")}
      (and (= 1 (count arguments))
        (.exists (io/as-file (first arguments))))
      {:filename (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (println "EXITING" status)
  ;(System/exit status)
  )


(defn -main
  "The Main."
  [& args]

  (reader-init)

  (let [{:keys [filename options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [name (.getName (io/as-file filename))
            ^float multiplier (:multiplier options)
            ^float z-lift (:z-lift options)
            ^Dataset ds (gdal/Open filename gdalconstConstants/GA_ReadOnly)
            ^Driver drv (.GetDriver ds)
            x (.getRasterXSize ds)
            y (.getRasterYSize ds)
            hmap (make-array Float/TYPE x y)
            ^Band band (.GetRasterBand ds 1)
            minMax (double-array 2)
            _ (.ComputeRasterMinMax band minMax)
            sum-start {:min Float/MAX_VALUE :max Float/MIN_VALUE}
            sum-fn (fn [sum next]
                     {:min (min-fn (:min sum) next) :max (max (:max sum) next)})]

        (println "GDAL Ver. " (gdal/VersionInfo))
        (println (str "Driver: " (.getShortName drv) "/" (.getLongName drv)))
        (println (str "Width: " (.getRasterXSize ds) ", Height:" (.getRasterYSize ds)
                   "\nMin: " (aget minMax 0) ", Max: " (aget minMax 1)))
        (println (str "DataType: " (getDataType (.getDataType band))))

        (println "Reading raster.")
        (doseq [^int i (range x)]
          (let [^floats arr (float-array y)]
            (.ReadRaster band i 0 1 y arr)
            (aset hmap i arr)))
        (println "Done reading raster.  \nParsing data.")
        (let [sum (reduce sum-fn sum-start
                    (for [i (range x)
                          j (range y)]
                      (let [v (float (+ z-lift (* (aget hmap i j) multiplier 0.01)))]
                        (when (= (mod i 10) j 0)
                          (print ".") (flush))
                        (aset hmap i j v)
                        v)))]
          (println "Done parsing: Modified Min: " (:min sum) ", Max: " (:max sum)))
        (println "Saving STL")
        (let [stl (StlObject/fromHeightmap name y x hmap)
              name (if (.contains name ".")
                     (.substring name 0 (.lastIndexOf name "."))
                     name)]
          (set! (. stl -path) (str name ".stl"))
          (.save stl StlObject/FILE_BINARY))
        )))

  ;(let [{:keys [action options exit-message ok?]} (validate-args args)]
  ;  (if exit-message
  ;    (exit (if ok? 0 1) exit-message)
  ;    (let [^String filename action
  ;          ;_ (println "OPTIONS " options)
  ;          multiplier (:multiplier options)
  ;          z-lift (:z-lift options)
  ;          ^File file (File. filename)
  ;          name (.getName file) ;
  ;          ^BufferedImage image (try
  ;                                 (ImageIO/read file)
  ;                                 (catch Exception ex (exit 1 ex)))
  ;          width (.getWidth image)
  ;          _ (println "width" width)
  ;          height (.getHeight image)
  ;          _ (println "height" height)
  ;          ^Raster raster (.getData image)
  ;          hmap (make-array Float/TYPE width height)
  ;          summary {:min Float/MAX_VALUE :max Float/MIN_VALUE}
  ;          sum-fn (fn [sum next]
  ;                   (let [[x y] next
  ;                         v (.getSample raster x y 0)
  ;                         v (+ z-lift (* multiplier (/ v 100.0)))
  ;                         min (min-fn (:min sum) v)
  ;                         max (max (:max sum) v)
  ;                         ]
  ;                     ;(aset-float hmap (- (- width 1) x) y v)
  ;                     ;(when (= (mod x 10) 0)
  ;                     ;  (println x y v))
  ;                     {:min min :max max}))]
  ;
  ;      (println "Calculating samples and min/max (z-lift: " z-lift ", mult: " multiplier ")")
  ;
  ;      (println "RESULTS"
  ;        (reduce sum-fn summary (for [w (range width) h (range height)] [w h])))
  ;
  ;
  ;      ;
  ;      ;(let [keys (vec (for [w (range width) h (range height)]
  ;      ;                  [w h]))
  ;      ;      samples (vec (for [[w h] keys]
  ;      ;                     (let [s (.getSample raster w h 0)]
  ;      ;                       (if (> s 0)
  ;      ;                         (+ z-lift (* multiplier (/ s 100.0)))
  ;      ;                         0))))
  ;      ;      ;_ (println "samples " samples)
  ;      ;      min (reduce min-fn samples)
  ;      ;      max (reduce max samples)]
  ;      ;  (println "min: " min "max: " max)
  ;      ;  (when (> min 10)
  ;      ;    (println "min is over 10, consider a lower z-lift value"))
  ;      ;  (println "Converting to java array")
  ;      ;  (doseq [i (range (count samples))]
  ;      ;    (let [[w h] (get keys i)
  ;      ;          sample (get samples i)]
  ;      ;      (aset-float hmap (- (- width 1) w) h
  ;      ;        sample))))
  ;      ;
  ;      ;(println "Saving STL")
  ;      ;(let [stl (StlObject/fromHeightmap name height width hmap)
  ;      ;      name (if (.contains name ".")
  ;      ;             (.substring name 0 (.lastIndexOf name "."))
  ;      ;             name)]
  ;      ;  (set! (. stl -path) (str name ".stl"))
  ;      ;  (.save stl StlObject/FILE_BINARY))
  ;
  ;      ;(println "count loop-recur" (time (count (points-loop width height hmap))))
  ;      ;(println "count for-concat" (time (count (points-concat width height hmap))))
  ;      ;(println "count for-array" (time (count (points-array width height hmap))))
  ;
  ;      )))

  )
