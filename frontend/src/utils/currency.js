const BASE_CURRENCY = (import.meta.env.VITE_BASE_CURRENCY || "USD").trim().toUpperCase();

const TO_USD_RATES = {
  USD: Number(import.meta.env.VITE_RATE_USD_TO_USD || 1.0),
  EUR: Number(import.meta.env.VITE_RATE_EUR_TO_USD || 1.08),
  GBP: Number(import.meta.env.VITE_RATE_GBP_TO_USD || 1.27),
  INR: Number(import.meta.env.VITE_RATE_INR_TO_USD || 0.012)
};

function roundToTwo(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function normalizeCurrency(currency) {
  if (!currency) {
    return null;
  }
  return String(currency).trim().toUpperCase();
}

export function getConvertedAmountInfo(amount, sourceCurrency) {
  const normalizedSource = normalizeCurrency(sourceCurrency);
  if (!normalizedSource || Number.isNaN(Number(amount))) {
    return null;
  }

  const sourceRate = TO_USD_RATES[normalizedSource];
  const baseRate = TO_USD_RATES[BASE_CURRENCY];
  if (!sourceRate || !baseRate) {
    return null;
  }

  const sourceAmount = Number(amount);
  const amountInUsd = sourceAmount * sourceRate;
  const converted = roundToTwo(amountInUsd / baseRate);

  return {
    sourceAmount: roundToTwo(sourceAmount),
    sourceCurrency: normalizedSource,
    baseCurrency: BASE_CURRENCY,
    convertedAmount: converted,
    wasConverted: normalizedSource !== BASE_CURRENCY
  };
}
