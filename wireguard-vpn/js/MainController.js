Ext.define('Ung.apps.wireguard-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wireguard-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    // General settings that are checked and if different, force
    // a full restart of wireguard.
    fullRestartSettingsKeys: [
        'keepaliveInterval',
        'listenPort',
        'mtu',
        'addressPool'
    ],

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings')
        ], this).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set('originalSettings', JSON.parse(JSON.stringify(result[0])));
            vm.set('settings', result[0]);
            vm.set('panel.saveDisabled', false);

            var networkSettings = result[1];
            var warning = '';
            var listenPort = vm.get('settings.listenPort');
            if(me.isUDPAccessAllowedForPort(networkSettings, listenPort) == false) {
                warning = '<i class="fa fa-exclamation-triangle fa-red fa-lg"></i> <strong>' + 'There are no enabled access rules to allow traffic on UDP port '.t() + listenPort + '</strong>';
            }
            vm.set('warning', warning);
            vm.set('hostname', networkSettings['hostName']);


            // bind any network grids with updated network store data using their listProperty
            v.query('ungrid').forEach(function (grid) {
                if(grid.isNetworkGrid) {
                    var netGridStore = WireguardUtil.createNetworkStore(vm.get(grid.listProperty));
                    grid.setStore(netGridStore);
                }
            });

            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

        // trigger active clients/servers fetching when instance run state changes
        vm.bind('{state.on}', function (stateon) {
            if (stateon) {
                me.getTunnelStatus();
            } else {
                vm.set({
                    tunnelStatusData: []
                });
            }
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        if (me.validateSettings() != true) return;

        var tunnelsAdded = [],
            tunnelsDeleted = [],
            settingsChanged = false;

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                if(grid.listProperty == 'settings.tunnels.list'){
                    vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                    store.getModifiedRecords().forEach(function(record){
                        var previousPublicKey = record.getPrevious('publicKey');
                        if(previousPublicKey == undefined ){
                            previousPublicKey = record.get('publicKey');
                        }
                        if(previousPublicKey != ""){
                            if(tunnelsDeleted.indexOf(previousPublicKey) == -1){
                                tunnelsDeleted.push(previousPublicKey);
                            }
                        }
                        if( record.get("enabled") &&
                            tunnelsAdded.indexOf(record.get('publicKey')) == -1){
                            tunnelsAdded.push(record.get('publicKey'));
                        }
                    });
                    store.getNewRecords().forEach(function(record){
                        if( record.get("enabled") &&
                            tunnelsAdded.indexOf(record.get('publicKey')) == -1){
                            tunnelsAdded.push(record.get('publicKey'));
                        }
                    });
                    store.getRemovedRecords().forEach(function(record){
                        if(tunnelsDeleted.indexOf(record.get('publicKey')) == -1){
                            tunnelsDeleted.push(record.get('publicKey'));
                        }
                    });
                }

                // Now we need to convert the list back to settings format that setSettings is expecting (just an array of strings)
                 if(grid.listProperty == 'settings.networks.list') {

                     // Only update the setting if any of these changed
                     if(store.getModifiedRecords().length > 0 || store.getNewRecords().length > 0 || store.getRemovedRecords().length > 0){
                         settingsChanged = true;
                         var netList = [];
                         store.each(function(record){
                             netList.push(record.get('network'));
                         });
                         vm.set(grid.listProperty, netList);
                        }
                 }
            }
        });

        // Determine if settings changed, requiring a full restart.
        me.fullRestartSettingsKeys.forEach( function(key){
            if(vm.get('originalSettings')[key] != vm.get('settings')[key]){
                settingsChanged = true;
            }
        });
        if(tunnelsDeleted.length == 0 && tunnelsAdded == 0){
            settingsChanged = true;
        }

        v.setLoading(true);
        vm.set('panel.saveDisabled', true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'), settingsChanged)
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');

            if(settingsChanged == false){
                me.updateTunnels(tunnelsDeleted, tunnelsAdded);
            }

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', false);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    updateTunnels: function( deleted, added ){
        var me = this, v = this.getView(), vm = this.getViewModel();
        var rpcSequence = [];

        deleted.forEach(function(publicKey){
            rpcSequence.push(Rpc.asyncPromise(v.appManager, 'deleteTunnel', publicKey));
        });
        added.forEach(function(publicKey){
            rpcSequence.push(Rpc.asyncPromise(v.appManager, 'addTunnel', publicKey));
        });
        Ext.Deferred.sequence(rpcSequence, this)
        .then(function(result){
            if(Util.isDestroyed(vm)){
                return;
            }
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    validateSettings: function() {
        return(true);
    },

    getTunnelStatus: function () {
        var me = this,
            grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getTunnelStatus')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }
            var status = Ext.JSON.decode(result);

            var delay = 100;
            var updateStatusTask = new Ext.util.DelayedTask( Ext.bind(function(){
                if(Util.isDestroyed(vm, status)){
                    return;
                }
                var tunnels = vm.get('tunnels');
                if(!tunnels){
                    updateStatusTask.delay(delay);
                    return;
                }
                tunnels.each(function(tunnel){
                    status.wireguard.forEach(function(status){
                        if(tunnel.get('publicKey') == status['peer-key']){
                            status['tunnel-description'] = tunnel.get('description');
                        }
                    });
                });
                vm.set('tunnelStatusData', status.wireguard);
            }, me) );
            updateStatusTask.delay( delay );

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    isUDPAccessAllowedForPort: function(networkSettings, listenPort) {
        if(networkSettings.accessRules && networkSettings.accessRules.list) {
            for(var i=0; i<networkSettings.accessRules.list.length ; i++) {
                var rule = networkSettings.accessRules.list[i];
                if(rule.enabled == true && rule.blocked == false) {
                    if(rule.conditions && rule.conditions.list) {
                        var isUDP = false;
                        var isPort = false;
                        for(var j=0; j<rule.conditions.list.length ; j++) {
                            var condition = rule.conditions.list[j];
                            if(condition.invert == false) {
                                if(condition.conditionType == 'PROTOCOL' && condition.value == 'UDP') {
                                    isUDP = true;
                                }
                                if(condition.conditionType == 'DST_PORT' && parseInt(condition.value, 10) == listenPort) {
                                    isPort = true;
                                }
                            }
                        }

                        if(isUDP == true && isPort == true) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    },

    getNewAddressSpace: function() {
        var me = this,
        vm = this.getViewModel();
        Rpc.asyncData(this.getView().appManager, 'getNewAddressPool')
        .then( function(result){
            if(Util.isDestroyed(me, vm)){
                return;
            }

            vm.set('settings.addressPool', result);
        },function(ex){
            Util.handleException(ex);
        });
    }
});

Ext.define('Ung.apps.reports.cmp.Ung.apps.wireguard-vpn.cmp.WireGuardVpnTunnelGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unwireguardvpntunnelgrid',

    remoteConfigDisabled: function(view, rowIndex, colIndex, item, record){
        if(record.get('id') == "" || record.get('id') == -1){
            return true;
        }
        return false;
    },

    getRemoteConfig: function(unk1, unk2, unk3, event, unk5, record){
        var v = this.getView();
        var dialog = v.add({
            xtype: 'app-wireguard-vpn-remote-config',
            title: 'Remote Configuration'.t(),
            record: record
        });
        dialog.setPosition(event.getXY());
        dialog.show();
    }
});

Ext.define('Ung.apps.wireguard-vpn.cmp.WireGuardVpnTunnelRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unwireguardvpntunnelrecordeditor',
    alias: 'widget.unwireguardvpntunnelrecordeditor',

    controller: 'unwireguardvpntunnelrecordeditorcontroller'
});

Ext.define('Ung.apps.wireguard-vpn.cmp.WireGuardVpnTunnelRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unwireguardvpntunnelrecordeditorcontroller',

    pasteTunnel: function(component){
        if(!component.target || 
           !component.target.dataset.componentid ||
           !component.target.dataset.componentid){
            return;
        }
        var el = Ext.getCmp(component.target.dataset.componentid);
        if(!el){
            return;
        }
        var view = el.up('unwireguardvpntunnelrecordeditor'),
            controller = view.getController(),
            record = view.record; 
        if(record.get('id') != -1){
            // Only on a new record.
            return;
        }

        var remote = {};
        try{
            remote = JSON.parse(component.event.clipboardData.getData("text/plain"));
        }catch(e){
            return;
        }

        var remoteToRecordTask = new Ext.util.DelayedTask( Ext.bind(function(){
            if(Util.isDestroyed(remote, record)){
                return;
            }
            record.set('description', remote['hostname']);
            record.set('endpointDynamic', false);
            Ext.Object.each(remote, function(key, value){
                if(key in record.data){
                    record.set(key, value);
                }
            });
        }, view) );
        remoteToRecordTask.delay( 150 );
    },

    // Loop through the stored records to see if the passed ip
    // address is already used
    isAddrUsed: function(ip, store) {
        var ret = false;
        store.each(function(record,idx) {
            if( record.get('peerAddress') == ip ){
                ret = true;
            }
        });
        return ret;
    },

    // get next pool address
    getNextUnusedPoolAddr: function(){
        var me = this,
            grid = this.mainGrid,
            store = grid.getStore(),
            addressPool = grid.up('panel').up('apppanel').getViewModel().get('settings.addressPool'),
            pool = addressPool.split("/")[0];

        // Assume the first pool address is used by the wg interface
        var nextPoolAddr = Util.incrementIpAddr(pool, 2);
        while (me.isAddrUsed(nextPoolAddr, store)) {
            nextPoolAddr = Util.incrementIpAddr(nextPoolAddr, 1);
        }

        return nextPoolAddr;
    },

    // Override onAfterRender so we can prepopulate the peerAddress field with the next
    // available address from the wireguard tunnel address pool.  We loop through the
    // existing tunnels to make sure we select an address that isn't already used.  After
    // setting the peerAddress, we then call the default onAfterRender.
    onAfterRender: function (view) {
        var me = this,
            grid = this.mainGrid, vm = this.getViewModel(),
            record = vm.get('record');

        this.callParent([view]);

        view.down('form').add(
            Ung.apps['wireguard-vpn'].Main.hostDisplayFields(true, !record.get('markedForNew'), true)
        );

        view.getEl().on('paste', me.pasteTunnel);
    },

    endpointTypeComboChange: function(combo, newValue, oldValue){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            record = me.getViewModel().get('record'),
            form = combo.up('form');

        form.down('[itemId=publicKey]').allowBlank = newValue;
        form.down('[itemId=publicKey]').validate();

        var peerAddress = record.get('peerAddress');
        if(newValue && !peerAddress){
            // Dynamic
            record.set('peerAddress', me.getNextUnusedPoolAddr());
        } else if(!newValue && peerAddress){
            // Static
            if(record.get('markedForNew')){
                if(peerAddress == me.getNextUnusedPoolAddr()){
                    record.set('peerAddress', '');
                }
            }
        }

        // Bind the stores to the custom network grid
        v.query('ungrid').forEach(function (grid) {
            if(grid.isNetworkGrid) {
                if(grid.getBind() == null) {
                    var netGridStore = WireguardUtil.createNetworkStore(vm.get(grid.listProperty));
                    grid.setBind({store: netGridStore});
                }
            }
        });
    }
});

/**
 * WireguardUtil is a singleton used to add utility functions that may be needed across controller components.
 * 
 */
Ext.define('Ung.apps.wireguard-vpn.WireguardUtil', {
    singleton: true,
    alternateClassName: ['WireguardUtil'],

    /**
     * createNetworkStore creates a dynamic in memory store interface that will translate json arrays into 
     * a simple json object, to make it easier for an ungrid to handle
     * 
     * ie: "10.0.0.0/24", "10.0.0.1/24" -> network: "10.0.0.0/24", network: "10.0.0.1/24"
     * 
     * 
     * @param {json} listPropData - The list data that should be bound to the grid
     */
    createNetworkStore: function(listPropData) {
        var netStore = Ext.create('Ext.data.Store', {
            data: listPropData,
            fields: [{name:'network'}],
            autoLoad: true,
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json',
                    transform: function(data) {
                        data = data.map(function(val){
                            return { network: val };
                        });
                        return data;
                    }
                },
            }
        });
        
        return netStore;
    }
});
