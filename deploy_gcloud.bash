#!/bin/bash

gcloud --project=open-targets-genetics beta app deploy \
    --no-promote \
    --no-stop-previous-version \
    -v $(git describe --abbrev=0 \
    --tags | sed "s:\.:-:g")
