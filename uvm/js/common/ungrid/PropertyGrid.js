/**
 * Used in showing grid selection details containing all record data
 */
Ext.define('Ung.cmp.PropertyGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.unpropertygrid',

    controller: 'unpropertygrid',

    editable: false,
    width: Renderer.calculateWith(4),
    split: true,
    collapsible: true,
    resizable: true,
    shadow: false,
    animCollapse: false,
    titleCollapse: true,
    collapsed: false,

    // cls: 'prop-grid',

    disableSelection: true,

    store: {
        fields: ['name', 'value', 'category'],
        groupField: 'category',
        sorters: 'name',
        data: []
    },

    columns: [{
        text: 'Name',
        flex: 1,
        dataIndex: 'name'
    }, {
        text: 'Value',
        dataIndex: 'value'
    }, {
        text: 'Category',
        dataIndex: 'category',
        hidden: true       
    }],

    viewConfig: {
        enableTextSelection: true,
        getRowClass: function(record) {
            var cls = 'x-selectable'; // NGFW-11399 force selectable text
            if (record.get('value') === null || record.get('value') === '') {
                cls += ' empty';
            }
            return cls;
        }
    },

    nameColumnWidth: 200,

    listeners: {
        beforeedit: function () {
            return false;
        },
        beforeexpand: 'onBeforeExpand',
        beforerender: 'onBeforeRender'
    },

    // initComponent: function () {
    //     // var me = this;
    //     // if(me.emptyText){
    //     //     me.emptyText = '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>' + this.emptyText + '</p>';
    //     // }
    //     // this.callParent(arguments);
    // }
});
