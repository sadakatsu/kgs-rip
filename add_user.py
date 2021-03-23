from sqlite3 import Connection


def add_user(connection: Connection, user: str):
    try:
        with connection:
            cursor = connection.cursor()
            cursor.execute('select * from users where id = ?', [user])
            result = cursor.fetchall()
            if not result:
                cursor.execute('insert into users (id) values (?)', [user])

                cursor.execute('select count(*) from users')
                count = cursor.fetchone()[0]

                print(f'Found new user {user}!  There are now {count} user{"" if count == 1 else "s"}.')
            cursor.close()
    finally:
        pass
