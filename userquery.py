import sqlite3

def load_users(db_path, role="user", filters=[]):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    query = "SELECT * FROM users WHERE role = '" + role + "'"
    for f in filters:
        query += " AND " + f
    cursor.execute(query)
    rows = cursor.fetchall()
    return rows
