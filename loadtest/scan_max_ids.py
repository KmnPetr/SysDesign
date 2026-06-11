#!/usr/bin/env python3
"""1000 раз: users/random -> messages/{chat_id}, ищем максимальные id."""

import json
import random
import sys
import urllib.error
import urllib.request

BASE_URL = "http://localhost:4200"
ITERATIONS = 1000


def get_json(url: str) -> dict:
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read())


def update_max_user(data: dict, max_user: int) -> int:
    user = data.get("user")
    if user and user.get("id") is not None:
        max_user = max(max_user, user["id"])
    for u in data.get("users") or []:
        if u.get("id") is not None:
            max_user = max(max_user, u["id"])
    return max_user


def update_max_chat(data: dict, max_chat: int) -> int:
    chat = data.get("chat")
    if chat and chat.get("id") is not None:
        max_chat = max(max_chat, chat["id"])
    for c in data.get("chats") or []:
        if c.get("id") is not None:
            max_chat = max(max_chat, c["id"])
    return max_chat


def update_max_user_chat(data: dict, max_uc_user: int, max_uc_chat: int) -> tuple[int, int]:
    for uc in data.get("user_chats") or []:
        uid = uc.get("id", {}).get("userId")
        cid = uc.get("id", {}).get("chatId")
        if uid is not None:
            max_uc_user = max(max_uc_user, uid)
        if cid is not None:
            max_uc_chat = max(max_uc_chat, cid)
    return max_uc_user, max_uc_chat


def update_max_message(data: dict, max_message: int) -> int:
    for m in data.get("messages") or []:
        if m.get("id") is not None:
            max_message = max(max_message, m["id"])
    return max_message


def main() -> None:
    base_url = sys.argv[1] if len(sys.argv) > 1 else BASE_URL
    iterations = int(sys.argv[2]) if len(sys.argv) > 2 else ITERATIONS

    max_user = 0
    max_chat = 0
    max_uc_user = 0
    max_uc_chat = 0
    max_message = 0
    ok = 0
    failed = 0

    for i in range(iterations):
        try:
            user_data = get_json(f"{base_url}/api/users/random")
            chats = user_data.get("chats") or []
            if not chats:
                failed += 1
                continue

            max_user = update_max_user(user_data, max_user)
            max_chat = update_max_chat(user_data, max_chat)
            max_uc_user, max_uc_chat = update_max_user_chat(user_data, max_uc_user, max_uc_chat)

            chat_id = random.choice(chats)["id"]
            msg_data = get_json(f"{base_url}/api/messages/{chat_id}")

            max_user = update_max_user(msg_data, max_user)
            max_chat = update_max_chat(msg_data, max_chat)
            max_uc_user, max_uc_chat = update_max_user_chat(msg_data, max_uc_user, max_uc_chat)
            max_message = update_max_message(msg_data, max_message)
            ok += 1
        except (urllib.error.URLError, urllib.error.HTTPError, KeyError, json.JSONDecodeError) as e:
            failed += 1
            if failed <= 5:
                print(f"[{i}] {e}", file=sys.stderr)

    print(f"base_url:   {base_url}")
    print(f"iterations: {iterations}, ok: {ok}, failed: {failed}")
    print(f"max user id:            {max_user}")
    print(f"max chat id:            {max_chat}")
    print(f"max user_chat user_id:  {max_uc_user}")
    print(f"max user_chat chat_id:  {max_uc_chat}")
    print(f"max message id:         {max_message}")


if __name__ == "__main__":
    main()
