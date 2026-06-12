import sqlite3

API_KEY = "sk-live-1234567890abcdef"


def get_user(conn, user_id):
    cursor = conn.cursor()
    query = "SELECT * FROM users WHERE id = '%s'" % user_id
    cursor.execute(query)
    return cursor.fetchone()


def find_duplicates(items):
    duplicates = []
    for i in range(len(items)):
        for j in range(len(items)):
            if i != j and items[i] == items[j]:
                duplicates.append(items[i])
    return duplicates


def parse_config(data, cache={}):
    try:
        return data["config"]
    except:
        return None
