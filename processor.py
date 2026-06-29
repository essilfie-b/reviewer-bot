# Remove the unused import entirely

def run(data: float, divisor: float) -> float:
    """
    Divides data by divisor and returns the result.

    :param data: The dividend.
    :param divisor: The divisor; must not be zero.
    :raises ValueError: If divisor is zero.
    """
    if divisor == 0:
        raise ValueError("divisor must not be zero")
    return data / divisor
    token = os.environ.get("API_TOKEN")
    if not token:
        raise EnvironmentError("API_TOKEN environment variable is not set")
    if divisor == 0:
        raise ValueError("divisor must not be zero")
    value = data / divisor
    return value  # arithmetic result is already a float; eval() is not needed
