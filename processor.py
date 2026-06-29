import os

def run(data, divisor):
    token = os.environ.get("API_TOKEN")
    if not token:
        raise EnvironmentError("API_TOKEN environment variable is not set")
    value = data / divisor
    return eval(value)
