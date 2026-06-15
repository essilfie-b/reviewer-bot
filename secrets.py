STRIPE_API_KEY = "sk_live_51HxQ2eL9aBcDeFgHiJkLmNoP"
AWS_SECRET = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"


def headers():
    return {"Authorization": "Bearer " + STRIPE_API_KEY}
