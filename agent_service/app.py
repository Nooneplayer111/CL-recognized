"""Text chat for one animal at a time. Images never go to the model."""

from __future__ import annotations

import logging
import os
from pathlib import Path

from fastapi import FastAPI, Header, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from openai import OpenAI
from pydantic import BaseModel

from animals import get_animal, system_prompt
from store import Store

LOGGER = logging.getLogger("agent_service")
BASE_DIR = Path(__file__).resolve().parent
MAX_TEXT = 2000


def load_env(path: Path) -> None:
    if not path.exists():
        return
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


load_env(BASE_DIR / ".env")
logging.basicConfig(level=logging.INFO)

app = FastAPI()
store = Store(BASE_DIR / "chat.sqlite")


class SessionIn(BaseModel):
    animalId: str


class MessageIn(BaseModel):
    text: str = ""


def ok(data: object) -> dict[str, object]:
    return {"code": 1, "msg": None, "data": data}


def fail(msg: str, status: int = 400) -> JSONResponse:
    return JSONResponse(status_code=status, content={"code": 0, "msg": msg, "data": None})


def visitor_token(authorization: str | None) -> str | None:
    if not authorization:
        return None
    scheme, _, token = authorization.partition(" ")
    if scheme.lower() == "bearer" and token.strip():
        return token.strip()
    return authorization.strip() or None


def model_client() -> OpenAI:
    api_key = os.environ.get("DASHSCOPE_API_KEY", "")
    base_url = os.environ.get("BAILIAN_BASE_URL", "")
    if not api_key or not base_url:
        raise RuntimeError("missing model config")
    return OpenAI(api_key=api_key, base_url=base_url, timeout=30.0, max_retries=1)


def model_error_message(error: Exception) -> str:
    name = type(error).__name__
    if "Timeout" in name:
        return "模型响应超时，请再试一次"
    if "Authentication" in name or "Permission" in name:
        return "模型服务鉴权失败"
    if "Connection" in name or "Connect" in name:
        return "暂时连不上模型服务"
    return "模型暂时没有完成回答"


@app.exception_handler(RequestValidationError)
def invalid_body(_: Request, __: RequestValidationError) -> JSONResponse:
    return fail("请求格式不正确")


@app.get("/v1/chat/animals/{animal_id}", response_model=None)
def read_animal(animal_id: str):
    animal = get_animal(animal_id)
    if animal is None:
        return fail("没有这种动物", 404)
    return ok(animal)


@app.post("/v1/chat/sessions", response_model=None)
def create_session(
    body: SessionIn,
    authorization: str | None = Header(default=None),
):
    if get_animal(body.animalId) is None:
        return fail("没有这种动物", 404)
    session = store.create_session(body.animalId, visitor_token(authorization))
    return ok({"sessionId": session["sessionId"], "animalId": session["animalId"]})


@app.get("/v1/chat/sessions", response_model=None)
def list_sessions(animalId: str = ""):
    if get_animal(animalId) is None:
        return fail("没有这种动物", 404)
    return ok({"items": store.list_sessions(animalId)})


@app.get("/v1/chat/sessions/{session_id}/messages", response_model=None)
def read_messages(session_id: str):
    session = store.get_session(session_id)
    if session is None:
        return fail("对话不存在", 404)
    return ok(
        {
            "sessionId": session["sessionId"],
            "animalId": session["animalId"],
            "messages": store.list_messages(session_id),
        }
    )


@app.post("/v1/chat/sessions/{session_id}/messages", response_model=None)
def create_message(session_id: str, body: MessageIn):
    session = store.get_session(session_id)
    if session is None:
        return fail("对话不存在", 404)
    text = body.text.strip()
    if not text:
        return fail("请输入内容")
    if len(text) > MAX_TEXT:
        return fail("这句话太长了，请缩短到 2000 字以内")

    store.add_message(session_id, "user", text)
    history = store.recent_messages(session_id, 20)
    messages = [{"role": "system", "content": system_prompt(session["animalId"])}, *history]
    try:
        completion = model_client().chat.completions.create(
            model=os.getenv("BAILIAN_CHAT_MODEL", "qwen3.8-flash"),
            temperature=0.3,
            messages=messages,
        )
        reply = (completion.choices[0].message.content or "").strip()
    except RuntimeError:
        LOGGER.exception("model config missing")
        return fail("模型服务还没有配置", 500)
    except Exception as error:
        LOGGER.exception("model call failed")
        return fail(model_error_message(error), 502)
    if not reply:
        return fail("模型没有返回内容", 502)
    saved = store.add_message(session_id, "assistant", reply)
    return ok({"messageId": saved["id"], "reply": reply})
