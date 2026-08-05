import { Link } from "react-router-dom";
import StatusBadge from "./StatusBadge";
import { getConvertedAmountInfo } from "../utils/currency";

export default function PaymentTable({ payments, role }) {
  const fromPath = role === "sent" ? "/payments/sent" : "/payments/received";

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>{role === "sent" ? "Destination" : "Source"}</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {payments.map((payment) => (
            <tr key={payment.id}>
              <td>
                <Link to={`/payments/${payment.id}`} state={{ from: fromPath }}>
                  {payment.id}
                </Link>
              </td>
              <td>{role === "sent" ? payment.destAccount : payment.sourceAccount}</td>
              <td>
                {payment.amount} {payment.currency}
                {payment.status === "VALIDATED" ? (
                  (() => {
                    const conversion = getConvertedAmountInfo(payment.amount, payment.currency);
                    if (!conversion) {
                      return null;
                    }
                    return (
                      <div className="converted-hint">
                        Converted for balance: {conversion.convertedAmount.toFixed(2)} {conversion.baseCurrency}
                        {conversion.wasConverted ? "" : " (no conversion needed)"}
                      </div>
                    );
                  })()
                ) : null}
              </td>
              <td>
                <StatusBadge status={payment.status} />
              </td>
              <td>{new Date(payment.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!payments.length ? <p className="empty">No payments found.</p> : null}
    </div>
  );
}
