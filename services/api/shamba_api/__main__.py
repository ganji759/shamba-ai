import uvicorn

uvicorn.run("shamba_api.main:app_factory", factory=True, host="0.0.0.0", port=8080)  # noqa: S104
