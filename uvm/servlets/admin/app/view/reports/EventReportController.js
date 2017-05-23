Ext.define('Ung.view.reports.EventReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventreport',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            deactivate: 'onDeactivate'
        }
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel(), i;

        me.modFields = { uniqueId: null };

        // remove property grid if in dashboard
        if (me.getView().up('#dashboard')) {
            me.getView().down('unpropertygrid').hide();
        }

        vm.bind('{entry}', function (entry) {

            if (entry.get('type') !== 'EVENT_LIST') { return; }

            if (me.modFields.uniqueId !== entry.get('uniqueId')) {
                me.modFields = {
                    uniqueId: entry.get('uniqueId'),
                    defaultColumns: entry.get('defaultColumns')
                };

                var grid = me.getView().down('grid');
                var identifier = 'eventsGrid-' + entry.get('uniqueId');
                grid.itemId = identifier;
                grid.stateId = identifier;

                me.tableConfig = Ext.clone(TableConfig.getConfig(entry.get('table')));
                me.defaultColumns = vm.get('widget.displayColumns') || entry.get('defaultColumns'); // widget or report columns

                Ext.Array.each(me.tableConfig.columns, function (col) {
                    col.hidden = Ext.Array.indexOf(me.defaultColumns, col.dataIndex) < 0;
                    // !!!
                    if (!col.filter && col.dataIndex !== 'time_stamp' && col.dataIndex !== 'end_time') {
                        col.filter = 'string';
                    }
                });

                me.tableConfig.columns.forEach( function(column){
                    if( column.dataIndex &&
                        !column.stateId ){
                        column.stateId = column.dataIndex;
                    }
                    if( column.columns ){
                        /*
                         * Grouping
                         */
                        column.columns.forEach( Ext.bind( function( subColumn ){
                            if (!column.filter ){
                                column.filter = 'string';
                            }
                            grid.initComponentColumn( subColumn );
                        }, this ) );
                    }
                    grid.initComponentColumn( column );
                });

                grid.tableConfig = me.tableConfig;
                grid.setColumns(me.tableConfig.columns);
                // Force state processing for this renamed grid
                grid.mixins.state.constructor.call(grid);

                var propertygrid = me.getView().down('#eventsProperties');
                vm.set( 'eventProperty', null );
                propertygrid.fireEvent('beforerender');
                propertygrid.fireEvent('beforeexpand');

                if (!me.getView().up('reportwidget')) {
                    me.fetchData();
                } else {
                    me.isWidget = true;
                }
                return;
            }

            if (!Ext.Array.equals(me.modFields.defaultColumns, entry.get('defaultColumns'))) {
                me.modFields.defaultColumns = entry.get('defaultColumns');
                Ext.Array.each(me.getView().down('grid').getColumns(), function (col) {
                    col.setHidden(Ext.Array.indexOf(entry.get('defaultColumns'), col.dataIndex) < 0);
                });
            }

        }, me, { deep: true });

        // clear grid selection (hide event side data) when settings are open
        vm.bind('{settingsBtn.pressed}', function (pressed) {
            if (pressed) {
                me.getView().down('grid').getSelectionModel().deselectAll();
            }
        });
    },

    onDeactivate: function () {
        this.modFields = { uniqueId: null };
        this.getViewModel().set('eventsData', []);
        this.getView().down('grid').getSelectionModel().deselectAll();
    },

    fetchData: function (reset, cb) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        var limit = 0;
        if( me.getView().up('reports-entry') ){
            limit = me.getView().up('reports-entry').down('#eventsLimitSelector').getValue();
        }
        me.entry = vm.get('entry');

        var startDate = vm.get('startDate');
        var endDate = vm.get('tillNow') ? null : vm.get('endDate');
        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            startDate = new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600 * 24) * 1000);
            endDate = new Date(rpc.systemManager.getMilliseconds());
        }

        var grid = v.down('grid');

        me.getViewModel().set('eventsData', []);
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getEventsForDateRangeResultSet',
                        vm.get('entry').getData(), // entry
                        vm.get('sqlFilterData'), // etra conditions
                        limit,
                        startDate, // start date
                        endDate) // end date
            .then(function(result) {
                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').down('#currentData').setLoading(false);
                }
                me.getView().setLoading(false);

                me.loadResultSet(result);

                if (cb) { cb(); }
            });
    },

    loadResultSet: function (reader) {
        var me = this,
            grid = me.getView().down('grid');
        this.getView().setLoading(true);
        grid.getStore().setFields( grid.tableConfig.fields );
        reader.getNextChunk(Ext.bind(this.nextChunkCallback, this), 1000);
    },

    nextChunkCallback: function (result, ex) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        vm.set('eventsData', result.list);
        this.getView().setLoading(false);

    },

    onEventSelect: function (el, record) {
        var me = this, vm = this.getViewModel(), propsData = [];

        if (me.isWidget) { return; }

        Ext.Array.each(me.tableConfig.columns, function (column) {
            propsData.push({
                name: column.header,
                value: record.get(column.dataIndex)
            });
        });

        vm.set('propsData', propsData);
        // when selecting an event hide Settings if open
        me.getView().up('reports-entry').lookupReference('settingsBtn').setPressed(false);

    },

    onDataChanged: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if( vm.get('eventProperty') == null ){
            v.down('grid').getSelectionModel().select(0);
        }

        if( v.up().down('ungridstatus') ){
            v.up().down('ungridstatus').fireEvent('update');
        }
    }

});
