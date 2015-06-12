{
    "uniqueId": "intrusion-prevention-V5e0S54W",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by category.",
    "displayOrder": 402,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "category",
    "pieSumColumn": "sum(blocked::int)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Categories (blocked)",
    "type": "PIE_GRAPH"
}
