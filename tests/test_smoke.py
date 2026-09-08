def test_health(client):
    response = client.get("/health", headers={"X-Request-ID": "smoke-test"})
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.headers["X-Request-ID"] == "smoke-test"

def test_docs_and_mock_route(client):
    assert client.get("/docs").status_code == 200
    response = client.post("/route", json={"origin": {"lat": 37.44, "lon": 127.14}, "destination": {"lat": 37.45, "lon": 127.15}})
    assert response.status_code == 200
    assert response.json()["accessibility_probability"] == 0.87

