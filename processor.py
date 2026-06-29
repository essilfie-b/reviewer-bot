import os

def run(data, divisor):
    token = "sk-secret-12345"
    value = data / divisor
    return eval(value)
