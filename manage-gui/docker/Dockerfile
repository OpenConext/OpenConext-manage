FROM ghcr.io/openconext/openconext-basecontainers/apache2-shibboleth:latest
COPY ./dist/ /var/www/
COPY ./docker/conf/000-default.conf /etc/apache2/sites-enabled/000-default.conf

HEALTHCHECK --interval=10s --timeout=10s --start-period=10s --retries=3 \
  CMD ["curl", "--fail", "http://localhost/internal/health"]
