# wmsPerf
Simple tool to measure WMS perfs

To build:

```bash
./gradlew fatJar
```

To run:

```bash
for t in 1 5 10 20
do
    java -jar wmsLoad-all-1.0.jar -w http://example.com/pvalsecchi/mapserv \
         -l perf_shp -l perf_db -l perf_s3 \
         -z 1 -z 4 -z 16 -z 32 -n 20 -t $t
done
```