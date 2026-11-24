"""SQLite sessions and messages for the animal chat service."""

from __future__ import annotations

import sqlite3
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

SHANGHAI = timezone(timedelta(hours=8))


def now_iso() -> str:
    return datetime.now(SHANGHAI).isoformat(timespec="seconds")


def session_title(text: str | None) -> str:
    if not text:
        return "新的对话"
    compact = " ".join(text.split())
    if len(compact) <= 40:
        return compact
    return compact[:40] + "…"


class Store:
    def __init__(self, path: Path) -> None:
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self._connect() as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS chat_session (
                  id            TEXT PRIMARY KEY,
                  animal_id     TEXT NOT NULL,
                  visitor_token TEXT,
                  created_at    TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS chat_message (
                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  session_id  TEXT NOT NULL,
                  role        TEXT NOT NULL,
                  content     TEXT NOT NULL,
                  created_at  TEXT NOT NULL
                );
                """
            )

    def _connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.path)
        conn.row_factory = sqlite3.Row
        return conn

    def create_session(self, animal_id: str, visitor_token: str | None) -> dict[str, str]:
        session_id = uuid.uuid4().hex
        created_at = now_iso()
        with self._connect() as conn:
            conn.execute(
                "INSERT INTO chat_session (id, animal_id, visitor_token, created_at) VALUES (?, ?, ?, ?)",
                (session_id, animal_id, visitor_token or None, created_at),
            )
        return {"sessionId": session_id, "animalId": animal_id, "createdAt": created_at}

    def get_session(self, session_id: str) -> dict[str, str] | None:
        with self._connect() as conn:
            row = conn.execute(
                "SELECT id, animal_id, created_at FROM chat_session WHERE id = ?",
                (session_id,),
            ).fetchone()
        if row is None:
            return None
        return {"sessionId": row["id"], "animalId": row["animal_id"], "createdAt": row["created_at"]}

    def list_sessions(self, animal_id: str) -> list[dict[str, str]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT s.id, s.animal_id, s.created_at,
                  (
                    SELECT m.content FROM chat_message m
                    WHERE m.session_id = s.id AND m.role = 'user'
                    ORDER BY m.id ASC LIMIT 1
                  ) AS title
                FROM chat_session s
                WHERE s.animal_id = ?
                ORDER BY s.created_at DESC, s.id DESC
                """,
                (animal_id,),
            ).fetchall()
        return [
            {
                "sessionId": row["id"],
                "animalId": row["animal_id"],
                "title": session_title(row["title"]),
                "createdAt": row["created_at"],
            }
            for row in rows
        ]

    def add_message(self, session_id: str, role: str, content: str) -> dict[str, object]:
        created_at = now_iso()
        with self._connect() as conn:
            cursor = conn.execute(
                "INSERT INTO chat_message (session_id, role, content, created_at) VALUES (?, ?, ?, ?)",
                (session_id, role, content, created_at),
            )
            message_id = int(cursor.lastrowid)
        return {"id": message_id, "role": role, "content": content, "createdAt": created_at}

    def list_messages(self, session_id: str) -> list[dict[str, object]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT id, role, content, created_at
                FROM chat_message
                WHERE session_id = ?
                ORDER BY id ASC
                """,
                (session_id,),
            ).fetchall()
        return [
            {
                "id": row["id"],
                "role": row["role"],
                "content": row["content"],
                "createdAt": row["created_at"],
            }
            for row in rows
        ]

    def recent_messages(self, session_id: str, limit: int = 20) -> list[dict[str, str]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT role, content FROM chat_message
                WHERE session_id = ?
                ORDER BY id DESC
                LIMIT ?
                """,
                (session_id, limit),
            ).fetchall()
        return [{"role": row["role"], "content": row["content"]} for row in reversed(rows)]
