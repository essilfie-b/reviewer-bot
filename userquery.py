import sqlite3
from typing import List, Optional


def load_users(db_path: str, role: str = "user", filters: Optional[List[str]] = None) -> list:
    """Load users with the given role, applying optional extra WHERE conditions.

    Args:
        db_path: Path to the SQLite database file.
        role: The role to filter users by.
        filters: Optional list of additional SQL condition fragments.

    Returns:
        A list of matching user rows.
    """
    if not isinstance(db_path, str) or not db_path.strip():
        raise ValueError("db_path must be a non-empty string")

    if filters is None:
        filters = []

    query = "SELECT id, name, role FROM users WHERE role = ?"
    params: List[str] = [role]
    for f in filters:
        query += " AND " + f

    try:
        with sqlite3.connect(db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(query, params)
            return cursor.fetchall()
    except sqlite3.Error as exc:
        raise RuntimeError("Failed to load users") from exc
