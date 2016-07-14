1. Build it:

        mvn clean package

2. Copy target/SchedulingService-2.0.jar to the plugins/ directory of your Neo4j server.

3. Configure Neo4j by adding a line to the end of your conf/neo4j.conf:

	: dbms.unmanaged_extension_classes=com.connectedconnections=/v1
	
4. Start Neo4j server.

5. Query it over HTTP:

	: curl http://localhost:7474/v1/service/helloworld
	
	To calculate the cohorts:
	
	: curl http://localhost:7474/v1/service/findCohorts
	
6. Create indexes:

create index on :Issue(cliqueID);
create index on :Issue(numberInClique);
create index on :PerspectiveEntity(cliqueID);
create index on :PerspectiveEntity(numberInClique);

7. Run sample queries:

match (i:Issue) where i.numberInClique > 4
return i;

match (i:Issue) where i.numberInClique > 3
with i limit 1000
match (i)-[a:ASSERTS_ID_MAP*]-(i1:Issue)
return i.cliqueID, collect(distinct(i.key)), i.numberInClique as cliqueMembers
order by cliqueMembers DESC;