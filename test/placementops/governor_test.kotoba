(ns placementops.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [placementops.store :as store]
            [placementops.advisor :as advisor]
            [placementops.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-candidate! st {:candidate-id "C-1" :name "Kobo Tanaka" :verified? true})
    (store/register-employer! st {:employer-id "E-1" :name "Kobo Manufacturing" :verified? true})
    st))

(defn- match-op [stake]
  {:op :propose-placement-match :effect :propose :candidate-id "C-1" :employer-id "E-1"
   :stake stake :confidence 0.9 :rationale "drafted candidate-employer match"})

(deftest ok-verified-candidate-and-employer-match
  (let [st (fresh-store)
        v (governor/check {} {} (match-op :low) st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-log-candidate-record-references-only-candidate
  (let [st (fresh-store)
        v (governor/check {} {} {:op :log-candidate-record :effect :propose
                                 :candidate-id "C-1" :stake :low :confidence 0.9
                                 :rationale "drafted log-candidate-record for candidate C-1"} st)]
    (is (:ok? v))))

(deftest hard-on-unregistered-candidate
  (let [st (fresh-store)
        v (governor/check {} {} (assoc (match-op :low) :candidate-id "ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-candidate (:rule %)) (:violations v)))))

(deftest hard-on-unverified-candidate
  (let [st (fresh-store)]
    (store/register-candidate! st {:candidate-id "C-2" :name "Unverified" :verified? false})
    (let [v (governor/check {} {} (assoc (match-op :low) :candidate-id "C-2") st)]
      (is (:hard? v))
      (is (some #(= :candidate-unverified (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-employer
  (let [st (fresh-store)
        v (governor/check {} {} (assoc (match-op :low) :employer-id "ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-employer (:rule %)) (:violations v)))))

(deftest hard-on-unverified-employer
  (let [st (fresh-store)]
    (store/register-employer! st {:employer-id "E-2" :name "Unverified" :verified? false})
    (let [v (governor/check {} {} (assoc (match-op :low) :employer-id "E-2") st)]
      (is (:hard? v))
      (is (some #(= :employer-unverified (:rule %)) (:violations v))))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check {} {} (assoc (match-op :low) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-not-allowed
  (testing "an op outside the closed four-op allowlist — including anything
            that would directly finalize a hiring/rejection decision — is a
            permanent HARD block"
    (let [st (fresh-store)
          v (governor/check {} {} (assoc (match-op :low) :op :finalize-hiring-decision) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowed (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale
  (testing "a proposal on an otherwise-allowed op whose rationale names a
            finalization/execution action for a hiring or rejection decision
            is a permanent HARD block, independent of the op-allowlist check"
    (let [st (fresh-store)
          v (governor/check {} {} (assoc (match-op :low)
                                          :rationale "finalized the hiring decision for candidate C-1")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rejection-rationale
  (let [st (fresh-store)
        v (governor/check {} {} (assoc (match-op :low)
                                        :rationale "rejected the candidate application for C-1")
                           st)]
    (is (:hard? v))
    (is (some #(= :scope-excluded (:rule %)) (:violations v)))))

(deftest always-escalates-flag-compliance-concern-even-at-high-confidence
  (testing "surfacing a discrimination/screening-bias concern always requires
            human review — never auto-resolved"
    (let [st (fresh-store)
          v (governor/check {} {} {:op :flag-compliance-concern :effect :propose
                                   :candidate-id "C-1" :employer-id "E-1"
                                   :confidence 0.99 :stake :low
                                   :rationale "drafted flag-compliance-concern for candidate C-1"}
                             st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-high-stake-placement-match
  (testing "a high-stake candidate-to-employer match always needs human sign-off"
    (let [st (fresh-store)
          v (governor/check {} {} (assoc (match-op :high) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check {} {} (assoc (match-op :low) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (testing "the default mock advisor's own rationale text for every op in the
            closed allowlist never contains a scope-excluded finalization/
            execution phrase (fleet-known self-trip bug class regression)"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          requests [{:op :log-candidate-record :candidate-id "C-1" :stake :low}
                    {:op :propose-placement-match :candidate-id "C-1" :employer-id "E-1" :stake :low}
                    {:op :flag-compliance-concern :candidate-id "C-1" :employer-id "E-1" :stake :low}
                    {:op :coordinate-interview-schedule :candidate-id "C-1" :employer-id "E-1" :stake :low}]]
      (doseq [req requests]
        (let [proposal (advisor/-advise adv st req)]
          (is (false? (governor/out-of-scope? proposal))
              (str "op " (:op req) " self-tripped scope-exclusion: " (:rationale proposal)))
          (let [v (governor/check {} {} proposal st)]
            (is (not (contains? (set (map :rule (:violations v))) :scope-excluded))
                (str "op " (:op req) " tripped :scope-excluded in governor/check"))))))))
