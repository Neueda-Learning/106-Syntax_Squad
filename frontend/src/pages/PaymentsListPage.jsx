import { useEffect, useMemo, useState } from "react";
import PaymentTable from "../components/PaymentTable";
import { listPayments } from "../services/api";

export default function PaymentsListPage({ activeAccount, role }) {
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [globalError, setGlobalError] = useState("");

  async function loadPayments() {
    if (role === "received" && !activeAccount.trim()) {
      setPayments([]);
      return;
    }

    try {
      setLoading(true);
      const selectedAccount = activeAccount.trim();
      const data = await listPayments(role, statusFilter, selectedAccount || undefined);
      setPayments(data);
      setGlobalError("");
    } catch (error) {
      setGlobalError(error.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPayments();
  }, [activeAccount, role, statusFilter]);

  const summary = useMemo(() => {
    if (role !== "received") {
      return null;
    }

    const totalReceived = payments
      .filter((payment) => payment.status === "COMPLETED")
      .reduce((sum, payment) => sum + Number(payment.amount), 0);
    const pendingCount = payments.filter((payment) => payment.status === "VALIDATED" || payment.status === "SENT").length;
    const failedCount = payments.filter((payment) => payment.status === "FAILED").length;

    return { totalReceived, pendingCount, failedCount };
  }, [payments, role]);

  return (
    <div>
      <section className="control-panel">
        <label>
          Account Number Context
          <input value={activeAccount || "Auto (all sent by current user)"} readOnly />
        </label>

        <label>
          Status Filter
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="ALL">All</option>
            <option value="CREATED">CREATED</option>
            <option value="VALIDATED">VALIDATED</option>
            <option value="SENT">SENT</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="FAILED">FAILED</option>
          </select>
        </label>
      </section>

      {role === "received" && summary ? (
        <section className="cards">
          <article>
            <h3>Total Completed Received</h3>
            <p>{summary.totalReceived.toFixed(2)}</p>
          </article>
          <article>
            <h3>Pending Incoming</h3>
            <p>{summary.pendingCount}</p>
          </article>
          <article>
            <h3>Failed Incoming</h3>
            <p>{summary.failedCount}</p>
          </article>
        </section>
      ) : null}

      <section>
        <h2 className="section-title">
          <span className="section-icon">{role === "sent" ? "SN" : "RC"}</span>
          {role === "sent" ? "Sent Payments" : "Received Payments"}
        </h2>
        {loading ? <p>Loading payments...</p> : <PaymentTable payments={payments} role={role} />}
      </section>

      {globalError ? <p className="error-banner">{globalError}</p> : null}
    </div>
  );
}