# author: dr0i
#
# grep all MapperParsingException in ETL.log and send email
# see https://github.com/hbz/lobid-resources/issues/1512

MAIL_TO=$(cat .secrets/MAIL_TO)
MAIL_FROM=$(cat .secrets/MAIL_FROM)

ERROR_PATTERN="MapperParsingException"

cd ../
NEWEST_LOG_FN=$(ls ETL-log-*.gz| tail -n1)
echo $NEWEST_LOG_FN
ERRORS=$(zgrep  -B1 $ERROR_PATTERN $NEWEST_LOG_FN)
if [ -n "$ERRORS" ]; then
        chrlen=${#ERRORS}
        ERRORS_LINES_COUNT=$(echo "${ERRORS}"|wc -l)
        if [ ${chrlen} -gt 500000 ]; then
                ERRORS_TAILED="Es sind zu viele ERRORS - es werden nur die letzten 30 Zeilen angezeigt von insgesamt $ERRORS_LINES_COUNT:"
                ERRORS="$ERRORS_TAILED $(echo "$ERRORS"|tail -n30)"
        fi
       mail -s "$ERROR_PATTERN errors in Alma Fix ETL" "${MAIL_TO}" -a "From: ${MAIL_FROM}" << EOF
Getriggert von ausgeführt in $(pwd)/scripts :

Achte auf das Datum der ERROR-Zeilen - evtl. sind das alte Fehler!

$ERRORS
EOF
fi

