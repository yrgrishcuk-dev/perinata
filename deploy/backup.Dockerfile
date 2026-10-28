FROM alpine:3.20
RUN apk add --no-cache postgresql16-client openssl
COPY deploy/backup.sh /backup.sh
RUN chmod +x /backup.sh
ENTRYPOINT ["/bin/sh", "-c", "while true; do /backup.sh; sleep 86400; done"]
