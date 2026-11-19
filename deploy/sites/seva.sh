# Garden City Seva: the complaint app. Read by deploy.sh (laptop) and setup-server.sh (server).
DOMAIN=gardencityseva.com
JAR=app/target/app-1.0-SNAPSHOT.jar
DIST=frontend/dist/seva/browser
# the 1 GB server: leaves room for Nginx and the OS
HEAP=512m
EXTRA_PACKAGES=
# staff clock in and out with a selfie and their position
PERMISSIONS="geolocation=(self), camera=(self), microphone=(), payment=()"
# pages the deploy checks afterwards
PAGES="/ /login /legal/ /complaints/7 /i18n/mr.json"
