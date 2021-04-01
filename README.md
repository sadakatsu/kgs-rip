# KGS Rip

KGS Rip downloads all the 19x19 [Go](https://en.wikipedia.org/wiki/Go_(game)) games played by a connected subgraph of
[KGS](https://www.gokgs.com)'s players starting with one you pick. This uses the API that backs the
[KGS Archives](https://www.gokgs.com/archives.jsp) to download all the visible games played by a given player, then do
the same for that player's opponents, and so on. The result should be a large corpus of SGF files to process for
whatever ethical purpose one desires.

# How to use

1. Download a version of conda. I use [miniconda](https://docs.conda.io/en/latest/miniconda.html) .
2. Check out this repository: `git clone https://github.com/sadakatsu/kgs-rip.git`
3. Navigate into your clone.
4. Create a new conda environment: `conda create --name KgsRip --file requirements.txt`
5. Activate the conda environment: `conda activate KgsRip`
6. Run the script using a KGS username (mine is sadakatsu): `python main.py sadakatsu`
7. Wait. Scraping pages for links, downloading those links, and repeating is slow, especially when the backing server is
   slow.

If you need to kill the script for whatever reason, running the same launch command (or even supplying a different
username) will resume from whatever point got flushed to the Sqlite DB.

# Notes:

- 2021-04-01: I discovered that Windows 10 has a very hard time supporting hundreds of thousands of files in a flat
  directory. I had to change to using a nested directory structure to divide the files by date (`yyyy/MM/dd/sgf`). If
  you are using this tool, I recommend following the these steps:

    1. Stop the tool.
    2. Run the new script `restructure.py`. It will reorganize the downloaded games to use the new structure.
    3. Restart the tool.

- This program specifically downloads only 19x19 games and ignores both reviews and rengo (pair Go) matches. I have no
  need for other games. If you do. modify `get_games.py` .

- This was a pretty quick hack I threw together over some free hours. There are almost certainly bugs. Feel free to
  report any issues you discover.
