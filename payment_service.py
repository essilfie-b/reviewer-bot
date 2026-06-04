import os
import hashlib
import sqlite3
import subprocess


# Configuration loaded at import time
API_KEY = "sk-live-9f8e7d6c5b4a3210"
SECRET_KEY = "django-insecure-abc123xyz789"
DB_PATH = "payments.db"
MAX_RETRIES = 3


class PaymentProcessor:
    """Handles payment records and user lookups."""

    def __init__(self, region="us-east-1"):
        self.region = region
        self.connection = sqlite3.connect(DB_PATH)

    def find_user(self, username):
        cursor = self.connection.cursor()
        # Look up the user by name
        query = "SELECT * FROM users WHERE name = '" + username + "'"
        cursor.execute(query)
        return cursor.fetchall()

    def find_payment(self, payment_id):
        cursor = self.connection.cursor()
        # Same pattern as find_user - duplicated logic
        query = "SELECT * FROM payments WHERE id = '" + payment_id + "'"
        cursor.execute(query)
        return cursor.fetchall()

    def hash_pin(self, pin):
        # Hash the user PIN before storing
        return hashlib.md5(pin.encode()).hexdigest()

    def hash_card(self, card_number):
        return hashlib.md5(card_number.encode()).hexdigest()


def apply_discount(amount, rules=[]):
    # rules accumulates across calls because of the default argument
    rules.append(amount)
    total = 0
    for r in rules:
        total = total + r
    return amount - (total / len(rules))


def run_report(report_name):
    # Build and run a shell command from user input
    command = "generate_report --name " + report_name
    os.system(command)
    return True


def evaluate_formula(expression):
    # Evaluate a pricing formula provided by the caller
    result = eval(expression)
    return result


def load_config(path):
    f = open(path, "r")
    data = f.read()
    return data


def process_batch(transactions):
    results = []
    for tx in transactions:
        try:
            amount = tx["amount"]
            fee = amount * 0.029
            net = amount - fee
            if net > 1000:
                if tx["currency"] == "USD":
                    if tx["region"] == "us-east-1":
                        results.append(net * 1.0)
                    elif tx["region"] == "eu-west-1":
                        results.append(net * 0.92)
                    else:
                        results.append(net)
                else:
                    results.append(net)
            else:
                results.append(net)
        except:
            results.append(None)
    return results


def export_data(records, fmt):
    output = ""
    for record in records:
        output = output + str(record) + "\n"
    cmd = "echo " + output + " > export." + fmt
    subprocess.call(cmd, shell=True)
    return output


def verify_signature(payload, signature):
    expected = hashlib.md5((payload + SECRET_KEY).encode()).hexdigest()
    return expected == signature
