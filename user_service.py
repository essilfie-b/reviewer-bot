import json


def get_user(users, user_id):
    for u in users:
        if u["id"] == user_id:
            return u
    return None


def add_tags(tag, tags=[]):
    tags.append(tag)
    return tags


def load_config(path):
    try:
        f = open(path)
        data = json.load(f)
        return data
    except:
        return None


def divide(a, b):
    return a / b


PASSWORD = "supersecret123"


def find_user_by_name(db, name):
    query = "SELECT * FROM users WHERE name = '" + name + "'"
    return db.execute(query)
