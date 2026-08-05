import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { createPayee, createPaymentIntent, deletePayee, listPayees } from "../services/api";

const INITIAL_FORM = {
  payeeAccountNumber: "",
  nickname: ""
};

export default function PayeesPage() {
  const navigate = useNavigate();
  const [payees, setPayees] = useState([]);
  const [search, setSearch] = useState("");
  const [form, setForm] = useState(INITIAL_FORM);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function loadPayees() {
    try {
      setLoading(true);
      const data = await listPayees();
      setPayees(data);
      setError("");
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPayees();
  }, []);

  const filteredPayees = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) {
      return payees;
    }

    return payees.filter(
      (payee) =>
        payee.nickname.toLowerCase().includes(query) ||
        payee.payeeAccountNumber.toLowerCase().includes(query)
    );
  }, [payees, search]);

  async function startPayFlow(payeeAccountNumber) {
    const intent = await createPaymentIntent(payeeAccountNumber);
    navigate(`/pay/${intent.intentId}`);
  }

  async function handleSaveAndPay(event) {
    event.preventDefault();
    setError("");

    try {
      setSubmitting(true);
      const createdPayee = await createPayee(form);
      await loadPayees();
      setForm(INITIAL_FORM);
      await startPayFlow(createdPayee.payeeAccountNumber);
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handlePay(payeeAccountNumber) {
    try {
      setError("");
      await startPayFlow(payeeAccountNumber);
    } catch (apiError) {
      setError(apiError.message);
    }
  }

  async function handleDelete(payeeId) {
    try {
      setError("");
      await deletePayee(payeeId);
      await loadPayees();
    } catch (apiError) {
      setError(apiError.message);
    }
  }

  return (
    <div className="payees-page">
      <section className="detail-card">
        <h2 className="section-title">
          <span className="section-icon">PY</span>
          Pay Someone
        </h2>
        <p className="muted-text">Pick a saved payee, or add a new payee and continue in one step.</p>

        <label>
          Search payees
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search by nickname or account number"
          />
        </label>

        {loading ? (
          <p>Loading payees...</p>
        ) : (
          <div className="payee-list">
            {filteredPayees.map((payee) => (
              <article className="payee-item" key={payee.id}>
                <div>
                  <h3>{payee.nickname}</h3>
                  <p className="muted-text">{payee.payeeAccountNumber}</p>
                </div>
                <div className="payee-actions">
                  <button type="button" onClick={() => handlePay(payee.payeeAccountNumber)}>
                    Pay
                  </button>
                  <button type="button" className="ghost-btn" onClick={() => handleDelete(payee.id)}>
                    Remove
                  </button>
                </div>
              </article>
            ))}
            {filteredPayees.length === 0 ? <p className="empty">No payees found.</p> : null}
          </div>
        )}
      </section>

      <section className="create-form">
        <h3>Add Payee and Pay</h3>
        <form onSubmit={handleSaveAndPay}>
          <label>
            Nickname
            <input
              required
              value={form.nickname}
              onChange={(event) => setForm((prev) => ({ ...prev, nickname: event.target.value }))}
              placeholder="John Payroll"
            />
          </label>
          <label>
            Account number
            <input
              required
              value={form.payeeAccountNumber}
              onChange={(event) => setForm((prev) => ({ ...prev, payeeAccountNumber: event.target.value }))}
              placeholder="ACC-200"
            />
          </label>
          <button type="submit" disabled={submitting}>
            {submitting ? "Saving..." : "Save and pay"}
          </button>
        </form>
      </section>

      {error ? <p className="error-banner">{error}</p> : null}
    </div>
  );
}