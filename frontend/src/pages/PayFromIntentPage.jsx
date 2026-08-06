import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { createPayment, getPaymentIntent, updatePaymentStatus } from "../services/api";
import { getConvertedAmountInfo } from "../utils/currency";

const INITIAL_FORM = {
  amount: "",
  currency: "USD",
  reference: ""
};

export default function PayFromIntentPage({ activeAccount }) {
  const { intentId } = useParams();
  const navigate = useNavigate();
  const [intent, setIntent] = useState(null);
  const [form, setForm] = useState(INITIAL_FORM);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const conversion = getConvertedAmountInfo(form.amount, form.currency);

  useEffect(() => {
    async function loadIntent() {
      try {
        setLoading(true);
        const data = await getPaymentIntent(intentId);
        setIntent(data);
        setError("");
      } catch (apiError) {
        setError(apiError.message);
      } finally {
        setLoading(false);
      }
    }

    loadIntent();
  }, [intentId]);

  async function handleCreatePayment(event) {
    event.preventDefault();

    try {
      setSubmitting(true);
      setError("");
      const created = await createPayment(
        {
          sourceAccount: activeAccount || "",
          destAccount: intent.payeeAccountNumber,
          amount: form.amount,
          currency: form.currency,
          reference: form.reference,
          paymentIntentId: intent.intentId
        },
        intent.idempotencyKey
      );

      await updatePaymentStatus(created.id, "VALIDATED", "Validated after payment confirmation");
      navigate(`/payments/${created.id}`, { state: { from: "/payees" } });
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <p>Loading payment intent...</p>;
  }

  return (
    <section className="detail-card">
      <h2 className="section-title">
        <span className="section-icon">IN</span>
        Complete Payment
      </h2>

      {intent ? (
        <>
          <p>
            <strong>Payee:</strong> {intent.payeeNickname || "Saved payee"}
          </p>
          <p>
            <strong>Account:</strong> {intent.payeeAccountNumber}
          </p>
          <p>
            <strong>Source account:</strong> {activeAccount || "Auto (single-user default)"}
          </p>

          <form className="create-form" onSubmit={handleCreatePayment}>
            <label>
              Amount
              <input
                required
                type="number"
                min="0"
                step="0.01"
                value={form.amount}
                onChange={(event) => setForm((prev) => ({ ...prev, amount: event.target.value }))}
              />
            </label>

            <label>
              Currency
              <select
                value={form.currency}
                onChange={(event) => setForm((prev) => ({ ...prev, currency: event.target.value }))}
              >
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
                <option value="GBP">GBP</option>
                <option value="INR">INR</option>
              </select>
              {conversion ? (
                <span className="converted-hint">
                  Converted for balance: {conversion.convertedAmount.toFixed(2)} {conversion.baseCurrency}
                  {conversion.wasConverted ? ` (from ${conversion.sourceCurrency})` : " (no conversion needed)"}
                </span>
              ) : null}
            </label>

            <label>
              Reference
              <input
                value={form.reference}
                onChange={(event) => setForm((prev) => ({ ...prev, reference: event.target.value }))}
                placeholder="Salary advance"
              />
            </label>

            <button type="submit" disabled={submitting}>
              {submitting ? "Submitting..." : "Confirm payment"}
            </button>
          </form>
        </>
      ) : null}

      {error ? (
        <div>
          <p className="error-banner">{error}</p>
          <button type="button" onClick={() => navigate("/payees")}>Back to payees</button>
        </div>
      ) : null}
    </section>
  );
}