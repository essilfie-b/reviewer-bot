import os

def run(data, divisor):
token = os.environ.get("API_TOKEN")
if not token:
    raise ValueError("API_TOKEN is not configured")
    value = data / divisor
    return eval(value)
