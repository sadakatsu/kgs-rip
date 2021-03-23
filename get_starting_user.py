from sqlite3 import Connection
from typing import Optional


def get_starting_user(connection: Connection, default: str = None) -> Optional[str]:
    next_user = default
    last_year = 0
    last_month = 0

    with connection:
        found = connection.execute(
            'select id, last_year, last_month from users where started = 1 and finished = 0 limit 1'
        )
        rows = [x for x in found]
        if len(rows) > 0:
            next_user, last_year, last_month = rows[0]
        else:
            found = connection.execute(
                'select id, last_year, last_month from users where started = 0 and finished = 0 limit 1'
            )
            rows = [x for x in found]
            if len(rows) > 0:
                next_user, last_year, last_month = rows[0]

    return next_user, last_year, last_month
