import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    openai_api_key: str
    spring_api_url: str


settings = Settings(
    openai_api_key=os.getenv("OPENAI_API_KEY", ""),
    spring_api_url=os.getenv("SPRING_API_URL", "http://localhost:8080"),
)
