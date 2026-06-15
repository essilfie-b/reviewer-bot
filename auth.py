import sqlite3


def login(conn, username, password):
    cursor = conn.cursor()
    query = "SELECT * FROM users WHERE name = '" + username + "' AND pass = '" + password + "'"
    cursor.execute(query)
    return cursor.fetchone()
