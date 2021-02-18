#!/bin/sh
gpg --quiet --batch --yes --decrypt --passphrase="$KEY_PASSWORD" \
  --output ./src/test/resources/key.json ./src/test/resources/key.json.gpg