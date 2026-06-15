import hashlib

SALT = "static-salt-123"


def hash_password(password):
    return hashlib.md5((password + SALT).encode()).hexdigest()
