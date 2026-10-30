#!/bin/sh
# Unpacks the Oracle Cloud wallet before Java starts. The wallet is never baked into the image:
#   * Render "Secret File" named Wallet.zip  (-> /etc/secrets/Wallet.zip), or
#   * env var ORACLE_WALLET_B64 = base64 of the wallet zip
# Then ORACLE_URL can be   jdbc:oracle:thin:@<db>_high?TNS_ADMIN=/app/wallet
set -eu
umask 077
if [ -f /etc/secrets/Wallet.zip ]; then
  unzip -oq /etc/secrets/Wallet.zip -d /app/wallet
elif [ -n "${ORACLE_WALLET_B64:-}" ]; then
  printf '%s' "$ORACLE_WALLET_B64" | base64 -d > /tmp/wallet.zip
  unzip -oq /tmp/wallet.zip -d /app/wallet && rm -f /tmp/wallet.zip
  unset ORACLE_WALLET_B64
fi
exec java -jar /app/app.jar
