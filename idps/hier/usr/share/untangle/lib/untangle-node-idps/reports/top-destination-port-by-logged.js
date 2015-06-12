{
    "uniqueId": "intrusion-prevention-5vv8yvOs",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions detected grouped by destination port.",
    "displayOrder": 801,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "dest_port",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Destination Ports (logged)",
    "type": "PIE_GRAPH"
}
