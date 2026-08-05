package com.example.payments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyConversionService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR");

    private final String baseCurrency;
    private final Map<String, BigDecimal> toUsdRates;

    public CurrencyConversionService(
        @Value("${payment.currency.base:USD}") String baseCurrency,
        @Value("${payment.currency.usd-to-usd:1.0}") BigDecimal usdToUsd,
        @Value("${payment.currency.eur-to-usd:1.08}") BigDecimal eurToUsd,
        @Value("${payment.currency.gbp-to-usd:1.27}") BigDecimal gbpToUsd,
        @Value("${payment.currency.inr-to-usd:0.012}") BigDecimal inrToUsd
    ) {
        this.baseCurrency = normalizeCurrency(baseCurrency);
        this.toUsdRates = Map.of(
            "USD", usdToUsd,
            "EUR", eurToUsd,
            "GBP", gbpToUsd,
            "INR", inrToUsd
        );
    }

    public BigDecimal convertToBase(BigDecimal amount, String sourceCurrency) {
        String normalizedSource = normalizeCurrency(sourceCurrency);
        if (normalizedSource.equals(baseCurrency)) {
            return amount;
        }

        BigDecimal amountInUsd = amount.multiply(rateToUsd(normalizedSource));
        BigDecimal baseRateToUsd = rateToUsd(baseCurrency);
        return amountInUsd.divide(baseRateToUsd, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rateToUsd(String currency) {
        BigDecimal rate = toUsdRates.get(currency);
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Missing or invalid conversion rate for currency: " + currency);
        }
        return rate;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        String normalized = currency.trim().toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        return normalized;
    }
}