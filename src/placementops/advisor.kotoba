(ns placementops.advisor
  "Placement Advisor — the advisor named in this repository's README,
  proposing an employment-placement-coordination operation (log a
  candidate record, propose a candidate-to-employer match, coordinate
  an interview schedule, or flag a compliance concern) from a
  candidate application, employer opening and matching policy.
  Swappable mock/llm; the advisor ONLY proposes —
  `placementops.governor` checks candidate/employer verification and
  scope independently and always escalates compliance concerns and
  high-stake matches. Modeled on cloud-itonami-isco-3313's advisor.

  A proposal: {:op :log-candidate-record|:propose-placement-match|
               :flag-compliance-concern|:coordinate-interview-schedule
               :effect :propose :candidate-id str :employer-id str
               :stake kw :confidence n :rationale str}

  The advisor never proposes finalizing a hiring or rejection decision
  — it is structurally out of scope for every op in the closed
  allowlist below (`placementops.governor/closed-op-allowlist`), and
  the rationale text this advisor emits never uses a finalization/
  execution phrase (`placementops.governor/scope-excluded-terms`), so
  the advisor's own DEFAULT proposals never self-trip the governor's
  scope-exclusion check (see `placementops.governor-test/
  default-mock-advisor-proposals-never-self-trip-scope-exclusion`)."
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake candidate-id employer-id] :as request}]
  {:op op
   :effect :propose
   :candidate-id candidate-id
   :employer-id employer-id
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "drafted " (name op) " for candidate " candidate-id
                   (when employer-id (str " and employer " employer-id)))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an employment-placement-coordination advisor. Given a
   request, propose an :op, the :candidate-id and (when relevant)
   :employer-id, an honest :confidence and a :stake. Never propose an
   op outside the closed four-op allowlist, and never propose finalizing
   a hiring decision, a candidate-rejection decision, or any placement
   decision that could constitute discriminatory screening — you only
   coordinate candidate-to-employer matching and interview scheduling;
   the governor independently verifies candidate/employer registration
   and always escalates compliance concerns and high-stake matches to a
   human regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
