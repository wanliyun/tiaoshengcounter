#!/usr/bin/env python3
"""
Simple HTTP server to serve files from the release/ directory for downloading.
Usage:
    python3 serve_release.py              # default port 8080
    python3 serve_release.py --port 9000   # custom port
    python3 serve_release.py --bind 0.0.0.0 --port 8080  # accessible from LAN
"""

import argparse
import os
import sys
from http.server import HTTPServer, SimpleHTTPRequestHandler


class ReleaseHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=RELEASE_DIR, **kwargs)

    def log_message(self, format, *args):
        # Simple log format
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), format % args))


def main():
    parser = argparse.ArgumentParser(description="Serve release/ directory over HTTP")
    parser.add_argument("--port", type=int, default=8080, help="Port to listen on (default: 8080)")
    parser.add_argument("--bind", type=str, default="0.0.0.0", help="Address to bind (default: 0.0.0.0)")
    args = parser.parse_args()

    if not os.path.isdir(RELEASE_DIR):
        print(f"ERROR: release directory not found: {RELEASE_DIR}", file=sys.stderr)
        sys.exit(1)

    os.chdir(RELEASE_DIR)
    server = HTTPServer((args.bind, args.port), ReleaseHandler)
    print(f"Serving {RELEASE_DIR} on http://{args.bind}:{args.port}")
    print("Press Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped.")


if __name__ == "__main__":
    RELEASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "release")
    main()
