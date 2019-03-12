#!/bin/bash

gcloud --project=open-targets-genetics app deploy \
    --no-promote \
    -v $(git describe --abbrev=0 \
    --tags | sed "s:\.:-:g")
