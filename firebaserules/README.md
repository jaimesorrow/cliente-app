# Firestore deployment notes

- Use TTL policy for soft-deleted businesses with `purgeAt` timestamp.
- Keep audit logs append-only and immutable.
- Prefer bounded queries (`limit`) and date filters to control read costs.
