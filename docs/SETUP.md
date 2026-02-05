## Project Setup

This document explains how to set up the development environment.

Include here:
- Required tools and versions
- Environment setup steps
- How to run each component locally
- Any platform-specific notes (Windows/macOs/Linux)

Keep this up to date as setup changes.

## Demo QR Code Setup

- set up an ngrok account with: on https://dashboard.ngrok.com
- on your macbook, run the following commands:
  - brew install ngrok
  - verify installation ngrok version
  - ngrok config add-authtoken [the token ngrok dashboard gives you on signup]
  - ngrok config check
  - ngrok http 8080
  - brew install qrencode
  - qrencode -o demo-qr.png "https://<NGROK_PUBLIC_URL>"
  - open demo-qr.png


