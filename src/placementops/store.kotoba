(ns placementops.store
  "SSoT for the ISCO-08 3333 employment placement coordination actor
  (itonami actor pattern, ADR-2607121000 / CLAUDE.md Actors section;
  README's 'Robotics premise' — a candidate-intake and coordination
  robot performs application logging, match drafting and interview
  scheduling under this advisor/governor pair, which never dispatches
  hardware itself and never finalizes a hiring, rejection or other
  placement decision that could constitute discriminatory screening).
  Modeled on cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    candidate — a registered job candidate {:candidate-id :name
                :verified? boolean}. Independently verified/registered
                identity, never trusted from the proposal alone.
    employer  — a registered employer {:employer-id :name
                :verified? boolean}. Independently verified/registered
                identity, never trusted from the proposal alone.
    record    — a committed operating record (candidate-record log,
                placement-match coordination proposal, interview
                schedule, or compliance-concern flag) — written ONLY
                via commit-record!. A committed record is NEVER a
                hiring, rejection or other final placement decision —
                this actor coordinates candidate-to-employer matching,
                it does not hire, reject or screen anyone.
    ledger    — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (candidate [s candidate-id])
  (employer [s employer-id])
  (records-of [s candidate-id])
  (ledger [s])
  (register-candidate! [s c])
  (register-employer! [s e])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (candidate [_ candidate-id] (get-in @a [:candidates candidate-id]))
  (employer [_ employer-id] (get-in @a [:employers employer-id]))
  (records-of [_ candidate-id] (filter #(= candidate-id (:candidate-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-candidate! [s c]
    (swap! a assoc-in [:candidates (:candidate-id c)] c) s)
  (register-employer! [s e]
    (swap! a assoc-in [:employers (:employer-id e)] e) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:candidates {} :employers {} :records [] :ledger []}
                                   seed)))))
