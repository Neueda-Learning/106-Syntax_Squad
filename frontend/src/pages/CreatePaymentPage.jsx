import { useState } from "react";
import { createPayment, updatePaymentStatus } from "../services/api";

const INITIAL_FORM = {
  destAccount: "",
  amount: "",
  currency: "USD",
  reference: ""
};

export default function CreatePaymentPage({ accountNumber }) {
  const [form, setForm] = useState(INITIAL_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [globalError, setGlobalError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleCreatePayment(event) {
    event.preventDefault();
    setFieldErrors({});
    setGlobalError("");
    setSuccessMessage("");

    try {
      setSubmitting(true);
      const created = await createPayment({
        sourceAccount: accountNumber || "",
        destAccount: form.destAccount,
        amount: form.amount,
        currency: form.currency,
        reference: form.reference
      });

      await updatePaymentStatus(created.id, "VALIDATED", "Validated by sender action");
      setForm(INITIAL_FORM);
      setSuccessMessage(`Payment #${created.id} created and moved to VALIDATED.`);
    } catch (error) {
      mapApiError(error);
    } finally {
      setSubmitting(false);
    }
  }

  function mapApiError(error) {
    if (error.errorCode === "INVALID_ACCOUNT") {
      setFieldErrors({ destAccount: error.message, sourceAccount: error.message });
      return;
    }
    if (error.errorCode === "INVALID_AMOUNT") {
      setFieldErrors({ amount: error.message });
      return;
    }
    if (error.errorCode === "INVALID_CURRENCY") {
      setFieldErrors({ currency: error.message });
      return;
    }
    setGlobalError(error.message);
  }

  return (
    <section className="create-form">
      <h2 className="section-title">
        <span className="section-icon">CR</span>
        Create Payment
      </h2>
      <form onSubmit={handleCreatePayment}>
        <label>
          Source Account
          <input value={accountNumber || "Auto (single-user default)"} readOnly />
          {fieldErrors.sourceAccount ? <small className="error">{fieldErrors.sourceAccount}</small> : null}
        </label>
        <label>
          Destination Account
          <input
            value={form.destAccount}
            onChange={(e) => setForm((prev) => ({ ...prev, destAccount: e.target.value }))}
          />
          {fieldErrors.destAccount ? <small className="error">{fieldErrors.destAccount}</small> : null}
        </label>
        <label>
          Amount
          <input
            type="number"
            min="0"
            step="0.01"
            value={form.amount}
            onChange={(e) => setForm((prev) => ({ ...prev, amount: e.target.value }))}
          />
          {fieldErrors.amount ? <small className="error">{fieldErrors.amount}</small> : null}
        </label>
        <label>
          Currency
          <select value={form.currency} onChange={(e) => setForm((prev) => ({ ...prev, currency: e.target.value }))}>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
            <option value="GBP">GBP</option>
            <option value="INR">INR</option>
          </select>
          {fieldErrors.currency ? <small className="error">{fieldErrors.currency}</small> : null}
        </label>
        <label>
          Reference (optional)
          <input value={form.reference} onChange={(e) => setForm((prev) => ({ ...prev, reference: e.target.value }))} />
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? "Creating..." : "Create and Validate"}
        </button>
      </form>

      {successMessage ? <p className="success-banner">{successMessage}</p> : null}
      {globalError ? <p className="error-banner">{globalError}</p> : null}
    </section>
  );
}