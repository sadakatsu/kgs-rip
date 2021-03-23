from sqlite3 import Connection


def update_user_progress(connection: Connection, user: str, last_year: int, last_month):
    try:
        with connection:
            cursor = connection.cursor()
            cursor.execute('update users set last_year = ?, last_month = ? where id = ?', [last_year, last_month, user])
            cursor.close()

            print(f'Finished {user} {last_year}-{last_month}.')
    finally:
        pass
