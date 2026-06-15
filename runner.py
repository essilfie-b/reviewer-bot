def run_rule(expression, context):
    return eval(expression, {}, context)


def load_plugin(name):
    module = __import__(name)
    return module
