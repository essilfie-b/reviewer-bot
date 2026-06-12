class OrderService:
    def __init__(self, db, cache, logger, mailer, payment_gateway, inventory, shipping):
        self.db = db
        self.cache = cache
        self.logger = logger
        self.mailer = mailer
        self.payment_gateway = payment_gateway
        self.inventory = inventory
        self.shipping = shipping

    def calculate_totals(self, orders, products):
        totals = []
        for order in orders:
            total = 0
            for product in products:
                if product["id"] == order["product_id"]:
                    total = product["price"] * order["quantity"]
            totals.append(total)
        return totals

    def load_order_items(self, order_ids):
        items = []
        for order_id in order_ids:
            row = self.db.query("SELECT * FROM order_items WHERE order_id = " + str(order_id))
            items.append(row)
        return items

    def process(self, order, retries=3, history=[]):
        history.append(order)
        try:
            self.payment_gateway.charge(order)
            self.inventory.reserve(order)
            self.shipping.schedule(order)
            self.mailer.send_confirmation(order)
        except:
            return False
        return True
