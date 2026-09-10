(ns placementops.governor
  "PlacementOpsGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  placement-coordination operation an advisor may propose. The
  governor never dispatches hardware itself and NEVER lets a proposal
  finalize a hiring decision, a candidate-rejection decision, or any
  placement decision that could constitute discriminatory screening
  — those are permanently out of scope for this actor, not merely
  gated. Modeled on cloud-itonami-isco-3313's bookkeeping-style
  governor, with a closed proposal-op allowlist and a content-based
  scope-exclusion check added for this vertical's anti-discrimination
  guardrail.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. candidate provenance    — the candidate record must be
                                independently registered AND verified
                                before any proposal referencing it can
                                commit or escalate. Never trusts the
                                proposal's own claim.
    2. employer provenance     — same, for any proposal referencing an
                                employer.
    3. no-actuation            — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never itself performs a placement
                                action; it only gates what the advisor
                                may commit).
    4. closed op allowlist     — the proposal's :op must be one of the
                                four ops this actor is scoped to
                                (`closed-op-allowlist` below). Any
                                other op — including anything that
                                would finalize a hiring or rejection
                                decision — is a HARD, PERMANENT block.
    5. scope exclusion         — independent of the op-allowlist check,
                                any proposal whose rationale/content
                                names a finalization/execution action
                                for a hiring, rejection or other
                                placement decision (`scope-excluded-
                                terms` below) is a HARD, PERMANENT
                                block, evaluated unconditionally. This
                                actor never finalizes those decisions —
                                it only coordinates candidate-to-
                                employer matching.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off — these
  are :high/:safety-critical regardless of confidence):
    6. :op :flag-compliance-concern (surfacing a discrimination/
                                screening-bias concern ALWAYS requires
                                human review — it is never auto-
                                resolved and never appears in any
                                phase's auto-commit set).
    7. a :propose-placement-match proposal at :stake :high (a
                                high-stake candidate-to-employer match
                                always needs human sign-off, regardless
                                of confidence).
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [placementops.store :as store]))

(def confidence-floor 0.6)

;; The closed proposal-op allowlist. This governor NEVER allows any op
;; outside this set to commit or even escalate — an op outside this
;; set is a HARD, permanent block (see `hard-violations` :op-not-allowed
;; below), not merely un-auto-committable. In particular, no op here
;; can finalize a hiring or rejection decision.
(def closed-op-allowlist
  #{:log-candidate-record :propose-placement-match
    :flag-compliance-concern :coordinate-interview-schedule})

;; :flag-compliance-concern always escalates to a human — never
;; auto-commit-eligible at any phase.
(def ^:private always-escalate-ops #{:flag-compliance-concern})

;; Scope-exclusion terms, phrased as the FINALIZATION/EXECUTION ACTION
;; (never a bare noun like "hiring" or "rejection") — a known
;; self-tripping bug class in this fleet: a bare-noun term list can
;; accidentally match inside the mock advisor's own default rationale
;; text for a legitimate proposal (this advisor's default rationale is
;; "drafted <op> for candidate <id>[, employer <id>]", which never
;; contains any of these full action phrases). See
;; `placementops.governor-test/
;; default-mock-advisor-proposals-never-self-trip-scope-exclusion`.
(def scope-excluded-terms
  ["finalize the hiring decision" "finalized the hiring decision"
   "finalize the rejection decision" "finalized the rejection decision"
   "finalize the candidate rejection" "finalized the candidate rejection"
   "approve the hiring decision" "approved the hiring decision"
   "deny the candidate application" "denied the candidate application"
   "reject the candidate application" "rejected the candidate application"
   "confirm the hiring decision" "confirmed the hiring decision"
   "execute the hiring decision" "executed the hiring decision"
   "issue the final hiring decision" "issued the final hiring decision"
   "finalize the candidate screening decision" "finalized the candidate screening decision"
   "screen out the candidate" "screened out the candidate"
   "採用決定を確定" "採用を確定した"
   "不採用決定を確定" "不採用を確定した"
   "候補者の応募を却下した" "候補者の応募を拒否した"])

(defn out-of-scope?
  "True if any string field on `proposal` (currently just :rationale)
  contains a scope-excluded finalization/execution phrase."
  [proposal]
  (let [text (str (:rationale proposal))]
    (boolean (some #(str/includes? text %) scope-excluded-terms))))

(defn- hard-violations [{:keys [proposal]} candidate-record employer-record]
  (let [{:keys [op candidate-id employer-id]} proposal]
    (cond-> []
      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は placement 判断を直接実行しない）"})

      (not (contains? closed-op-allowlist op))
      (conj {:rule :op-not-allowed :detail "closed allowlist 外の op（採用/不採用/選考の直接確定を含む一切の finalize は許可されない）"})

      (and candidate-id (nil? candidate-record))
      (conj {:rule :unknown-candidate :detail "未登録 candidate への提案は不可"})

      (and candidate-id candidate-record (not (:verified? candidate-record)))
      (conj {:rule :candidate-unverified :detail "未検証 candidate への提案は不可（登録のみでは不十分）"})

      (and employer-id (nil? employer-record))
      (conj {:rule :unknown-employer :detail "未登録 employer への提案は不可"})

      (and employer-id employer-record (not (:verified? employer-record)))
      (conj {:rule :employer-unverified :detail "未検証 employer への提案は不可（登録のみでは不十分）"})

      (out-of-scope? proposal)
      (conj {:rule :scope-excluded
             :detail "採用/不採用/選考の最終決定を直接確定する提案は恒久的に許可されない（このactorはマッチングの調整のみを行う）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `placementops.store/Store`. Pure — never
  mutates the store, never finalizes a hiring/rejection/screening
  decision."
  [_request _context proposal store]
  (let [candidate-record (some->> (:candidate-id proposal) (store/candidate store))
        employer-record (some->> (:employer-id proposal) (store/employer store))
        hard (hard-violations {:proposal proposal} candidate-record employer-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        high-stake-match? (and (= :propose-placement-match (:op proposal))
                                (= :high (:stake proposal)))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not high-stake-match?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? high-stake-match?))}))
