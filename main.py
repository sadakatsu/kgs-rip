from datetime import datetime
import sqlite3
import sys

from add_game import add_game
from add_user import add_user
from count_games import count_games
from get_games import get_games
from get_starting_user import get_starting_user
from sqlite3 import Connection
from update_user_progress import update_user_progress


def main(connection: Connection):
    user, last_year, last_month = get_starting_user(connection)
    while user:
        print(f'Consuming {user}...')

        try:
            with connection:
                connection.execute('update users set started = 1 where id = ?', [user])
        except Exception as e:
            print(e)
            sys.exit(1)

        today = datetime.today()
        starting_year = last_year or 1999
        for year in range(starting_year, today.year + 1):
            starting_month = 1 if (year > last_year or not last_month) else last_month
            for month in range(starting_month, 13):
                if year == today.year and month > today.month:
                    break
                print(f'Looking for {user} in {year}-{month}...')

                games = get_games(user, year, month)
                for game in games:
                    add_user(connection, game.black)
                    add_user(connection, game.white)

                    if game.id:
                        add_game(connection, game)

                update_user_progress(connection, user, year, month)

                print()

        count = count_games(connection, user)
        print(f'Finished consuming {user}.  This user has {count} viewable games.')

        try:
            with connection:
                connection.execute('update users set finished = 1 where id = ?', [user])
        except Exception as e:
            print(e)
            sys.exit(1)

        user, last_year, last_month = get_starting_user(connection)

    connection.close()

    print('Done!')


if __name__ == '__main__':
    initial_user = sys.argv[1].strip()

    try:
        with sqlite3.connect('kgs.db') as connection:
            add_user(connection, initial_user)
            main(connection)
    finally:
        pass
