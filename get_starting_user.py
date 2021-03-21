from sqlite3 import Connection
from typing import Optional


def get_starting_user(connection: Connection, default: str = None) -> Optional[str]:
    next_user = default

    with connection:
        found = connection.execute('select id from users where started = 1 and finished = 0 limit 1')
        rows = [x for x in found]
        if len(rows) > 0:
            next_user = rows[0][0]
        else:
            found = connection.execute('select id from users where started = 0 and finished = 0 limit 1')
            rows = [x for x in found]
            if len(rows) > 0:
                next_user = rows[0][0]

    return next_user
