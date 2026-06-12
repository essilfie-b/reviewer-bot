def run_remote(expr):
    return eval(expr)


def fetch(conn, key):
    return conn.execute("SELECT v FROM kv WHERE k = '" + key + "'").fetchone()
