#!/bin/bash
# LocalStack init hook (runs inside the container once S3 is ready).
# Seeds a demo bucket with a couple of objects so the agent has something to inspect.
set -e

BUCKET="poc-bucket"

echo "[init] creating bucket $BUCKET"
awslocal s3 mb "s3://$BUCKET" || true

echo "[init] seeding objects"
echo "Welcome to the AI Agent PoC. This text lives in LocalStack S3." \
  | awslocal s3 cp - "s3://$BUCKET/notes/welcome.txt"

printf 'id,name\n1,alpha\n2,beta\n3,gamma\n' \
  | awslocal s3 cp - "s3://$BUCKET/data/items.csv"

echo "[init] done. Objects in $BUCKET:"
awslocal s3 ls "s3://$BUCKET" --recursive
