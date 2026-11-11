# Rolling back a bad deploy

Every deploy creates a new Cloud Run revision, and a revision never changes: it keeps the exact image (tagged with
the commit's SHA) and configuration it was created with. Rolling back rebuilds nothing. It sends the traffic back
to a revision that worked.

## 1. Find the revision to go back to

    gcloud run revisions list --service api-gateway --format="table(metadata.name,metadata.labels.commit,metadata.creationTimestamp)"

The newest is first. Pick the last one that worked, usually the second. Its `commit` label names the commit it
runs (the pipeline sets it; a revision deployed by hand has none). The revision's image shows only as a digest,
`@sha256:…`, not as the tag.

## 2. Send all traffic to it

    gcloud run services update-traffic api-gateway --to-revisions <revision>=100

It takes seconds. Then check the service and the system:

    gcloud run services describe api-gateway --format='value(status.traffic)'
    GATEWAY_URL=$(gcloud run services describe api-gateway --format='value(status.url)')
    curl -fsS $GATEWAY_URL/actuator/health

The old revision may have no instance running: the first request or two can fail while it starts. Try again before
you decide the rollback did not work.

Roll back only the service that broke. The services do not depend on each other's revision numbers, only on the
API and the messages they share.

## 3. Fix forward, and know what the next deploy does

While traffic is pinned to an old revision, **a new revision gets no traffic** by itself. That is why the pipeline
ends every deploy with:

    gcloud run services update-traffic <service> --to-latest

So the normal way out of a rollback is: fix the code, push to `main`, and let the pipeline deploy it and move the
traffic to the newest revision. To undo the rollback by hand, without a new deploy, run that same `--to-latest`.
