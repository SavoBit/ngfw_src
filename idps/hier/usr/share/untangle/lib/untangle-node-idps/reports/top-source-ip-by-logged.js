{
    "uniqueId": "intrusion-prevention-jt2pchkQ",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions detected grouped by source IP address.",
    "displayOrder": 501,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "source_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Source IP Addresses (logged)",
    "type": "PIE_GRAPH"
}
