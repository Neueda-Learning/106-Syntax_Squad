const STATUS_DESCRIPTION = {
  CREATED: "Payment record initialized",
  VALIDATED: "Field and business rule checks passed",
  SENT: "Payment request sent to processing network",
  COMPLETED: "Funds delivered to destination account",
  FAILED: "Payment processing failed"
};

function toTitle(status) {
  const normalized = String(status || "").toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function formatTime(value) {
  const date = new Date(value);
  const base = date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  });
  return `${base}.${String(date.getMilliseconds()).padStart(3, "0")}`;
}

function statusTone(status) {
  if (status === "SENT") {
    return "warn";
  }
  if (status === "FAILED") {
    return "error";
  }
  return "ok";
}

export default function Timeline({ history, payment }) {
  if (!history.length) {
    return <p>No history yet.</p>;
  }

  return (
    <section className="status-history-card">
      <h3>Payment P-{payment?.id} status history</h3>
      <p className="muted-text">
        {payment?.amount} {payment?.currency} to {payment?.destAccount}
      </p>

      <div className="status-timeline">
        {history.map((item, index) => (
          <div key={item.id} className="status-step">
            <div className="status-marker-column">
              <span className={`status-dot ${statusTone(item.toStatus)}`} />
              {index < history.length - 1 ? <span className="status-connector" /> : null}
            </div>

            <div className="status-step-body">
              <div className="status-step-row">
                <div>
                  <h4>{toTitle(item.toStatus)}</h4>
                  <p>{item.reason || STATUS_DESCRIPTION[item.toStatus] || "Status updated"}</p>
                </div>
                <span className="status-time">{formatTime(item.changedAt)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}