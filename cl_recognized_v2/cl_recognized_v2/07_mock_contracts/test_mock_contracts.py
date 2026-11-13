"""Offline schema sanity checks for fictional JSON fixtures (no network)."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def read_json(name: str) -> dict:
    return json.loads((ROOT / name).read_text(encoding="utf-8"))


class TestMockContracts(unittest.TestCase):
    def test_recognition_event_order_and_profile(self):
        obj = read_json("recognition_session_mock.json")
        ev = obj["events"]
        ids = [item["eventId"] for item in ev]
        self.assertEqual(ids, sorted(ids))
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(ev[0]["payload"]["stage"], "species")
        self.assertEqual(ev[-1]["type"], "done")
        self.assertTrue(ev[-1]["payload"]["eligibleForCheckin"])
        by_type = {item["type"]: item for item in ev if item["type"] != "stage"}
        self.assertEqual(by_type["individual"]["payload"]["individualId"],
                         by_type["profile"]["payload"]["animalCard"]["individualId"])
        self.assertEqual(obj["restFallback"]["data"]["recognitionJobId"], obj["recognitionJobId"])
        self.assertEqual(by_type["profile"]["payload"]["animalCard"]["ageDisplay"], "年龄暂未核实")

    def test_redeem_math_and_idempotency_key(self):
        obj = read_json("checkin_and_mall_mock.json")
        checkin = obj["checkin"]["response"]["data"]
        good = obj["mall"]["product"]
        redeem = obj["mall"]["redeemResponse"]["data"]
        self.assertEqual(checkin["awardedPoints"], 50)
        self.assertEqual(redeem["walletBalance"], checkin["walletBalance"] - good["pricePoints"])
        self.assertEqual(obj["mall"]["redeemRequest"]["requestId"], redeem["requestId"])
        self.assertGreater(good["stock"], 0)

    def test_no_credentials_in_mock_fixtures(self):
        for filename in ("recognition_session_mock.json", "checkin_and_mall_mock.json"):
            obj = read_json(filename)
            serialized = json.dumps(obj, ensure_ascii=False).lower()
            for forbidden in ("das hs cope", "password", "secret_key", "access_token", "jwt="):
                self.assertNotIn(forbidden, serialized)


if __name__ == "__main__":
    unittest.main(verbosity=2)
