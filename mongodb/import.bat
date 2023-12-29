mongoimport -h 39.108.137.38 -u admin -p 12345678  --db fox-cloud-server-aggregator -c socialBoxEntity  --type json ./socialBoxEntity.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeEntitySchema  --type json ./edgeEntitySchema.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeEntityTimeStamp  --type json ./edgeEntityTimeStamp.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeRepoComp  --type json ./edgeRepoComp.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeRepoCompScript  --type json ./edgeRepoCompScript.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeRepoCompScriptVersion  --type json ./edgeRepoCompScriptVersion.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeRepoGroup  --type json ./edgeRepoGroup.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeRepoGroupRelation --type json ./edgeRepoGroupRelation.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeRepoProduct --type json ./edgeRepoProduct.json

mongoimport -h 39.108.137.38 -u admin -p 12345678 --db fox-cloud-server-aggregator -c edgeServer --type json ./edgeServer.json
