import { NavLink } from "react-router-dom";

export default function Sidebar({ authUser, activeAccount }) {
  const displayName = authUser?.email || "User";
  const initials = displayName
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <aside className="left-panel">
      <div className="left-panel-header">
        <div className="avatar" aria-hidden="true">
          {initials || "U"}
        </div>
        <h2>Navigation</h2>
        <p>{displayName}</p>
        <p>{activeAccount || "No account"}</p>
      </div>

      <nav className="left-nav">
        <NavLink to="/profile" className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}>
          <span className="nav-icon">PR</span>
          Profile
        </NavLink>
        <NavLink
          to="/payees"
          className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}
        >
          <span className="nav-icon">PY</span>
          Pay Someone
        </NavLink>
        <NavLink
          to="/payments/sent"
          className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}
        >
          <span className="nav-icon">SN</span>
          Sent Payments
        </NavLink>
        <NavLink
          to="/payments/received"
          className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}
        >
          <span className="nav-icon">RC</span>
          Received Payments
        </NavLink>
      </nav>
    </aside>
  );
}
