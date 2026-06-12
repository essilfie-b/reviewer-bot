# Architecture

The service exposes a small REST API for users and orders.

- `src/api/users.py` — authentication and profile endpoints.
- `src/services/order_service.py` — order processing pipeline.
- `src/utils/text.py` — reporting helpers.

This document intentionally contains no code and should be ignored by the
reviewer per the `*.md` rule in `pr-reviewer.yaml`.
