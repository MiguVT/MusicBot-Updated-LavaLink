## Unofficial Unofficial JMusicBot update (Forked from: https://github.com/xPrinny/MusicBot)
## Unofficial JMusicBot update (Forked from: https://github.com/d-pacheco/MusicBot)
Compiled using Github Actions

### To enable Oauth
- In your bot folder, edit "config.txt" and add `youtubeoauth2=true` to enable Youtube oauth.
- USE A BURNER GOOGLE ACCOUNT AS YOU MAY GET BANNED IN THE WORSE CASE.

<img align="right" src="https://i.imgur.com/zrE80HY.png" height="200" width="200">

# JMusicBot

[![Downloads](https://img.shields.io/github/downloads/jagrosh/MusicBot/total.svg)](https://github.com/jagrosh/MusicBot/releases/latest)
[![Stars](https://img.shields.io/github/stars/jagrosh/MusicBot.svg)](https://github.com/jagrosh/MusicBot/stargazers)
[![Release](https://img.shields.io/github/release/jagrosh/MusicBot.svg)](https://github.com/jagrosh/MusicBot/releases/latest)
[![License](https://img.shields.io/github/license/jagrosh/MusicBot.svg)](https://github.com/jagrosh/MusicBot/blob/master/LICENSE)
[![Discord](https://discordapp.com/api/guilds/147698382092238848/widget.png)](https://discord.gg/0p9LSGoRLu6Pet0k)<br>
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/jagrosh/MusicBot/tree/master.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/jagrosh/MusicBot/tree/master)
[![Build and Test](https://github.com/jagrosh/MusicBot/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/jagrosh/MusicBot/actions/workflows/build-and-test.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/jagrosh/musicbot/badge)](https://www.codefactor.io/repository/github/jagrosh/musicbot)

A cross-platform Discord music bot with a clean interface, and that is easy to set up and run yourself!

[![Setup](http://i.imgur.com/VvXYp5j.png)](https://jmusicbot.com/setup)

## Features
  * Easy to run (just make sure Java is installed, and run!)
  * Fast loading of songs
  * No external keys needed (besides a Discord Bot token)
  * Smooth playback
  * Server-specific setup for the "DJ" role that can moderate the music
  * Clean and beautiful menus
  * Supports many sites, including Youtube, Soundcloud, and more
  * Supports many online radio/streams
  * Supports local files
  * Playlist support (both web/youtube, and local)

## Supported sources and formats
JMusicBot supports all sources and formats supported by [lavaplayer](https://github.com/sedmelluq/lavaplayer#supported-formats):
### Sources
  * YouTube
  * SoundCloud
  * Bandcamp
  * Vimeo
  * Twitch streams
  * Local files
  * HTTP URLs
### Formats
  * MP3
  * FLAC
  * WAV
  * Matroska/WebM (AAC, Opus or Vorbis codecs)
  * MP4/M4A (AAC codec)
  * OGG streams (Opus, Vorbis and FLAC codecs)
  * AAC streams
  * Stream playlists (M3U and PLS)

## Example
![Loading Example...](https://i.imgur.com/kVtTKvS.gif)

## Setup
Please see the [Setup Page](https://jmusicbot.com/setup) to run this bot yourself!

## Questions/Suggestions/Bug Reports
**Please read the [Issues List](https://github.com/jagrosh/MusicBot/issues) before suggesting a feature**. If you have a question, need troubleshooting help, or want to brainstorm a new feature, please start a [Discussion](https://github.com/jagrosh/MusicBot/discussions). If you'd like to suggest a feature or report a reproducible bug, please open an [Issue](https://github.com/jagrosh/MusicBot/issues) on this repository. If you like this bot, be sure to add a star to the libraries that make this possible: [**JDA**](https://github.com/DV8FromTheWorld/JDA) and [**lavaplayer**](https://github.com/sedmelluq/lavaplayer)!

## Editing
This bot (and the source code here) might not be easy to edit for inexperienced programmers. The main purpose of having the source public is to show the capabilities of the libraries, to allow others to understand how the bot works, and to allow those knowledgeable about java, JDA, and Discord bot development to contribute. There are many requirements and dependencies required to edit and compile it, and there will not be support provided for people looking to make changes on their own. Instead, consider making a feature request (see the above section). If you choose to make edits, please do so in accordance with the Apache 2.0 License.
