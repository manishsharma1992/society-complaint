# The public feed. Read by deploy.sh (laptop) and setup-server.sh (server).
DOMAIN=runwalltdfakepromises.com
JAR=feed-app/target/feed-app-1.0-SNAPSHOT.jar
DIST=frontend/dist/feed/browser
# the 2 GB server: leaves room for Nginx, the OS and ffmpeg
HEAP=1g
# ffmpeg re-encodes feed videos; it parses untrusted files, so keep it updated with the system (apt upgrade)
EXTRA_PACKAGES=ffmpeg
PERMISSIONS="geolocation=(), camera=(), microphone=(), payment=()"
# pages the deploy checks afterwards
PAGES="/ /public /hi/ /i18n/mr.json"
