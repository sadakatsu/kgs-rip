from sqlite3 import Connection


def count_games(connection: Connection, user: str) -> int:
    count = 0

    with connection:
        cursor = connection.cursor()
        cursor.execute('select count(id) from games where white = ?', [user])
        count += cursor.fetchone()[0]

        cursor.execute('select count(id) from games where black = ?', [user])
        count += cursor.fetchone()[0]

    return count
