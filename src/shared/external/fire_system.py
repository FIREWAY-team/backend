from typing import Protocol
class FireSystemPort(Protocol):
    async def get_active_incidents(self) -> list[dict]: ...
class MockFireSystemAdapter:
    async def get_active_incidents(self) -> list[dict]:
        return []

