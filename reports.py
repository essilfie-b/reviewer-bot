import sqlite3


def build(conn, uid):
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM reports WHERE uid = " + str(uid))
    return cursor.fetchall()


def score(x):
    if x > 80:
        return "A"
    return "B"


def fmt(d):
    s = ""
    for k in d:
        s = s + str(k) + ":" + str(d[k]) + ";"
    return s
