import requests
import sys
import re

from get_games import Game
from sqlite3 import Connection


def add_game(connection: Connection, game: Game):
    try:
        with connection:
            cursor = connection.cursor()

            cursor.execute('select * from games where id = ?', [game.id])
            result = cursor.fetchall()
            if not result:
                print(f'Found new game!  Downloading {game.id} ...')
                sgf = requests.get(game.id, allow_redirects=True)

                match = re.match(
                    r'http://files.gokgs.com/games/(?P<year>\d+)/(?P<month>\d+)/(?P<day>\d+)/(?P<name>.*)\.sgf',
                    game.id
                )
                year = int(match.group('year'))
                month = int(match.group('month'))
                day = int(match.group('day'))
                name = match.group('name')
                filename = f'{year:0000d}-{month:00d}-{day:00d}_{name}.sgf'

                with open(f'games/{filename}', 'wb') as outfile:
                    outfile.write(sgf.content)

                cursor.execute(
                    'insert into games (id, white, black, start_time, type, result) values(?, ?, ?, ?, ?, ?)',
                    [game.id, game.white, game.black, game.start_time, game.game_type, game.result]
                )

                cursor.execute('select count(*) from games')
                count = cursor.fetchone()[0]
                print(f'Game saved.  There are now {count} games.')

            cursor.close()
    finally:
        pass
