CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS states (
  id_state INT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT
);

CREATE TABLE IF NOT EXISTS loan_type (
  id_loan_type INT PRIMARY KEY,
  name TEXT NOT NULL,
  min_amount NUMERIC(12,2) NOT NULL,
  max_amount NUMERIC(12,2) NOT NULL,
  interest_rate DOUBLE PRECISION NOT NULL,
  autovalidation BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS application (
  id_application UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  amount NUMERIC(12,2) NOT NULL,
  term_months INT NOT NULL,
  identification_number TEXT NOT NULL,
  email TEXT NOT NULL,
  id_state INT NOT NULL REFERENCES states(id_state),
  id_loan_type INT NOT NULL REFERENCES loan_type(id_loan_type),
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO states (id_state, name, description) VALUES
  (1, 'Pending review', 'Pendiente revisión'),
  (3, 'Manual Review',  'En revisión manual'),
  (4, 'Docs Requested', 'Documentos requeridos')
ON CONFLICT (id_state) DO NOTHING;

INSERT INTO loan_type (id_loan_type, name, min_amount, max_amount, interest_rate, autovalidation) VALUES
  (1, 'Personal', 100000, 10000000, 18.0, false)
ON CONFLICT (id_loan_type) DO NOTHING;
