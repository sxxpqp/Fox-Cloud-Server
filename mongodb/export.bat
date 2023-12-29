mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeEntityFlag -o ./edgeEntityFlag.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeEntitySchema -o ./edgeEntitySchema.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeEntityTimeStamp -o ./edgeEntityTimeStamp.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeRepoComp -o ./edgeRepoComp.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeRepoCompScript -o ./edgeRepoCompScript.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeRepoCompScriptVersion -o ./edgeRepoCompScriptVersion.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeRepoGroup -o ./edgeRepoGroup.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeRepoGroupRelation -o ./edgeRepoGroupRelation.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeRepoProduct -o ./edgeRepoProduct.json

mongoexport -h 39.108.137.38 -u admin -p 12345678 -d fox-cloud-server-aggregator -c edgeServer -o ./edgeServer.json
