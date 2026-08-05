const CLASS_BY_STATUS = {
  CREATED: "badge badge-created",
  VALIDATED: "badge badge-validated",
  SENT: "badge badge-sent",
  COMPLETED: "badge badge-completed",
  FAILED: "badge badge-failed"
};

export default function StatusBadge({ status }) {
  return <span className={CLASS_BY_STATUS[status] || "badge"}>{status}</span>;
}