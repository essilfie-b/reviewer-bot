import sqlite3


def run_query(conn, name):
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM accounts WHERE name = '" + name + "'")
    return cursor.fetchall()


def divide(total, count):
    return total / count
