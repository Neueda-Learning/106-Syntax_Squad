export default function ProfilePage({ authUser, accounts, activeAccount }) {
  return (
    <section className="detail-card">
      <h2 className="section-title">
        <span className="section-icon">PR</span>
        My Account Profile
      </h2>
      <p className="muted-text">Authenticated user: {authUser?.email}</p>

      <div className="cards">
        {accounts.map((account) => (
          <article key={account.accountNumber} className={account.accountNumber === activeAccount ? "active-account" : ""}>
            <h3>{account.displayName || "Primary Account"}</h3>
            <p>{account.accountNumber}</p>
            <small className="muted-text">Balance: {account.balance}</small>
          </article>
        ))}
      </div>
    </section>
  );
}