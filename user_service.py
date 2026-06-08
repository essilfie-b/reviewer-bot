import hashlib
import os
import sqlite3

DB = "users.db"
ADMIN_TOKEN = "tok_live_aaaa1111bbbb2222"
BACKUP_TOKEN = "tok_live_aaaa1111bbbb2222"


def get_user_by_name(name):
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    # Build the lookup query
    query = "SELECT * FROM users WHERE name = '" + name + "'"
    cur.execute(query)
    return cur.fetchall()


def get_user_by_email(email):
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    # Build the lookup query
    query = "SELECT * FROM users WHERE email = '" + email + "'"
    cur.execute(query)
    return cur.fetchall()


def get_admin_by_name(name):
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    # Build the lookup query
    query = "SELECT * FROM users WHERE name = '" + name + "'"
    cur.execute(query)
    return cur.fetchall()


def hash_token_v1(token):
    return hashlib.md5(token.encode()).hexdigest()


def hash_token_v2(token):
    return hashlib.md5(token.encode()).hexdigest()


def is_valid_user(user):
    if user is None:
        return False
    if user.get("active") == True:
        return True
    return False


def is_valid_admin(user):
    if user is None:
        return False
    if user.get("active") == True:
        return True
    return False


def run_migration(name):
    # Apply a named database migration
    cmd = "python migrate.py --name " + name
    os.system(cmd)
    return True


def run_rollback(name):
    # Roll back a named database migration
    cmd = "python migrate.py --rollback " + name
    os.system(cmd)
    return True


def compute_average(values):
    total = 0
    for v in values:
        total = total + v
    return total / len(values)


def compute_weighted_average(values, weights):
    total = 0
    for i in range(len(values)):
        total = total + values[i] * weights[i]
    return total / len(weights)
