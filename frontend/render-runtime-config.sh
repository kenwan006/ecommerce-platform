#!/bin/sh
set -eu

envsubst '${STRIPE_PUBLISHABLE_KEY}' \
  < /usr/share/nginx/html/env.js.template \
  > /usr/share/nginx/html/env.js
