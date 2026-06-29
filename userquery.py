import os
import sqlite3
from typing import Dict, List, Optional

ALLOWED_FILTER_COLUMNS = {"status", "department", "active"}


def load_users(
    db_path: str,
    role: str = "user",
    filters: Optional[Dict[str, str]] = None,
) -> List[tuple]:
    """Load users with the given role, applying optional equality filters.

    Args:
        db_path: Path to the SQLite database file.
        role: The role to filter users by.
        filters: Optional mapping of column name to required value. Only
            columns in ``ALLOWED_FILTER_COLUMNS`` are accepted.

    Returns:
        A list of matching user rows.

    Raises:
        ValueError: If ``db_path`` is empty or a filter column is not allowed.
        RuntimeError: If the database query fails.
    """
    if not isinstance(db_path, str) or not db_path.strip():
        raise ValueError("db_path must be a non-empty string")

    resolved_path = os.path.realpath(db_path)

    filters = filters or {}
    conditions = ["role = ?"]
    params: List[str] = [role]
    for column, value in filters.items():
        if column not in ALLOWED_FILTER_COLUMNS:
            raise ValueError(f"Unsupported filter column: {column}")
        conditions.append(f"{column} = ?")
        params.append(value)

    query = "SELECT id, name, role FROM users WHERE " + " AND ".join(conditions)

    try:
        with sqlite3.connect(resolved_path) as conn:
            cursor = conn.cursor()
            cursor.execute(query, params)
            return cursor.fetchall()
    except sqlite3.Error as exc:
        raise RuntimeError("Failed to load users") from exc
