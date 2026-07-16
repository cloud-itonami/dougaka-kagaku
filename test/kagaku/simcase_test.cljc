(ns kagaku.simcase-test
  (:require [clojure.test :refer [deftest is testing]]
            [kagaku.simcase :as simcase]))

(def tidal-case
  {:id :moon-tidal
   :solver {:kind :tidal-scaling}
   :domain {:distance-ratio 0.5}
   :outputs {:tidal-force-ratio {:unit "ratio"}}})

(deftest valid-case-passes
  (is (:valid? (simcase/validate-case tidal-case))))

(deftest unknown-solver-fails
  (testing "solver kind は kotoba-lang sim スタックの既知 kind のみ"
    (let [{:keys [valid? errors]}
          (simcase/validate-case (assoc-in tidal-case [:solver :kind] :magic))]
      (is (not valid?))
      (is (some #(= :case/solver-unknown (first %)) errors)))))

(deftest budget-guard
  (let [{:keys [valid? errors]}
        (simcase/validate-case (assoc tidal-case :budget {:cells 5000000}))]
    (is (not valid?))
    (is (some #(= :case/budget-exceeded (first %)) errors))))

(deftest duplicate-case-ids-fail
  (is (not (:valid? (simcase/validate-cases [tidal-case tidal-case])))))

(def claims
  [{:id :tidal-ratio :text "潮汐力は8倍"
    :value {:quantity 8.0 :unit "ratio"}
    :source {:kind :sim :case :moon-tidal :output :tidal-force-ratio}}])

(deftest consistency-pass
  (let [results {:moon-tidal {:outputs {:tidal-force-ratio
                                        {:quantity 8.0 :unit "ratio"}}}}
        rows (simcase/claim-consistency claims results)]
    (is (every? :ok? rows))))

(deftest consistency-value-mismatch
  (testing "台本の数値が sim 結果とずれていたら fail（AI の堂々とした計算間違いを弾く）"
    (let [results {:moon-tidal {:outputs {:tidal-force-ratio
                                          {:quantity 4.0 :unit "ratio"}}}}
          [row] (simcase/claim-consistency claims results)]
      (is (not (:ok? row)))
      (is (= :value-mismatch (:reason row))))))

(deftest consistency-unit-mismatch
  (let [results {:moon-tidal {:outputs {:tidal-force-ratio
                                        {:quantity 8.0 :unit "N"}}}}
        [row] (simcase/claim-consistency claims results)]
    (is (not (:ok? row)))
    (is (= :unit-mismatch (:reason row)))))

(deftest consistency-missing-result
  (let [[row] (simcase/claim-consistency claims {})]
    (is (not (:ok? row)))
    (is (= :result-missing (:reason row)))))

(deftest bench-datoms-shape
  (let [results {:moon-tidal {:outputs {:tidal-force-ratio {:quantity 8.0 :unit "ratio"}}
                              :bench {:wall-ms 12 :runtime :nbb}}}
        [d] (simcase/bench-datoms :ep-001 [tidal-case] results)]
    (is (= :tidal-scaling (:kagaku.bench/solver d)))
    (is (= 12 (:kagaku.bench/wall-ms d)))
    (is (= :nbb (:kagaku.bench/runtime d)))))
