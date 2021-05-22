import os
import re
import requests
import sys
import time

from get_games import Game
from requests.exceptions import ConnectionError
from sqlite3 import Connection


def add_game(connection: Connection, game: Game):
    try:
        with connection:
            cursor = connection.cursor()

            cursor.execute('select * from games where id = ?', [game.id])
            result = cursor.fetchall()
            if not result:
                print(f'Found new game!  Downloading {game.id} ...')

                sgf = None
                failures = 0
                while not sgf:
                    try:
                        sgf = requests.get(game.id, allow_redirects=True)
                        if sgf.ok:
                            print('Downloaded SGF.')
                        else:
                            print(f'Could not download SGF: {sgf.status_code} - {sgf.reason}\n')
                            return
                    except (TimeoutError, ConnectionError):
                        print('SGF DOWNLOAD FAILED; SLEEPING BEFORE ATTEMPTING AGAIN')
                        failures += 1
                        time.sleep(0.5 * failures)

                match = re.match(
                    r'http://files.gokgs.com/games/(?P<year>\d+)/(?P<month>\d+)/(?P<day>\d+)/(?P<name>.*)\.sgf',
                    game.id
                )

                year = int(match.group('year'))
                year = f'{year:04d}'

                month = int(match.group('month'))
                month = f'{month:02d}'

                day = int(match.group('day'))
                day = f'{day:02d}'

                name = match.group('name')

                year_directory = f'games/{year}'
                month_directory = f'{year_directory}/{month}'
                day_directory = f'{month_directory}/{day}'
                filename = f'{day_directory}/{year}-{month}-{day}_{name}.sgf'

                if not os.path.isdir(year_directory):
                    os.mkdir(year_directory)
                if not os.path.isdir(month_directory):
                    os.mkdir(month_directory)
                if not os.path.isdir(day_directory):
                    os.mkdir(day_directory)

                with open(filename, 'wb') as outfile:
                    outfile.write(sgf.content)

                cursor.execute(
                    'insert into games (id, white, black, setup, start_time, type, result) values(?, ?, ?, ?, ?, ?, ?)',
                    [game.id, game.white, game.black, game.setup, game.start_time, game.game_type, game.result]
                )

                # cursor.execute('select count(*) from games')
                # count = cursor.fetchone()[0]
                # print(f'Game saved.  There are now {count} games.\n')
                print('Game saved.\n')

            cursor.close()
    finally:
        pass
