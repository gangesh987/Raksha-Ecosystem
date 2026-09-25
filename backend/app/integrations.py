"""
External integration boundaries.

A real SMS/push provider can be connected here using credentials stored in a
secrets manager. The prototype intentionally returns a prepared notification
rather than sending a real message.
"""
class NotificationProvider:
    async def send_trusted_contact(self, destination: str, message: str) -> dict:
        return {"delivered": False, "simulated": True, "destination": destination, "message": message}

class SmsProvider(NotificationProvider):
    pass
