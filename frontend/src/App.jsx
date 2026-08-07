import { Navigate, Route, Routes } from "react-router-dom";
import { useEffect, useState } from "react";
import Sidebar from "./components/Sidebar";
import PaymentsListPage from "./pages/PaymentsListPage";
import PaymentDetailPage from "./pages/PaymentDetailPage";
import PayeesPage from "./pages/PayeesPage";
import PayFromIntentPage from "./pages/PayFromIntentPage";
import ProfilePage from "./pages/ProfilePage";
import { getMyAccounts } from "./services/api";

export default function App() {
  const initialTheme = localStorage.getItem("theme") || "light";
  const [theme, setTheme] = useState(initialTheme);
  const [accounts, setAccounts] = useState([]);
  const [activeAccount, setActiveAccount] = useState("");
  const [accountsLoading, setAccountsLoading] = useState(false);
  const [accountsError, setAccountsError] = useState("");
  const authUser = { email: "single.user@local" };

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
  }, [theme]);

  useEffect(() => {
    async function loadAccounts() {
      try {
        setAccountsLoading(true);
        const data = await getMyAccounts();
        setAccounts(data);
        setAccountsError("");
        if (data.length > 0) {
          setActiveAccount((prev) => (prev && data.some((item) => item.accountNumber === prev) ? prev : data[0].accountNumber));
        } else {
          setActiveAccount("");
        }
      } catch (error) {
        setAccountsError(error.message);
      } finally {
        setAccountsLoading(false);
      }
    }

    loadAccounts();
  }, []);

  function toggleTheme() {
    setTheme((prev) => (prev === "light" ? "dark" : "light"));
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>Payment Processing Application</h1>
          <p>Lifecycle: payee → intent → CREATED → VALIDATED → SENT → COMPLETED / FAILED</p>
        </div>
        <div className="topbar-actions">
          <label className="account-picker" htmlFor="account-picker">
            Active Account
            <select
              id="account-picker"
              value={activeAccount}
              onChange={(event) => setActiveAccount(event.target.value)}
              disabled={accountsLoading || accounts.length === 0}
            >
              {accounts.length === 0 ? <option value="">No linked accounts</option> : null}
              {accounts.map((account) => (
                <option key={account.accountNumber} value={account.accountNumber}>
                  {account.accountNumber}
                </option>
              ))}
            </select>
          </label>

          <button
            className="theme-toggle"
            type="button"
            onClick={toggleTheme}
            aria-label={theme === "light" ? "Switch to dark mode" : "Switch to light mode"}
            title={theme === "light" ? "Switch to dark mode" : "Switch to light mode"}
          >
            {theme === "light" ? "\u263D" : "\u2600\uFE0F"}
          </button>
        </div>
      </header>

      <main className="main-content">
        <Sidebar authUser={authUser} activeAccount={activeAccount} />

        <section className="page-content">
          {accountsError ? <p className="error-banner">{accountsError}</p> : null}
          <Routes>
            <Route path="/" element={<Navigate to="/payees" replace />} />
            <Route path="/payees" element={<PayeesPage />} />
            <Route
              path="/pay/:intentId"
              element={<PayFromIntentPage activeAccount={activeAccount} />}
            />
            <Route
              path="/payments/sent"
              element={<PaymentsListPage activeAccount={activeAccount} role="sent" />}
            />
            <Route
              path="/payments/received"
              element={<PaymentsListPage activeAccount={activeAccount} role="received" />}
            />
            <Route path="/payments/:id" element={<PaymentDetailPage />} />
            <Route
              path="/profile"
              element={<ProfilePage authUser={authUser} accounts={accounts} activeAccount={activeAccount} />}
            />
            <Route path="*" element={<Navigate to="/payees" replace />} />
          </Routes>
        </section>
      </main>
    </div>
  );
}
