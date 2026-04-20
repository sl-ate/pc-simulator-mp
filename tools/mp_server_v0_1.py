#!/usr/bin/env python3
from __future__ import annotations

import argparse
import asyncio
import json
import signal
import time
import uuid
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Optional


@dataclass
class PlayerRecord:
    player_id: str
    name: str
    platform: str
    state: Dict[str, Any] = field(default_factory=dict)
    world_id: int = 1
    money: float = 0.0
    bitcoin: float = 0.0
    inventory: Dict[str, Any] = field(default_factory=dict)
    last_seen: float = field(default_factory=time.time)
    online: bool = False


class MpServerV01:
    def __init__(
        self,
        host: str,
        port: int,
        save_file: Path,
        tick_rate: float,
        default_world: int,
        default_money: float,
        default_bitcoin: float,
        max_slots_per_world: int,
        force_online: bool,
        hide_offline_ui: bool,
    ) -> None:
        self.host = host
        self.port = port
        self.save_file = save_file
        self.tick_rate = tick_rate

        self.default_world = default_world
        self.default_money = default_money
        self.default_bitcoin = default_bitcoin
        self.max_slots_per_world = max(1, min(64, int(max_slots_per_world)))
        self.force_online = force_online
        self.hide_offline_ui = hide_offline_ui

        self.players: Dict[str, PlayerRecord] = {}
        self.clients: Dict[asyncio.StreamWriter, str] = {}
        self.state_lock = asyncio.Lock()

    async def start(self) -> None:
        self._load_state()

        server = await asyncio.start_server(self._handle_client, self.host, self.port)
        addrs = ", ".join(str(sock.getsockname()) for sock in (server.sockets or []))
        print(f"[MP 0.1] Listening on {addrs}")
        print(f"[MP 0.1] Save file: {self.save_file}")

        world_task = asyncio.create_task(self._world_tick_loop(), name="world-tick")

        stop_event = asyncio.Event()

        def _request_stop() -> None:
            stop_event.set()

        loop = asyncio.get_running_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            try:
                loop.add_signal_handler(sig, _request_stop)
            except NotImplementedError:
                pass

        async with server:
            serve_task = asyncio.create_task(server.serve_forever(), name="serve-forever")
            await stop_event.wait()
            serve_task.cancel()
            try:
                await serve_task
            except asyncio.CancelledError:
                pass

        world_task.cancel()
        try:
            await world_task
        except asyncio.CancelledError:
            pass

        async with self.state_lock:
            self._save_state_locked()

        print("[MP 0.1] Server stopped.")

    async def _handle_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        peer = writer.get_extra_info("peername")
        session_tag = str(uuid.uuid4())[:8]
        player_id: Optional[str] = None
        print(f"[MP 0.1] + connection {peer} session={session_tag}")

        try:
            await self._send(writer, {"type": "server_hello", "version": "0.1", "session": session_tag})

            while True:
                raw = await reader.readline()
                if not raw:
                    break

                line = raw.decode("utf-8", errors="ignore").strip()
                if not line:
                    continue

                try:
                    msg = json.loads(line)
                except json.JSONDecodeError:
                    await self._send(writer, {"type": "error", "message": "invalid_json"})
                    continue

                packet_type = _safe_text(msg.get("type")).lower()
                if packet_type == "hello":
                    accepted_player = await self._handle_hello(writer, msg)
                    if accepted_player is not None:
                        player_id = accepted_player
                    continue

                if player_id is None:
                    await self._send(writer, {"type": "error", "message": "send_hello_first"})
                    continue

                if packet_type in {"player_state", "state", "snapshot"}:
                    await self._handle_state(player_id, msg)
                    continue

                if packet_type == "set_name":
                    await self._handle_set_name(player_id, msg)
                    continue

                if packet_type == "set_economy":
                    await self._handle_set_economy(player_id, msg)
                    continue

                if packet_type == "set_profile":
                    await self._handle_set_profile(writer, player_id, msg)
                    continue

                if packet_type == "request_session":
                    await self._handle_request_session(writer, player_id)
                    continue

                if packet_type == "enter_world":
                    await self._handle_enter_world(writer, player_id, msg)
                    continue

                if packet_type == "ping":
                    await self._send(writer, {"type": "pong", "time": int(time.time() * 1000)})
                    continue

                await self._send(writer, {"type": "error", "message": "unknown_packet", "packet": packet_type})
        except (ConnectionResetError, BrokenPipeError):
            pass
        finally:
            await self._disconnect(writer)
            print(f"[MP 0.1] - connection {peer} session={session_tag}")

    async def _handle_hello(self, writer: asyncio.StreamWriter, msg: Dict[str, Any]) -> Optional[str]:
        requested_id = _safe_text(msg.get("playerId")) or _safe_text(msg.get("name"))
        if not requested_id:
            requested_id = f"guest-{int(time.time())}"

        player_id = requested_id[:48]
        name = (_safe_text(msg.get("name")) or player_id)[:32]
        platform = (_safe_text(msg.get("platform")) or "unknown")[:32]
        reject_payload: Optional[Dict[str, Any]] = None

        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                record = PlayerRecord(
                    player_id=player_id,
                    name=name,
                    platform=platform,
                    world_id=self.default_world,
                    money=self.default_money,
                    bitcoin=self.default_bitcoin,
                )
                self.players[player_id] = record

            target_world = max(1, record.world_id)
            world_online = self._count_online_in_world_locked(target_world, exclude_player=player_id)
            if world_online >= self.max_slots_per_world:
                reject_payload = {
                    "type": "error",
                    "message": "world_full",
                    "worldId": target_world,
                    "maxSlots": self.max_slots_per_world,
                    "onlineCount": world_online,
                }
            else:
                record.world_id = target_world
                record.name = name
                record.platform = platform
                record.online = True
                record.last_seen = time.time()
                self.clients[writer] = player_id
                session_payload = self._build_session_payload(record)
                world_payload = self._build_world_entered_payload(record)
                self._save_state_locked()

        if reject_payload is not None:
            await self._send(writer, reject_payload)
            print(
                f"[MP 0.1] reject hello player={player_id} world={reject_payload['worldId']} "
                f"full {reject_payload['onlineCount']}/{reject_payload['maxSlots']}"
            )
            return None

        await self._send(
            writer,
            {
                "type": "hello_ack",
                "playerId": player_id,
                "name": name,
                "platform": platform,
                "online": True,
                "session": session_payload,
            },
        )
        await self._send(writer, {"type": "session_config", "session": session_payload})
        await self._send(writer, world_payload)

        await self._broadcast(
            {
                "type": "player_joined",
                "playerId": player_id,
                "name": name,
                "worldId": record.world_id,
            },
            except_writer=None,
        )

        print(
            f"[MP 0.1] hello player={player_id} name={name} "
            f"world={record.world_id} money={record.money:.0f} btc={record.bitcoin:.6f}"
        )
        return player_id

    async def _handle_state(self, player_id: str, msg: Dict[str, Any]) -> None:
        body = msg.get("body", {})
        if not isinstance(body, dict):
            body = {"raw": body}

        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                return

            record.state = body
            record.last_seen = time.time()
            record.online = True

            economy = body.get("economy")
            if isinstance(economy, dict):
                record.money = _safe_float(economy.get("money"), record.money)
                record.bitcoin = _safe_float(economy.get("bitcoin"), record.bitcoin)

            if "worldId" in body:
                record.world_id = max(1, _safe_int(body.get("worldId"), record.world_id))

            self._save_state_locked()

    async def _handle_set_name(self, player_id: str, msg: Dict[str, Any]) -> None:
        new_name = _safe_text(msg.get("name"))[:32]
        if not new_name:
            return

        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                return

            record.name = new_name
            record.last_seen = time.time()
            self._save_state_locked()

        await self._broadcast(
            {"type": "player_renamed", "playerId": player_id, "name": new_name},
            except_writer=None,
        )

    async def _handle_set_economy(self, player_id: str, msg: Dict[str, Any]) -> None:
        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                return

            record.money = _safe_float(msg.get("money"), record.money)
            record.bitcoin = _safe_float(msg.get("bitcoin"), record.bitcoin)
            record.last_seen = time.time()
            session_payload = self._build_session_payload(record)
            self._save_state_locked()

        await self._broadcast({"type": "session_config", "session": session_payload}, except_writer=None)

    async def _handle_set_profile(self, writer: asyncio.StreamWriter, player_id: str, msg: Dict[str, Any]) -> None:
        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                return

            record.money = _safe_float(msg.get("money"), record.money)
            record.bitcoin = _safe_float(msg.get("bitcoin"), record.bitcoin)

            inventory = msg.get("inventory")
            if isinstance(inventory, dict):
                record.inventory = inventory

            record.last_seen = time.time()
            session_payload = self._build_session_payload(record)
            self._save_state_locked()

            inv_count = 0
            if isinstance(record.inventory.get("spawnCounts"), dict):
                inv_count = len(record.inventory["spawnCounts"])
            unlocked_count = 0
            if isinstance(record.inventory.get("unlocked"), list):
                unlocked_count = len(record.inventory["unlocked"])

        print(
            f"[MP 0.1] profile sync player={player_id} world={record.world_id} "
            f"money={record.money:.0f} btc={record.bitcoin:.6f} "
            f"spawnCounts={inv_count} unlocked={unlocked_count}"
        )

        await self._send(writer, {"type": "session_config", "session": session_payload})

    async def _handle_request_session(self, writer: asyncio.StreamWriter, player_id: str) -> None:
        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                return
            session_payload = self._build_session_payload(record)
            world_payload = self._build_world_entered_payload(record)

        await self._send(writer, {"type": "session_config", "session": session_payload})
        await self._send(writer, world_payload)

    async def _handle_enter_world(self, writer: asyncio.StreamWriter, player_id: str, msg: Dict[str, Any]) -> None:
        reject_payload: Optional[Dict[str, Any]] = None
        async with self.state_lock:
            record = self.players.get(player_id)
            if record is None:
                return

            requested_world = _safe_int(msg.get("worldId"), record.world_id)
            target_world = max(1, requested_world)
            world_online = self._count_online_in_world_locked(target_world, exclude_player=player_id)
            if world_online >= self.max_slots_per_world:
                reject_payload = {
                    "type": "error",
                    "message": "world_full",
                    "worldId": target_world,
                    "maxSlots": self.max_slots_per_world,
                    "onlineCount": world_online,
                }
            else:
                record.world_id = target_world
                record.last_seen = time.time()
                world_payload = self._build_world_entered_payload(record)
                self._save_state_locked()

        if reject_payload is not None:
            await self._send(writer, reject_payload)
            return

        await self._send(writer, world_payload)

    async def _world_tick_loop(self) -> None:
        interval = 1.0 / self.tick_rate if self.tick_rate > 0 else 0.1
        while True:
            await asyncio.sleep(interval)
            packet = await self._build_world_state_packet()
            await self._broadcast(packet, except_writer=None)

    async def _build_world_state_packet(self) -> Dict[str, Any]:
        async with self.state_lock:
            players = [
                {
                    "playerId": rec.player_id,
                    "name": rec.name,
                    "platform": rec.platform,
                    "online": rec.online,
                    "updatedAt": int(rec.last_seen * 1000),
                    "worldId": rec.world_id,
                    "money": rec.money,
                    "bitcoin": rec.bitcoin,
                    "state": rec.state,
                }
                for rec in self.players.values()
                if rec.online
            ]

        return {
            "type": "world_state",
            "serverTime": int(time.time() * 1000),
            "players": players,
        }

    async def _broadcast(self, packet: Dict[str, Any], except_writer: Optional[asyncio.StreamWriter]) -> None:
        stale = []
        for writer in list(self.clients.keys()):
            if writer is except_writer:
                continue
            try:
                await self._send(writer, packet)
            except Exception:
                stale.append(writer)

        for writer in stale:
            await self._disconnect(writer, announce=False)

    async def _send(self, writer: asyncio.StreamWriter, packet: Dict[str, Any]) -> None:
        line = json.dumps(packet, ensure_ascii=False, separators=(",", ":")) + "\n"
        writer.write(line.encode("utf-8"))
        await writer.drain()

    async def _disconnect(self, writer: asyncio.StreamWriter, announce: bool = True) -> None:
        player_id: Optional[str]

        async with self.state_lock:
            player_id = self.clients.pop(writer, None)
            if player_id is not None:
                record = self.players.get(player_id)
                if record is not None:
                    record.online = False
                    record.last_seen = time.time()
                self._save_state_locked()

        if announce and player_id is not None:
            await self._broadcast({"type": "player_left", "playerId": player_id}, except_writer=writer)

        if not writer.is_closing():
            writer.close()
            try:
                await writer.wait_closed()
            except Exception:
                pass

    def _build_session_payload(self, record: PlayerRecord) -> Dict[str, Any]:
        return {
            "mode": "online",
            "version": "0.1",
            "forceOnline": self.force_online,
            "hideOfflineUi": self.hide_offline_ui,
            "maxSlotsPerWorld": self.max_slots_per_world,
            "worldId": record.world_id,
            "currency": {
                "money": record.money,
                "bitcoin": record.bitcoin,
            },
            "inventory": record.inventory,
        }

    def _count_online_in_world_locked(self, world_id: int, exclude_player: Optional[str] = None) -> int:
        count = 0
        for pid, rec in self.players.items():
            if exclude_player is not None and pid == exclude_player:
                continue
            if rec.online and rec.world_id == world_id:
                count += 1
        return count

    @staticmethod
    def _build_world_entered_payload(record: PlayerRecord) -> Dict[str, Any]:
        return {
            "type": "world_entered",
            "worldId": record.world_id,
            "money": record.money,
            "bitcoin": record.bitcoin,
        }

    def _load_state(self) -> None:
        if not self.save_file.exists():
            return

        try:
            data = json.loads(self.save_file.read_text(encoding="utf-8"))
        except Exception as exc:
            print(f"[MP 0.1] Failed to load state file: {exc}")
            return

        players = data.get("players", {})
        if not isinstance(players, dict):
            return

        for player_id, raw in players.items():
            if not isinstance(raw, dict):
                continue

            record = PlayerRecord(
                player_id=str(player_id),
                name=_safe_text(raw.get("name")) or str(player_id),
                platform=_safe_text(raw.get("platform")) or "unknown",
                state=raw.get("state") if isinstance(raw.get("state"), dict) else {},
                world_id=max(1, _safe_int(raw.get("world_id"), self.default_world)),
                money=_safe_float(raw.get("money"), self.default_money),
                bitcoin=_safe_float(raw.get("bitcoin"), self.default_bitcoin),
                inventory=raw.get("inventory") if isinstance(raw.get("inventory"), dict) else {},
                last_seen=float(raw.get("last_seen", time.time())),
                online=False,
            )
            self.players[record.player_id] = record

        print(f"[MP 0.1] Loaded {len(self.players)} player records from disk")

    def _save_state_locked(self) -> None:
        self.save_file.parent.mkdir(parents=True, exist_ok=True)

        data = {
            "version": "0.1",
            "savedAt": int(time.time() * 1000),
            "players": {
                player_id: {
                    "name": rec.name,
                    "platform": rec.platform,
                    "state": rec.state,
                    "world_id": rec.world_id,
                    "money": rec.money,
                    "bitcoin": rec.bitcoin,
                    "inventory": rec.inventory,
                    "last_seen": rec.last_seen,
                }
                for player_id, rec in self.players.items()
            },
        }

        tmp = self.save_file.with_suffix(self.save_file.suffix + ".tmp")
        tmp.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        tmp.replace(self.save_file)


def _safe_text(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


def _safe_int(value: Any, default: int) -> int:
    try:
        return int(value)
    except Exception:
        return default


def _safe_float(value: Any, default: float) -> float:
    try:
        parsed = float(value)
    except Exception:
        return default
    if parsed != parsed:
        return default
    return parsed


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="PC Simulator MP server v0.1 (TCP + JSON lines)")
    parser.add_argument("--host", default="0.0.0.0", help="Bind host (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=27015, help="Bind port (default: 27015)")
    parser.add_argument(
        "--save",
        default="tools/mp_state_v0_1.json",
        help="Path to persistent state file (default: tools/mp_state_v0_1.json)",
    )
    parser.add_argument("--tick-rate", type=float, default=10.0, help="World broadcast rate in Hz (default: 10)")
    parser.add_argument("--default-world", type=int, default=1, help="Default world id for new profiles")
    parser.add_argument("--default-money", type=float, default=2500.0, help="Default money for new profiles")
    parser.add_argument("--default-bitcoin", type=float, default=0.15, help="Default BTC for new profiles")
    parser.add_argument(
        "--max-slots-per-world",
        type=int,
        default=32,
        help="Max online players per world (1..64, default: 32)",
    )
    parser.add_argument("--allow-offline", action="store_true", help="Do not force online mode")
    parser.add_argument("--show-offline-ui", action="store_true", help="Do not hide offline UI on client gate")
    return parser.parse_args()


async def _main() -> None:
    args = parse_args()
    server = MpServerV01(
        host=args.host,
        port=args.port,
        save_file=Path(args.save),
        tick_rate=args.tick_rate,
        default_world=max(1, args.default_world),
        default_money=args.default_money,
        default_bitcoin=args.default_bitcoin,
        max_slots_per_world=max(1, min(64, int(args.max_slots_per_world))),
        force_online=not args.allow_offline,
        hide_offline_ui=not args.show_offline_ui,
    )
    await server.start()


if __name__ == "__main__":
    try:
        asyncio.run(_main())
    except KeyboardInterrupt:
        pass
