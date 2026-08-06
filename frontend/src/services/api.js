const BASE_URL = "/api";

async function request(path, options = {}) {
  const customHeaders = options.headers || {};
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...customHeaders
    }
  });

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    const error = new Error(data?.message || "Request failed");
    error.errorCode = data?.errorCode;
    error.status = response.status;
    throw error;
  }

  return data;
}

export function getMyAccounts() {
  return request("/accounts/me");
}

export function listPayees() {
  return request("/payees");
}

export function createPayee(payload) {
  return request("/payees", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function deletePayee(id) {
  return request(`/payees/${id}`, { method: "DELETE" });
}

export function createPaymentIntent(payeeAccountNumber) {
  return request("/payment-intents", {
    method: "POST",
    body: JSON.stringify({ payeeAccountNumber })
  });
}

export function getPaymentIntent(id) {
  return request(`/payment-intents/${id}`);
}

export function createPayment(payload, idempotencyKey) {
  return request("/payments", {
    method: "POST",
    headers: {
      "Idempotency-Key": idempotencyKey
    },
    body: JSON.stringify(payload)
  });
}

export function updatePaymentStatus(id, newStatus, reason) {
  return request(`/payments/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ newStatus, reason })
  });
}

export function getPayment(id) {
  return request(`/payments/${id}`);
}

export function listPayments(role, status, account) {
  const params = new URLSearchParams({ role });
  if (account) {
    params.set("account", account);
  }
  if (status && status !== "ALL") {
    params.set("status", status);
  }
  return request(`/payments?${params.toString()}`);
}

export function getPaymentHistory(id) {
  return request(`/payments/${id}/history`);
}

export function getSendAttempts(id) {
  return request(`/payments/${id}/attempts`);
}

export function sendPayment(id) {
  return request(`/payments/${id}/send`, { method: "POST" });
}