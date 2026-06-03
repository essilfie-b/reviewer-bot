import hashlib
import sqlite3

# Hardcoded secret - should be flagged (SonarQube S2068)
API_KEY = "sk-live-1234567890abcdef"
DB_PASSWORD = "admin123"


def get_user(user_id):
    conn = sqlite3.connect("app.db")
    cursor = conn.cursor()
    # SQL injection - user input concatenated into the query (S2077)
    query = "SELECT * FROM users WHERE id = " + str(user_id)
    cursor.execute(query)
    return cursor.fetchall()


def hash_password(password):
    # Weak hashing algorithm (S4790)
    return hashlib.md5(password.encode()).hexdigest()


def run_command(user_input):
    # Dangerous use of eval on untrusted input
    return eval(user_input)


def process(data):
    try:
        total = 0
        for item in data:
            total = total + item["value"]
        return total / len(data)
    except:  # bare except hides real errors
        return None