(ns optcase.core
  (:gen-class :main true)
  (:refer-clojure :exclude [use import])
  (:require [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]
            [clojure.math.numeric-tower :refer :all]))

;;;;;;;;
;TRANSLATION OF PRIMARY FUNCTIONS OF ATTACH MODULE
;;;;;;;;

(def defaulDrawingResolution 6) ;low res for vector arrows

(defn- sqr [x] ;square x
  (expt x 2))

(defn modofvec [[x y z]] ;modulus of vecotr

	(sqrt (+ (sqr x ) (sqr y) (sqr z) )))

(defn cross [[x1 y1 z1] [x2 y2 z2]] ;cross product
	(vector
		(- (* y1 z2) (* z1 y2))
  		(- (* z1 x2) (* x1 z2))
  		(- (* x1 y2) (* y1 x2))
  		))

(defn dot [[x1 y1 z1] [x2 y2 z2]] ;dot product
	 (+ (* x1 x2) (* y1 y2) (* z1 z2)))

(defn unitv [[x y z]] ;unit vecotr
	(vector 
		(/ x (modofvec [x y z]) )
		(/ y (modofvec [x y z]) ) 
		(/ z (modofvec [x y z]) )))


(defn anglev [[x1 y1 z1] [x2 y2 z2]] ;angle between vectors in radians
	(Math/acos (/ (dot [x1 y1 z1] [x2 y2 z2]) (* (modofvec [x1 y1 z1]) (modofvec [x2 y2 z2])))))

(defn point [p] ;make a small sphere at location p
	(->>(sphere 0.7)
		(translate p)
		(with-fn defaulDrawingResolution)))

(defn vectorz [l l_arrow mark]  ;make a vector in the z direction of length l, arrow length l_arrow, mark (true or false) to show angle
	(let [lb 	(- l l_arrow)]
		(union
		(translate [0 0 (/ lb 2)] 
			(union

				(->>(cylinder [1 0.2] l_arrow);draw tye arrow
					(translate [0 0 (/ lb 2)])
					(with-fn defaulDrawingResolution))

				(if mark
					(->>
						(cube 2 0.3 (* 0.8 l_arrow) )
						(translate [1 0 0])
						(translate [0 0 (+ (/ lb 2))])
						)
					)

				(->> (cylinder 0.5 lb)
					 (with-fn defaulDrawingResolution))
				)
			)
		(->> (sphere 1)
			 (with-fn defaulDrawingResolution))
		))

	)

(defn orientate 
	([v shape] (orientate v [0 0 1] 0 shape)) ;for default values

	([v vref roll shape]
	(let [
		raxis 		(cross vref v) 
		ang 		(anglev vref v)]

	(->> shape
		(rotate ang raxis)
		(rotate roll v)
		)
	)))

(defn drawingvector [v l l_arrow mark]
	(->>(vectorz l l_arrow mark)
		(orientate v)
		))
	
(defn connector [[u v ang]] ;u is position, v is vector, ang is rotation around vector
	
	(union
		(->> (point u)
			(color [0.5 1 1 1]))

		(->> (drawingvector v 8 2 true)
			(color [0.5 1 1 1])
			(rotate ang v)
			(translate u)
			)

		))

(defn attach [mainpart seconpart shape]
	(let [ ;get data from parts
		pos1 		(first mainpart)
		v			(second mainpart)
		roll 		(nth mainpart 2)

		pos2 		(first seconpart)
		vref		(second seconpart)

		; calculation of the roll axis
		raxis 		(cross vref v)

		;calculate the angle between the vectors
		ang 		(anglev vref v)
		]

	(->> shape
		(translate (map #(- %1) pos2))
		(rotate ang raxis)
		(rotate roll v)
		(translate pos1)
		)

	))

(def a1 [[0 0 0] [1 0 0] 0])
(def c1 [[5 5 5] [0 0 1] 2])
(def testshapes (union (sphere 3) (cube 2 10 2) (cube 10 2 2)))

(defn testmessing [] (union
	(connector a1)
	(connector c1)

	; (->> (cube 10 10 10)
	; 	 (color [0.6 0.8 0.2 0.5]))

	testshapes

	(attach c1 a1 testshapes)

	))

;;;;;;;;;;;;
;GETTING THE PLATE MATRIX SET UP
;;;;;;;;;;;;
; x is the internal
; y is the external
; each is a map: 
; {
; :cpnt [[1 2 3] [0 0 1] 0]
; :xcoord		3
; :ycoord 	3
; }


; [
; [ 0 1 2 3 4 5]
; [ 0 1 2 3 4 5]
; [ 0 1 2 3 4 5]
; [ 0 1 2 3 4 5]
; ]

(def keywidthForSpacing 	14.00)
(def keySpacing 			5.05)
(def arrXWid				7 )
(def arrYLen				10 )

(defn createarray [x y] ;x is across, y is down
	(vec(for [ycoin (range y)]
		(vec (for [xcoin (range x)]
			{:xcoord xcoin, 
			 :ycoord ycoin,
			 :cpntPos [ (* xcoin (+ keySpacing keywidthForSpacing)) (* ycoin (+ keySpacing keywidthForSpacing)) 0], 
			 :cpntVec [0 0 1],
			 :cpntAng 0}
			)
		))
	))


(defn retr [arr x y]
	((arr y) x)
)

(defn putsquareinarr [arr & more]
	(for [ycoin (range arrYLen) xcoin (range arrXWid)]

		(let [
			pntData 	(retr arr xcoin ycoin)
			cpntP 		(:cpntPos pntData)
			cpntV 		(:cpntVec pntData)
			cpntA 		(:cpntAng pntData)

			]
			;(println pntData)
			
			(attach [cpntP cpntV cpntA] [[0 0 0] [0 0 1] 0] (difference (cube 16 16 0.5) (cube 14 14 1)))
			
		)
	))

(defn moveonXYplane [arr & more]
	(vec(for [ycoin (range arrYLen)]
		(vec (for [xcoin (range arrXWid)]
			(let [
				pntData (retr arr xcoin ycoin)
				xval		(:xcoord pntData)
				yval  		(:ycoord pntData)
				cpntP 		(:cpntPos pntData)
				cpntV 		(:cpntVec pntData)
				cpntA 		(:cpntAng pntData)
				]
				

				{:xcoord xval, 
				 :ycoord yval,
				 :cpntPos [ (+ (cpntP 0) 5) (+ (cpntP 1) 5) (+ (cpntP 2) 5)], 
				 :cpntVec cpntV,
				 :cpntAng cpntA}
				

				)

			)
		))
	)
	)



;;;;;;;;;;;;;;;;
;; SA Keycaps ;;
;;;;;;;;;;;;;;;;

(def sa-length 18.25)
(def sa-double-length 37.5)
(def sa-cap {1 (let [bl2 (/ 18.5 2)
                     m (/ 17 2)
                     key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 6]))
                                   (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 12])))]
                 (->> key-cap
                      (translate [0 0 (+ 5 plate-thickness)])
                      (color [220/255 163/255 163/255 1])))
             2 (let [bl2 (/ sa-double-length 2)
                     bw2 (/ 18.25 2)
                     key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 12])))]
                 (->> key-cap
                      (translate [0 0 (+ 5 plate-thickness)])
                      (color [127/255 159/255 127/255 1])))
             1.5 (let [bl2 (/ 18.25 2)
                       bw2 (/ 28 2)
                       key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 0.05]))
                                     (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 12])))]
                   (->> key-cap
                        (translate [0 0 (+ 5 plate-thickness)])
                        (color [240/255 223/255 175/255 1])))})

(defn showkeycaps [arr]
		(for [ycoin (range arrYLen) xcoin (range arrXWid)]
			(let [
			pntData 	(retr arr xcoin ycoin)
			cpntP 		(:cpntPos pntData)
			cpntV 		(:cpntVec pntData)
			cpntA 		(:cpntAng pntData)
				]

			(attach [cpntP cpntV cpntA]
					[[0 0 0] [0 0 1] 0]
					(sa-cap 1)
				)

	)))

;
;Making web
;

(def mount-hole-width 14)
(def mount-hole-height 14)
(def plate-thickness 4)

(def web-thickness 3.5)
(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))

(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-hole-width  2) post-adj) (- (/ mount-hole-height  2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-hole-width -2) post-adj) (- (/ mount-hole-height  2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-hole-width -2) post-adj) (+ (/ mount-hole-height -2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-hole-width  2) post-adj) (+ (/ mount-hole-height -2) post-adj) 0] web-post))

(defn triangle-hulls [& shapes]
	"TBH I didn't write this. Adereth did. Its just a nice hulling function that makes 
	multiple hulls instead of one big hull. I guess hulls in sets of three shapes as 
	three points will always form a plane. This way the hulls will always be planes (flat)"
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))


(defn getcolPntData [arr x y]
	"Similar to getrowPntData but this deals with x. See getrowPntData and smartretrPntData"
	(if (<= 0 x (dec arrXWid) )

			(retr arr x y)

			(if (= x arrXWid)
				(update-in (retr arr (dec x) y) [:cpntPos 0] + mount-hole-height rightedgepadding)
				(if (= x -1)
					(update-in (retr arr (inc x) y) [:cpntPos 0] - mount-hole-height leftedgepadding)
					)
				)	
		)
	)

(defn getrowPntData [arr x y]
	"Retrieves the pntData at x y in arr and makes sure that y is in the correct range. See smartretrPntData 
	for what it does when y is out of bounds. This only deals with y as it gets getcolPntData to deal with x."
	(if (<= 0 y (dec arrYLen) )
			(getcolPntData arr x y) 

			(if (= y arrYLen)
				(update-in (getcolPntData arr x (dec y)) [:cpntPos 1] + mount-hole-height topedgepadding)
				(if (= y -1)
					(update-in (getcolPntData arr x (inc y)) [:cpntPos 1] - mount-hole-height bottedgepadding ) 
					)
				)	
		)
	)

(defn smartretrPntData [arr x y]
	"This is used to make the edges of the plate. Because the webbing loops through -1
	to arrXWid (or arrYLen), you need to be careful not to go outside of the array.
	this function catches when you are at -1 or arrYLen, and returns the closest good position
	+ edge padding. For instance if you call the array with arrXWid, this will find arrXWid - 1,
	then it will return arrXWid -1 with an updated x pos. This update will include edge padding.

	getrowPntData is used because inside of getrowPntData it calls getcolPntData. You can't also 
	call getrowPntData inside getcolPntData because then you will get an infinite loop."
	(getrowPntData arr x y)
	)


(def leftedgepadding 3)
(def rightedgepadding 3)
(def topedgepadding 3)
(def bottedgepadding 3)

(defn makeconnectors [arr] 
	(apply union
		(concat
			;Row connectors

			(for [ycoin (range arrYLen)
				  xcoin (range -1 arrXWid)] 

				  (let [
					pntData 	(smartretrPntData arr xcoin ycoin)
					cpntP 		(:cpntPos pntData)
					cpntV 		(:cpntVec pntData)
					cpntA 		(:cpntAng pntData)
					nextColPntData (smartretrPntData arr (inc xcoin) ycoin)
					cpntPNxCol  (:cpntPos nextColPntData)
					cpntVNxCol  (:cpntVec nextColPntData)
					cpntANxCol 	(:cpntAng nextColPntData)
					] (color [1 (rand) 1 1] (triangle-hulls
						(attach 
							[cpntPNxCol cpntVNxCol cpntANxCol] 
							[[0 0 0] [0 0 1] 0] 
							web-post-tl)
						(attach 
							[cpntP cpntV cpntA] 
							[[0 0 0] [0 0 1] 0] 
							web-post-tr)
						(attach 
							[cpntPNxCol cpntVNxCol cpntANxCol] 
							[[0 0 0] [0 0 1] 0] 
							web-post-bl)
						(attach 
							[cpntP cpntV cpntA] 
							[[0 0 0] [0 0 1] 0] 
							web-post-br)
				))))

			;columns connecting
			(for [ycoin (range -1 arrYLen)
				  xcoin (range arrXWid)] 

				  (let [
					pntData 	(smartretrPntData arr xcoin ycoin)
					cpntP 		(:cpntPos pntData)
					cpntV 		(:cpntVec pntData)
					cpntA 		(:cpntAng pntData)
					nextRowPntData (smartretrPntData arr xcoin (inc ycoin))
					cpntPNxRow  (:cpntPos nextRowPntData)
					cpntVNxRow  (:cpntVec nextRowPntData)
					cpntANxRow 	(:cpntAng nextRowPntData)
					] (color [(rand) 1 1 1] (triangle-hulls
						(attach 
							[cpntP cpntV cpntA] 
							[[0 0 0] [0 0 1] 0] 
							web-post-tl)
						(attach 
							[cpntP cpntV cpntA] 
							[[0 0 0] [0 0 1] 0] 
							web-post-tr)
						(attach 
							[cpntPNxRow cpntVNxRow cpntANxRow] 
							[[0 0 0] [0 0 1] 0] 
							web-post-bl)
						(attach 
							[cpntPNxRow cpntVNxRow cpntANxRow] 
							[[0 0 0] [0 0 1] 0] 
							web-post-br)
				))))

			;diagonal connecting
			(for [ycoin (range -1  arrYLen)
				  xcoin (range -1  arrXWid)] 

				  (let [
					pntData 	(smartretrPntData arr xcoin ycoin)
					cpntP 		(:cpntPos pntData)
					cpntV 		(:cpntVec pntData)
					cpntA 		(:cpntAng pntData)
					nextRowPntData (smartretrPntData arr xcoin (inc ycoin))
					cpntPNxRow  (:cpntPos nextRowPntData)
					cpntVNxRow  (:cpntVec nextRowPntData)
					cpntANxRow 	(:cpntAng nextRowPntData)
					nextColPntData (smartretrPntData arr (inc xcoin) ycoin)
					cpntPNxCol  (:cpntPos nextColPntData)
					cpntVNxCol  (:cpntVec nextColPntData)
					cpntANxCol 	(:cpntAng nextColPntData)
					nextRowAndColPntData (smartretrPntData arr (inc xcoin) (inc ycoin))
					cpntPNxRowAndCol  (:cpntPos nextRowAndColPntData)
					cpntVNxRowAndCol  (:cpntVec nextRowAndColPntData)
					cpntANxRowAndCol  (:cpntAng nextRowAndColPntData)
					] (color [1 1 (rand) 1] (triangle-hulls
						(attach 
							[cpntP cpntV cpntA] 
							[[0 0 0] [0 0 1] 0] 
							web-post-tr)
						(attach 
							[cpntPNxCol cpntVNxCol cpntANxCol] 
							[[0 0 0] [0 0 1] 0] 
							web-post-tl)
						(attach 
							[cpntPNxRow cpntVNxRow cpntANxRow] 
							[[0 0 0] [0 0 1] 0] 
							web-post-br)
						(attach 
							[cpntPNxRowAndCol cpntVNxRowAndCol cpntANxRowAndCol] 
							[[0 0 0] [0 0 1] 0] 
							web-post-bl)

					)))
			)
	
		)))

(defn findnewvec [[x1 y1 z1] [x2 y2 z2]]
	[(- x2 x1) (- y2 y1) (- z2 z1)]
	)

(defn curveitbaby [arr]
	(vec(for [ycoin (range arrYLen)]
		(vec (for [xcoin (range arrXWid)]
			(let [
				pntData (retr arr xcoin ycoin)
				xval		(:xcoord pntData)
				yval  		(:ycoord pntData)
				cpntP 		(:cpntPos pntData)
				cpntV 		(:cpntVec pntData)
				cpntA 		(:cpntAng pntData)
				

				newPos 		[ (cpntP 0) (cpntP 1) (* (expt 1.2 (inc yval)) 20) ]
				focuspnt	[ (newPos 0) 0 150]

				newVec 		(findnewvec  [(newPos 0) (newPos 1) (newPos 2)] focuspnt )
				]
				

				{:xcoord xval, 
				 :ycoord yval,
				 :cpntPos newPos, 
				 :cpntVec newVec,
				 :cpntAng cpntA}

		)

	)))))

(defn transformationFunctions [arr & more]
	(-> (moveonXYplane arr) ;thread this one into others
		(curveitbaby)
		)
	)

(defn doesnttoucharrayFunctions [arr & more]
	(union 
		;(putsquareinarr arr)
		(makeconnectors arr)
		(showkeycaps arr)
		)
	)

(defn messingaround []
		 (-> (createarray arrXWid arrYLen)
		 	 (transformationFunctions)
		 	 (doesnttoucharrayFunctions) ;the outcome of this should be code for scad-clj
		 	 )

		 
	)

(spit "things/post-demo.scad"
      (write-scad (messingaround )))