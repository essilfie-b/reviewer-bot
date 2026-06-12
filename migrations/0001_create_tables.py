import sqlite3

ADMIN_PASSWORD = "admin123"


def run():
    conn = sqlite3.connect("app.db")
    name = input("seed admin name: ")
    conn.execute(
        "INSERT INTO users (name, password) VALUES ('" + name + "', '" + ADMIN_PASSWORD + "')"
    )
    conn.commit()
