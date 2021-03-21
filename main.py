from datetime import datetime
import sqlite3
import sys

from count_games import count_games
from get_starting_user import get_starting_user

connection = sqlite3.connect('kgs.db')

user = get_starting_user(connection)
while user:
    print(f'Consuming {user}...')

    today = datetime.today()
    for year in range(1999, today.year + 1):
        for month in range(1, 13):
            if year == today.year and month > today.month:
                break
            print(f'Looking for {user} in {year}-{month}...')


    count = count_games(connection, user)
    print(f'Finished consuming {user}.  This user has {count} viewable games.')

    raise Exception('foo!')
    try:
        with connection:
            connection.execute('update users set finished = 1 where id = ?', user)
    except Exception as e:
        print(e)
        sys.exit(1)


    user = get_starting_user(connection)

connection.close()