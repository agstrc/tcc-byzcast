from io import StringIO
import unittest

from log import ParsedLog, find_cycles, map_requests, parse_log_file


class FindCyclesTest(unittest.TestCase):
    def test_find_no_cycles(self):
        mapping = {
            "1": ["2", "3"],
            "2": ["3"],
            "3": ["4"],
        }

        self.assertEqual(find_cycles(mapping), [])

    def test_find_cycle(self):
        mapping = {
            "1": ["2"],
            "2": ["3"],
            "3": ["1"],
        }

        self.assertEqual(find_cycles(mapping), [["1", "2", "3", "1"]])


class ParseLogFileTest(unittest.TestCase):
    def test_parse_log_file(self):
        log_content = r"""2024-08-14T21:16:22.243618409 dev.agst.byzcast.replica.RequestHandler:83 INFO GID=2 SID=1 RID=e4399c01-fe39-44de-8af7-991269c44c94 source=REPLICA Request has reached minimum receive count
2024-08-14T21:16:22.243746502 dev.agst.byzcast.replica.RequestHandler:111 INFO GID=2 SID=1 RID=e4399c01-fe39-44de-8af7-991269c44c94 source=REPLICA Request locally handled
2024-08-14T21:16:22.244065592 dev.agst.byzcast.replica.RequestHandler:72 INFO GID=2 SID=1 RID=e4399c01-fe39-44de-8af7-991269c44c94 source=REPLICA Response is cached
2024-08-14T21:16:22.440327292 dev.agst.byzcast.replica.RequestHandler:83 INFO GID=2 SID=1 RID=7f2ccf47-4b23-440e-8208-b01da770456f source=REPLICA Request has reached minimum receive count
2024-08-14T21:16:22.440541711 dev.agst.byzcast.replica.RequestHandler:111 INFO GID=2 SID=1 RID=7f2ccf47-4b23-440e-8208-b01da770456f source=REPLICA Request locally handled
2024-08-14T21:16:22.441230468 dev.agst.byzcast.replica.RequestHandler:72 INFO GID=2 SID=1 RID=7f2ccf47-4b23-440e-8208-b01da770456f source=REPLICA Response is cached
2024-08-14T21:16:22.615582747 dev.agst.byzcast.replica.RequestHandler:83 INFO GID=2 SID=1 RID=4148c444-b0d7-478d-bde8-8d312b602503 source=REPLICA Request has reached minimum receive count
2024-08-14T21:16:22.616635662 dev.agst.byzcast.replica.RequestHandler:111 INFO GID=2 SID=1 RID=4148c444-b0d7-478d-bde8-8d312b602503 source=REPLICA Request locally handled
2024-08-14T21:16:22.617721607 dev.agst.byzcast.replica.RequestHandler:72 INFO GID=2 SID=1 RID=4148c444-b0d7-478d-bde8-8d312b602503 source=REPLICA Response is cached
2024-08-14T21:16:22.795376185 dev.agst.byzcast.replica.RequestHandler:83 INFO GID=2 SID=1 RID=6dae56d6-e066-4386-8b55-7bbbd258e43f source=REPLICA Request has reached minimum receive count
2024-08-14T21:16:22.79550553 dev.agst.byzcast.replica.RequestHandler:111 INFO GID=2 SID=1 RID=6dae56d6-e066-4386-8b55-7bbbd258e43f source=REPLICA Request locally handled
2024-08-14T21:16:22.802906352 dev.agst.byzcast.replica.RequestHandler:72 INFO GID=2 SID=1 RID=6dae56d6-e066-4386-8b55-7bbbd258e43f source=REPLICA Response is cached
2024-08-14T21:16:23.00022843 dev.agst.byzcast.replica.RequestHandler:83 INFO GID=2 SID=1 RID=c5070ab8-a983-4aba-84c7-271db6304f49 source=REPLICA Request has reached minimum receive count
2024-08-14T21:16:23.000376858 dev.agst.byzcast.replica.RequestHandler:111 INFO GID=2 SID=1 RID=c5070ab8-a983-4aba-84c7-271db6304f49 source=REPLICA Request locally handled
2024-08-14T21:16:23.001205636 dev.agst.byzcast.replica.RequestHandler:72 INFO GID=2 SID=1 RID=c5070ab8-a983-4aba-84c7-271db6304f49 source=REPLICA Response is cached
2024-08-14T21:16:23.126568703 dev.agst.byzcast.replica.RequestHandler:83 INFO GID=2 SID=1 RID=c229293d-9409-4031-a5c4-d704bca7d4e2 source=REPLICA Request has reached minimum receive count
2024-08-14T21:16:23.126747577 dev.agst.byzcast.replica.RequestHandler:111 INFO GID=2 SID=1 RID=c229293d-9409-4031-a5c4-d704bca7d4e2 source=REPLICA Request locally handled
2024-08-14T21:16:23.127317525 dev.agst.byzcast.replica.RequestHandler:72 INFO GID=2 SID=1 RID=c229293d-9409-4031-a5c4-d704bca7d4e2 source=REPLICA Response is cached
2024-08-14T21:16:23.169493677 dev.agst.byzcast.replica.RequestHandler:64 INFO GID=2 SID=1 RID=2516f461-642e-4cab-af05-f8feb1c7ef50 Request is client request"""
        log = StringIO(log_content)
        expected_requests = [
            "e4399c01-fe39-44de-8af7-991269c44c94",
            "7f2ccf47-4b23-440e-8208-b01da770456f",
            "4148c444-b0d7-478d-bde8-8d312b602503",
            "6dae56d6-e066-4386-8b55-7bbbd258e43f",
            "c5070ab8-a983-4aba-84c7-271db6304f49",
            "c229293d-9409-4031-a5c4-d704bca7d4e2",
        ]

        self.assertEqual(parse_log_file(log), expected_requests)


class TestMapMessages(unittest.TestCase):
    def test_map_messages(self):
        parsed_logs = [
            ParsedLog("", ["1", "2", "3", "4"]),
            ParsedLog("", ["1", "2", "4", "6"]),
            ParsedLog("", ["1", "2", "3", "4"]),
        ]
        expected = {"1": ["2"], "2": ["3", "4"], "3": ["4"], "4": ["6"]}

        self.assertEqual(map_requests(parsed_logs), expected)


if __name__ == "__main__":
    unittest.main()
