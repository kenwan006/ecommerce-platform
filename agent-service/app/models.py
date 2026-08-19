from pydantic import BaseModel


class AskRequest(BaseModel):
    message: str


class AskResponse(BaseModel):
    answer: str
