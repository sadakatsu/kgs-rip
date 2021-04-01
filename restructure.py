import os
import re
import sys

for filename in os.listdir('games'):
    matcher = re.match(r'^(\d+)-(\d+)-(\d+).*\.sgf', filename)
    if not matcher:
        continue

    year, month, day = matcher.groups()
    year = f'games/{year}'
    month = f'{year}/{month}'
    day = f'{month}/{day}'
    if not os.path.isdir(year):
        os.mkdir(year)
    if not os.path.isdir(month):
        os.mkdir(month)
    if not os.path.isdir(day):
        os.mkdir(day)

    os.replace(f'games/{filename}', f'{day}/{filename}')
    print(f'Moved {filename} .')
