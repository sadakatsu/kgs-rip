import html
import requests
import re

from collections import namedtuple
from io import StringIO
from lxml import etree
from lxml.etree import ElementTree
from typing import List

Game = namedtuple('Game', ['id', 'white', 'black', 'setup', 'start_time', 'game_type', 'result'])


def get_url(node: ElementTree) -> str:
    found = node.xpath('a/@href')
    return None if not found else str(found[0])


def get_user(node: ElementTree) -> str:
    url = get_url(node)
    if url:
        match = re.match(r'gameArchives\.jsp\?.*&?user=(?P<user>[^&]+)', url)
        user = match.group('user') if match else None
    else:
        user = None
    return user


def get_text(node: ElementTree) -> str:
    return None if not node.text else html.unescape(node.text.strip())


def get_games(user: str, year: int, month: int) -> List[Game]:
    games = []

    url = f'https://www.gokgs.com/gameArchives.jsp?user={user}&oldAccounts=t&year={year}&month={month}'
    response = requests.get(url)
    content = response.text
    if f'Sorry, {user} did not play any games' not in content:
        content = content[content.index('<html>'):]

        parser = etree.HTMLParser()
        root = etree.parse(StringIO(content), parser)
        for row in root.xpath('/html/body/table[1]/tr[td]'):
            cells = row.xpath('td')

            # Demonstration/review boards are not interesting.
            if cells[-2] == 'Review' or len(cells) != 7:
                continue

            viewable = cells[0]
            white = cells[1]
            black = cells[2]
            setup = cells[3]
            start_time = cells[4]
            game_type = cells[5]
            results = cells[6]

            game = Game(
                get_url(viewable),
                get_user(white),
                get_user(black),
                get_text(setup),
                get_text(start_time),
                get_text(game_type),
                get_text(results)
            )
            print(game)
            games.append(game)

    return games

