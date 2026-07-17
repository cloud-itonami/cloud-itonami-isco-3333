(ns placementops.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [placementops.actor :as actor]
            [placementops.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-candidate! st {:candidate-id "C-1" :name "Kobo Tanaka" :verified? true})
    (store/register-employer! st {:employer-id "E-1" :name "Kobo Manufacturing" :verified? true})
    st))

(deftest commits-a-verified-candidate-log-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :log-candidate-record :stake :low :candidate-id "C-1"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "C-1"))))))

(deftest commits-a-verified-low-stake-placement-match
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :propose-placement-match :stake :low
                 :candidate-id "C-1" :employer-id "E-1"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "C-1"))))))

(deftest holds-an-unverified-candidate-proposal
  (let [st (fresh-store)]
    (store/register-candidate! st {:candidate-id "C-2" :name "Unverified" :verified? false})
    (let [graph (actor/build-graph {:store st})
          request {:op :propose-placement-match :stake :low
                   :candidate-id "C-2" :employer-id "E-1"}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "C-2"))))))

(deftest holds-an-op-outside-the-closed-allowlist
  (testing "no path through this actor can finalize a hiring decision"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:op :finalize-hiring-decision :stake :low
                   :candidate-id "C-1" :employer-id "E-1"}
          result (actor/run-request! graph request {} "thread-4")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "C-1"))))))

(deftest interrupts-then-approves-high-stake-placement-match-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :propose-placement-match :stake :high
                 :candidate-id "C-1" :employer-id "E-1"}
        interrupted (actor/run-request! graph request {} "thread-5")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "C-1")))
    (let [resumed (actor/approve! graph "thread-5")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "C-1")))))))

(deftest interrupts-then-approves-compliance-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :flag-compliance-concern :stake :low
                 :candidate-id "C-1" :employer-id "E-1"}
        interrupted (actor/run-request! graph request {} "thread-6")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "C-1")))
    (let [resumed (actor/approve! graph "thread-6")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "C-1")))))))
