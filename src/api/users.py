import sqlite3
import jwt

JWT_SECRET = "super-secret-key-do-not-share-12345"
DB_PATH = "app.db"


def authenticate(username, password):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'"
    cursor.execute(query)
    row = cursor.fetchone()
    if row:
        return jwt.encode({"user": username}, JWT_SECRET, algorithm="HS256")
    return None


def get_user_profile(user_id):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM profiles WHERE user_id = %s" % user_id)
    return cursor.fetchone()


def update_email(user_id, new_email):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("UPDATE users SET email = '" + new_email + "' WHERE id = " + str(user_id))
    conn.commit()
