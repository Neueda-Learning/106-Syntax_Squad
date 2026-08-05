import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import StatusBadge from "../components/StatusBadge";
import Timeline from "../components/Timeline";
import { getPayment, getPaymentHistory, getSendAttempts, sendPayment } from "../services/api";

const TERMINAL_STATUSES = new Set(["COMPLETED", "FAILED"]);

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export default function PaymentDetailPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [payment, setPayment] = useState(null);
  const [history, setHistory] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(true);
  const backPath = location.state?.from || "/payments/sent";

  async function loadDetails() {
    try {
      setLoading(true);
      const [paymentData, historyData, attemptsData] = await Promise.all([
        getPayment(id),
        getPaymentHistory(id),
        getSendAttempts(id)
      ]);
      setPayment(paymentData);
      setHistory(historyData);
      setAttempts(attemptsData);
      setError("");
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDetails();
  }, [id]);

  async function handleSendNow() {
    try {
      setSending(true);
      setError("");
      await sendPayment(id);

      let latestPayment = payment;
      for (let index = 0; index < 20; index += 1) {
        await sleep(1500);
        latestPayment = await getPayment(id);
        setPayment(latestPayment);
        setAttempts(await getSendAttempts(id));

        if (TERMINAL_STATUSES.has(latestPayment.status)) {
          break;
        }
      }

      setHistory(await getPaymentHistory(id));

      if (!TERMINAL_STATUSES.has(latestPayment?.status)) {
        setError("Send is still in progress. Refresh in a moment for final state.");
      }
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setSending(false);
    }
  }

  if (loading) {
    return <p>Loading payment details...</p>;
  }

  if (!payment) {
    return (
      <div>
        <button onClick={() => navigate(backPath)}>Back</button>
        <p className="error-banner">Payment not found.</p>
      </div>
    );
  }

  return (
    <div className="detail-page">
      <button className="back-btn" onClick={() => navigate(backPath)}>
        Back
      </button>

      <section className="detail-card">
        <h2>Payment #{payment.id}</h2>
        <p>
          <strong>Amount:</strong> {payment.amount} {payment.currency}
        </p>
        <p>
          <strong>Source:</strong> {payment.sourceAccount}
        </p>
        <p>
          <strong>Destination:</strong> {payment.destAccount}
        </p>
        <p>
          <strong>Status:</strong> <StatusBadge status={payment.status} />
        </p>
        {payment.reference ? (
          <p>
            <strong>Reference:</strong> {payment.reference}
          </p>
        ) : null}

        {payment.status === "FAILED" ? (
          <div className="error-banner">
            Payment failed with error code: {payment.errorCode || "PROCESSING_ERROR"}
          </div>
        ) : null}

        {payment.status === "VALIDATED" ? (
          <button className="send-btn" onClick={handleSendNow} disabled={sending}>
            {sending ? "Sending..." : "Send now"}
          </button>
        ) : null}
      </section>

      <section className="detail-card">
        <h3>Send attempts</h3>
        {attempts.length === 0 ? (
          <p className="muted-text">No send attempts yet.</p>
        ) : (
          <div className="attempt-list">
            {attempts.map((attempt) => (
              <article key={`${attempt.attemptNumber}-${attempt.attemptedAt}`} className="attempt-item">
                <strong>Attempt {attempt.attemptNumber}</strong>
                <span>{attempt.outcome}</span>
                <small className="muted-text">{new Date(attempt.attemptedAt).toLocaleString()}</small>
              </article>
            ))}
          </div>
        )}
      </section>

      <Timeline history={history} payment={payment} />

      {error ? <p className="error-banner">{error}</p> : null}
    </div>
  );
}