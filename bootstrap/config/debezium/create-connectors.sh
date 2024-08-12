curl -i -X POST http://10.1.1.9:8083/connectors/ \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    -d @connectors/equipment.json
