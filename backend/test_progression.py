import os
import sys
import asyncio
import httpx

BASE_URL = "http://localhost:8000"

async def run_scam_progression():
    async with httpx.AsyncClient() as client:
        try:
            await client.post(f"{BASE_URL}/api/auth/register", json={"email": "test@example.com", "name": "Test User", "password": "password123"})
        except: pass
        resp = await client.post(f"{BASE_URL}/api/auth/login", json={"email": "test@example.com", "password": "password123"})
        token = resp.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        sid = (await client.post(f"{BASE_URL}/api/sessions", json={"source": "test"}, headers=headers)).json()["id"]
    
        turns = [
            "Hello, I am calling from the cyber crime department.",
            "Your Aadhaar is connected to an illegal transaction.",
            "Do not tell your family or anyone else.",
            "You must cooperate immediately.",
            "Transfer the money to the verification account.",
            "Send me the OTP to complete verification."
        ]
        
        print("\n--- SCAM PROGRESSION TEST ---")
        for t in turns:
            res = (await client.post(f"{BASE_URL}/api/sessions/{sid}/analyze", json={"transcript": t}, headers=headers)).json()
            print(f"[{res.get('stage', 'UNKNOWN'):15s}] Score: {res.get('risk_score', 0):3d} | Vel: {res.get('velocity_level', 'UNKNOWN'):10s} | Brake: {res.get('safety_brake_triggered')} | {t}")

async def run_hard_negatives():
    async with httpx.AsyncClient() as client:
        token = (await client.post(f"{BASE_URL}/api/auth/login", json={"email": "test@example.com", "password": "password123"})).json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
    
        turns = [
            "Do not share your OTP.",
            "Never install remote access software.",
            "Bank employees never ask for your PIN.",
            "This is an example of a digital arrest scam.",
            "Learn how to protect yourself from scams.",
            "The bank refunded my money.",
            "My bank asked me to verify a transaction."
        ]
        
        print("\n--- HARD NEGATIVES TEST ---")
        for t in turns:
            sid = (await client.post(f"{BASE_URL}/api/sessions", json={"source": "test"}, headers=headers)).json()["id"]
            res = (await client.post(f"{BASE_URL}/api/sessions/{sid}/analyze", json={"transcript": t}, headers=headers)).json()
            print(f"[{res.get('risk_level', 'UNKNOWN'):8s}] Score: {res.get('risk_score', 0):3d} | Brake: {res.get('safety_brake_triggered')} | {t}")

async def main():
    await run_scam_progression()
    await run_hard_negatives()

if __name__ == "__main__":
    asyncio.run(main())
