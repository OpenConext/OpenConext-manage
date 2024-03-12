FROM ghcr.io/openconext/openconext-basecontainers/apache2-shibboleth:latest
COPY ./build/ /var/www/
COPY ./docker/conf/000-default.conf /etc/apache2/sites-enabled/000-default.conf
