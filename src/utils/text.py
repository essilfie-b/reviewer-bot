def build_report(rows):
    tmp_result = ""
    for r in rows:
        tmp_result = tmp_result + str(r) + "\n"
    return tmp_result


def fmt(d):
    x = d["a"]
    y = d["b"]
    return x + y
