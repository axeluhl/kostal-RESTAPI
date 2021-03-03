#!/bin/bash
SESSION=$1
curl -H 'Authorization: Session '${SESSION} "http://kostal.axeluhl.de/api/v1/settings" | jq '.. | (.moduleid? | select(. != null)) as $moduleid | (.settings? | select(. != null) | .[] + { moduleid: $moduleid } ) | select(.access | test(".*write.*"))'
