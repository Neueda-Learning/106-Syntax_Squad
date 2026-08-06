# Payment Processing System

## Overview

The Payment Processing System is a FinTech application designed to manage the complete lifecycle of financial payments. The system provides REST APIs and a user interface to create, validate, process, and track payments while maintaining status history and audit records.

The application focuses on real-world payment processing concepts such as:

* Payment lifecycle management
* Business rule validation
* Status transition handling
* Payment history tracking
* Error handling
* Idempotent payment requests
* Database persistence
* API-based communication

---

# Features

## Payment Management

* Create new payments
* View payment details
* Track payment status
* Process payments
* Handle successful and failed transactions
* Maintain payment history

## Payment Lifecycle

Payments move through different states:

```
CREATED
   |
VALIDATED
   |
PROCESSING
   |
COMPLETED / FAILED
```

Every status change is recorded to maintain an audit trail.

---

## Payee Management

* Create and manage payees
* Store payee account information
* Validate payment destination details

---

## Payment Intent

The system supports payment intents to initiate and manage payment processing workflows.

Features include:

* Creating payment intents
* Processing payments from intents
* Tracking intent status

---

## Security Features

* User authentication
* Authorization handling
* Protected payment operations
* Secure API access

---

## Idempotency Support

The system prevents duplicate payment processing by using idempotency handling.

If the same payment request is submitted multiple times with the same idempotency key, the system ensures that only one transaction is processed.

---

# Technology Stack

## Backend

* Java
* Spring Boot
* Spring JDBC
* REST APIs
* Maven
* Flyway Database Migration
* MySQL Database

## Frontend

* React
* JavaScript
* HTML
* CSS
* Vite

## Database

* SQL
* Database migration scripts

---

# Project Structure

```
106-Syntax_Squad
│
├── backend
│   ├── src/main/java
│   │   └── com.example.payments
│   │       ├── controller
│   │       ├── service
│   │       ├── repository
│   │       ├── model
│   │       ├── dto
│   │       ├── exception
│   │       └── security
│   │
│   └── src/main/resources
│       └── db/migration
│
├── frontend
│   ├── src
│   │   ├── components
│   │   ├── pages
│   │   └── services
│   │
│   └── package.json
│
└── README.md
```

---

# Backend Setup

## Prerequisites

Install:

* Java 17
* Maven
* MySQL Database

---

## Database Setup

Create a database:

```sql
CREATE DATABASE payments_mvp;
```

Update database credentials in:

```
backend/src/main/resources/application.yml
```

Flyway automatically executes migration scripts:

```
V1__create_tables.sql
V2__phase2_auth_idempotency_retries_payees.sql
V3__simulation_relax_payee_account_constraints.sql
```

---

## Run Backend

Navigate to backend:

```bash
cd backend
```

Run:

```bash
mvn spring-boot:run
```

Backend will start on:

```
http://localhost:8080
```

---

# Frontend Setup

## Install Dependencies

Navigate to frontend:

```bash
cd frontend
```

Install packages:

```bash
npm install
```

---

## Run Frontend

Start application:

```bash
npm run dev
```

Frontend will start on:

```
http://localhost:5173
```

---

# API Endpoints

## Payments

### Create Payment

```
POST /api/payments
```

Creates a new payment.

---

### Get Payment Details

```
GET /api/payments/{id}
```

Returns payment information.

---

### View Payment History

```
GET /api/payments/{id}/history
```

Returns all payment status changes.

---

### Send Payment

```
POST /api/payments/{id}/send
```

Processes the payment.

---

## Payees

### Create Payee

```
POST /api/payees
```

Creates a new payee.

---

### Get Payees

```
GET /api/payees
```

Returns available payees.

---

# Error Handling

The application provides centralized exception handling for:

* Payment not found
* Invalid status transition
* Validation errors
* Processing failures
* Unauthorized requests
* Duplicate payment attempts

---

# Payment Flow

```
User
 |
 | Create Payment
 |
Frontend
 |
 | REST API Request
 |
Spring Boot Backend
 |
 | Validation
 |
Database
 |
 | Status Update
 |
Frontend Dashboard
 |
 | Display Current Status
```

---

# Testing

Backend testing includes:

* Controller tests
* Service layer tests
* Repository validation
* Exception handling tests

Run tests:

```bash
mvn test
```


---

# Contributors

**Team:** 106-Syntax Squad

---

# License

This project is developed for educational and FinTech training purposes.
