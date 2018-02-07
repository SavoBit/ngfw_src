Ext.define('Ung.config.network.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-network',

    control: {
        '#': { painted: 'loadSettings' },
        // '#interfaces': { beforerender: 'onInterfaces' },
        // '#interfacesGrid': { reconfigure: 'interfacesGridReconfigure'},
        // '#routes': { afterrender: 'refreshRoutes' },
        // '#qosStatistics': { afterrender: 'refreshQosStatistics' },
        // '#upnpStatus': { afterrender: 'refreshUpnpStatus' },
        // '#dhcpLeases': { afterrender: 'refreshDhcpLeases' },
        // 'networktest': { afterrender: 'networkTestRender' },
        // '#advanced': {
        //     activate: Ung.controller.Global.onSubtabActivate,
        // },
        // '#advanced #advanced': {
        //     beforetabchange: Ung.controller.Global.onBeforeSubtabChange
        // },
        // '#troubleshooting': {
        //     activate: Ung.controller.Global.onSubtabActivate,
        // },
        // '#troubleshooting #troubleshooting': {
        //     beforetabchange: Ung.controller.Global.onBeforeSubtabChange
        // }
    },

    onPainted: function(grid) {
        console.log(grid.conditions);
        grid.menu = {
            xtype: 'menu',
            viewModel: {},
            anchor: true,
            padding: '10 0',
            defaultType: 'menucheckitem',
            mouseLeaveDelay: 0,
            // maxHeight: 200,
            // scrollable: true,
            tbar: {
                items: [{
                    xtype: 'component',
                    html: 'Conditions'.t()
                }]
            }
        };
    },

    loadSettings: function () {
        console.log('loading');
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();

        v.mask({
            xtype: 'loadmask',
            message: 'loading ...',
            indicator: false
        });
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings'),
            Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
            Rpc.asyncPromise('rpc.networkManager.getDeviceStatus'),
        ], this).then(function (result) {
            v.unmask();
            var intfStatus, devStatus;
            console.log(result);
            rpc.networkSettings = result[0]; // update rpc.networkSettings with latest data (usually after save)

            result[0].interfaces.list.forEach(function (intf) {

                if (result[1] && result[1].list.length > 0) {
                    intfStatus = Ext.Array.findBy(result[1].list, function (intfSt) {
                        return intfSt.interfaceId === intf.interfaceId;
                    });
                    if (intfStatus != null){
                        delete intfStatus.javaClass;
                    }
                    Ext.apply(intf, intfStatus);
                }

                if (result[2] && result[2].list.length > 0) {
                    devStatus = Ext.Array.findBy(result[2].list, function (devSt) {
                        return devSt.deviceName === intf.physicalDev;
                    });
                    delete devStatus.javaClass;
                    Ext.apply(intf, devStatus);
                }

            });
            vm.set('settings', result[0]);

            // check if Allow SSH access rule is enabled
            // var accessRulesSshEnabled = me.isSshAccessRuleEnabled(vm.get('settings'));
            // vm.set('accessRulesSshEnabled', accessRulesSshEnabled);

            // var accessRulesLength = me.getAccessRulesCount(vm.get('settings'));
            // vm.set('accessRulesLength', accessRulesLength);

            // me.setPortForwardSimples(); // commented out as it should work withou ti
            // me.setPortForwardWarnings();
            // me.setInterfaceConditions(); // update dest/source interfaces conditions from grids

        }, function (ex) {
            v.unmask();
            console.error(ex);
            Util.handleException(ex);
        });
    },

    saveSettings: function() {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;

        console.log(vm.get('settings'));

        view.query('mastergrid').forEach(function (grid) {
            var store = grid.getStore();

            console.log(store.getModifiedRecords(), store.getNewRecords());
            // vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));

            vm.set('settings.' + grid.settingsProperty + '.list', Ext.Array.pluck(store.getRange(), 'data'));

            console.log(vm.get('settings'));

            // if(store.type == "chained"){
            //     return;
            // }
            // if(grid.listProperty == null){
            //     return;
            // }

            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            // if (store.getModifiedRecords().length > 0 || store.isReordered) {
            //     store.each(function (record) {
            //         if (record.get('markedForDelete')) {
            //             record.drop();
            //         }
            //     });
            //     store.isReordered = undefined;
            //     vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            //     // store.commitChanges();
            // }
        });

        view.mask();
        Rpc.asyncData('rpc.networkManager.setNetworkSettings', vm.get('settings'))
            .then(function(result) {
                me.loadSettings();
                Ext.toast({
                    message: 'Settings saved',
                    centered: true,
                    timeout: 2000,
                    showAnimation: {
                        type: 'slide',
                        duration: 250,
                        easing: 'ease-out'
                    }
                });
                // Util.successToast('Network'.t() + ' settings saved!');
                // Ext.fireEvent('resetfields', view);
            }).always(function () {
                view.unmask();
            });
    },


    onEditInterface: function () {
        var view = this.getView(),
            dialog = view.add({
                xtype: 'config-interface'
            });
        dialog.show();
    },


    conditionsRenderer: function (value) {
        if (!value) { return ''; } // if conditions are null return empty string



        var view = this.getView().down('config-network-nat-rules'),
            conds = value.list,
            resp = [], i, valueRenderer = [];

        for (i = 0; i < conds.length; i += 1) {
            valueRenderer = [];

            switch (conds[i].conditionType) {
            case 'SRC_INTF':
            case 'DST_INTF':
                conds[i].value.toString().split(',').forEach(function (intfff) {
                    valueRenderer.push(Util.interfacesListNamesMap()[intfff]);
                });
                break;
            case 'DST_LOCAL':
            case 'WEB_FILTER_FLAGGED':
                valueRenderer.push('true'.t());
                break;
            case 'DAY_OF_WEEK':
                conds[i].value.toString().split(',').forEach(function (day) {
                    valueRenderer.push(Util.weekdaysMap[day]);
                });
                break;
            default:
                // to avoid exceptions, in some situations condition value is null
                if (conds[i].value !== null) {
                    valueRenderer = conds[i].value.toString().split(',');
                } else {
                    valueRenderer = [];
                }
            }
            // for boolean conditions just add 'True' string as value
            // if (view.conditionsMap[conds[i].conditionType].type === 'boolean') {
            //     valueRenderer = ['True'.t()];
            // }

            resp.push(view.conditionsMap[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + valueRenderer.join(', ') + '</span>' + '</strong>');
        }
        return resp.length > 0 ? resp.join(' &nbsp;&bull;&nbsp; ') : '<em>' + 'No conditions' + '</em>';
    },


    getMenu: function () {
        var menu, menuOpts = this.getView().down('config-network-nat-rules > grid').menu,
            view = this.getView().down('config-network-nat-rules');


        // if (!menu) {
        this.rulesMenu = menu = Ext.create(Ext.apply({
            ownerCmp: view
        }, menuOpts));
        // }
        console.log(menu);
        return menu;
    },

    updateMenu: function (record, el, e, align) {
        var menu = this.getMenu();

        console.log(menu);
        //     conds = record.get('conditions').list;
        // console.log(conds);
        // this.getViewModel().set('record', record.get('conditions').list);
        // Ext.Array.each(menu.getItems().items, function (item) {
        //     if (item.xtype === 'toolbar') { return; }
        //     var found = Ext.Array.findBy(conds, function (c) {
        //         // c.value = '1,2,3,4,5';
        //         return c.conditionType === item.val;
        //     });
        //     if (found) {
        //         var menu2 = item.getMenu();
        //         // menu2.getItems().items[0].setChecked(!found.invert);
        //         // menu2.getItems().items[1].setChecked(found.invert);
        //     }
        //     item.setChecked(found ? true : false);
        // });
        menu.showBy(el, align);
    },

    setExpand: function (grid, context) {
        this.updateMenu(context.record, context.tool.el, context.event, 't0-b0');
        // menu.show();
        // var rec = info.record;
        // console.log(rec);
    },

    check: function (item) {
        // console.log(item.up('menu'));
        var rules = this.getViewModel().get('natRules');
        var conds = rules.getData().items[0].get('conditions');

        rules.getData().items[0].set('conditions.list', []);
        // conds.set('list', []);
        //conds.list[0].value = '1, 2, 3, 4, any';
    },


    onDataUpdate: function (el, record) {
        console.log(record);
    },

    // saveSettings: function () {
    //     var view = this.getView();
    //     var vm = this.getViewModel();
    //     var me = this;

    //     if (!Util.validateForms(view)) {
    //         return;
    //     }

    //     // update interfaces data
    //     var interfacesStore = view.down('#interfacesGrid').getStore();
    //     if (interfacesStore.getModifiedRecords().length > 0 ||
    //         interfacesStore.getNewRecords().length > 0 ||
    //         interfacesStore.getRemovedRecords().length > 0) {
    //         vm.set('settings.interfaces.list', Ext.Array.pluck(interfacesStore.getRange(), 'data'));
    //     }


    //     // used to update all tabs data
    //     view.query('ungrid').forEach(function (grid) {
    //         var store = grid.getStore();

    //         if(store.type == "chained"){
    //             return;
    //         }
    //         if(grid.listProperty == null){
    //             return;
    //         }

    //         /**
    //          * Important!
    //          * update custom grids only if are modified records or it was reordered via drag/drop
    //          */
    //         if (store.getModifiedRecords().length > 0 || store.isReordered) {
    //             store.each(function (record) {
    //                 if (record.get('markedForDelete')) {
    //                     record.drop();
    //                 }
    //             });
    //             store.isReordered = undefined;
    //             vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
    //             // store.commitChanges();
    //         }
    //     });

    //     // check if Block All access rule exists and is enabled
    //     var blockAllRule = me.isBlockAllAccessRuleEnabled(vm.get('settings'));
    //     if (!blockAllRule) {
    //         Ext.MessageBox.alert("Failed".t(), "The Block All rule in Access Rules is missing. This is dangerous and not allowed! Refer to the documentation.".t());
    //         return;
    //     } else {
    //         if (!blockAllRule.enabled) {
    //             Ext.MessageBox.alert("Failed".t(), "The Block All rule in Access Rules is disabled. This is dangerous and not allowed! Refer to the documentation.".t());
    //             return;
    //         }
    //     }

    //     // check to see if any access rules have been added/removed
    //     var accessRulesLength = me.getAccessRulesCount(vm.get('settings'));
    //     if ( accessRulesLength != vm.get('accessRulesLength') ) {
    //         Ext.Msg.show({
    //             title: 'Access Rules changed!'.t(),
    //             msg: "The Access Rules have been changed!".t() + "<br/><br/>" +
    //                 "Improperly configuring the Access Rules can be very dangerous.".t() + "<br/>" +
    //                 "Read the documentation for more details.".t() + "<br/><br/>" +
    //                 "Do you want to continue?".t(),
    //             buttons: Ext.Msg.YESNO,
    //             fn: function(btnId) {
    //                 if (btnId === 'yes') {
    //                     vm.set('accessRulesLength', accessRulesLength); // set this so it doesnt warning again
    //                     me.saveSettings(); // start over
    //                     return;
    //                 }
    //                 else {
    //                     view.setLoading(false);
    //                     return;
    //                 }
    //             },
    //             animEl: 'elId',
    //             icon: Ext.MessageBox.QUESTION
    //         });
    //         return;
    //     }

    //     // check if Allow SSH access rule has been enabled
    //     var accessRulesSshEnabled = me.isSshAccessRuleEnabled(vm.get('settings'));
    //     if ( accessRulesSshEnabled && !vm.get('accessRulesSshEnabled') ) {
    //         Ext.Msg.show({
    //             title: 'SSH Access Enabled!'.t(),
    //             msg: "The 'Allow SSH' rule in Access Rules has been enabled!".t() + "<br/><br/>" +
    //                 "If the admin/root password is poorly chosen, enabling SSH is very dangerous.".t() + "<br/><br/>" +
    //                 "Any changes made via the command line can be dangerous and destructive.".t() + "<br/>" +
    //                 "Any changes made via the command line are not supported and can limit your support options.".t() + "<br/><br/>" +
    //                 "Do you want to continue?".t(),
    //             buttons: Ext.Msg.YESNO,
    //             fn: function(btnId) {
    //                 if (btnId === 'yes') {
    //                     vm.set('accessRulesSshEnabled', true); // set this so it doesnt warning again
    //                     me.saveSettings(); // start over
    //                     return;
    //                 }
    //                 else {
    //                     view.setLoading(false);
    //                     return;
    //                 }
    //             },
    //             animEl: 'elId',
    //             icon: Ext.MessageBox.QUESTION
    //         });
    //         return;
    //     }

    //     var qosOK = true;
    //     if( vm.get('settings').qosSettings.qosEnabled === true ){
    //         vm.get('wanInterfaces').each( function(intf){
    //             if( intf.get('downloadBandwidthKbps') == null ||
    //                 intf.get('downloadBandwidthKbps') == 0 ||
    //                 intf.get('uploadBandwidthKbps') == null ||
    //                 intf.get('uploadBandwidthKbps') == 0) {
    //                 qosOK = false;
    //             }
    //         });
    //         if(!qosOK){
    //             Ext.MessageBox.alert(
    //                 "Failed".t(),
    //                 "QoS is Enabled. Please set valid Download Bandwidth and Upload Bandwidth limits in WAN Bandwidth for all WAN interfaces.".t()
    //             );
    //             view.setLoading(false);
    //             return;
    //         }

    //     }

    //     me.setNetworkSettings();
    // },

    // setNetworkSettings: function() {
    //     var view = this.getView();
    //     var vm = this.getViewModel();
    //     var me = this;

    //     view.setLoading(true);
    //     Rpc.asyncData('rpc.networkManager.setNetworkSettings', vm.get('settings'))
    //     .then(function(result) {
    //         me.loadSettings();
    //         Util.successToast('Network'.t() + ' settings saved!');
    //         Ext.fireEvent('resetfields', view);
    //     }).always(function () {
    //         view.setLoading(false);
    //     });
    // },

    // isSshAccessRuleEnabled: function(networkSettings) {
    //     var accessRulesSshEnabled = false;
    //     if(networkSettings.accessRules && networkSettings.accessRules.list) {
    //         var i;
    //         for( i=0; i<networkSettings.accessRules.list.length ; i++ ) {
    //             var rule = networkSettings.accessRules.list[i];
    //             if ( rule.description == "Allow SSH" ) {
    //                 accessRulesSshEnabled = rule.enabled;
    //                 break;
    //             }
    //         }
    //     }
    //     return accessRulesSshEnabled;
    // },

    // getAccessRulesCount: function(networkSettings) {
    //     if(networkSettings.accessRules && networkSettings.accessRules.list) {
    //         return networkSettings.accessRules.list.length;
    //     }
    //     return 0;
    // },

    // isBlockAllAccessRuleEnabled: function(networkSettings) {
    //     var rule, blockAllRule = null;
    //     if(networkSettings.accessRules && networkSettings.accessRules.list) {
    //         var i;
    //         for( i=0; i<networkSettings.accessRules.list.length ; i++ ) {
    //             rule = networkSettings.accessRules.list[i];
    //             if ( rule.description === "Block All" ) {
    //                 blockAllRule = rule;
    //             }
    //         }
    //     }
    //     return blockAllRule;
    // },

    // onInterfaces: function () {
    //     var me = this,
    //         vm = this.getViewModel();

    //     vm.bind('{interfacesGrid.selection}', function(interface) {
    //         if (interface) {
    //             me.getInterfaceStatus();
    //             me.getInterfaceArp();
    //         }
    //     });
    // },

    // interfacesGridReconfigure: function(){
    //     this.getView().down('#interfacesGrid').getSelectionModel().select(0);
    // },

    // getInterfaceStatus: function () {
    //     var statusView = this.getView().down('#interfaceStatus'),
    //         vm = this.getViewModel(),
    //         symbolicDev = vm.get('interfacesGrid.selection').get('symbolicDev'),
    //         command1 = 'ifconfig ' + symbolicDev + ' | grep "Link\\|packets" | grep -v inet6 | tr "\\n" " " | tr -s " " ',
    //         command2 = 'ifconfig ' + symbolicDev + ' | grep "inet addr" | tr -s " " | cut -c 7- ',
    //         command3 = 'ifconfig ' + symbolicDev + ' | grep inet6 | grep Global | cut -d" " -f 13',
    //         stat = {
    //             device: symbolicDev,
    //             macAddress: null,
    //             address: null,
    //             mask: null,
    //             v6Addr: null,
    //             rxpkts: null,
    //             rxerr: null,
    //             rxdrop: null,
    //             txpkts: null,
    //             txerr: null,
    //             txdrop: null
    //         };
    //     statusView.setLoading(true);
    //     Rpc.asyncData('rpc.execManager.execOutput', command1).then(function (result) {
    //         if (Ext.isEmpty(result) || result.search('Device not found') >= 0) {
    //             statusView.setLoading(false);
    //             return;
    //         }
    //         var lineparts = result.split(' ');
    //         if (result.search('Ethernet') >= 0) {
    //             Ext.apply(stat, {
    //                 macAddress: lineparts[4],
    //                 rxpkts: lineparts[6].split(':')[1],
    //                 rxerr: lineparts[7].split(':')[1],
    //                 rxdrop: lineparts[8].split(':')[1],
    //                 txpkts: lineparts[12].split(':')[1],
    //                 txerr: lineparts[13].split(':')[1],
    //                 txdrop: lineparts[14].split(':')[1]
    //             });
    //         }
    //         if (result.search('Point-to-Point') >= 0) {
    //             Ext.apply(stat, {
    //                 macAddress: '',
    //                 rxpkts: lineparts[5].split(':')[1],
    //                 rxerr: lineparts[6].split(':')[1],
    //                 rxdrop: lineparts[7].split(':')[1],
    //                 txpkts: lineparts[11].split(':')[1],
    //                 txerr: lineparts[12].split(':')[1],
    //                 txdrop: lineparts[13].split(':')[1]
    //             });
    //         }

    //         Rpc.asyncData('rpc.execManager.execOutput', command2).then(function (result) {
    //             if (Ext.isEmpty(result)) {
    //                 statusView.setLoading(false);
    //                 return;
    //             }

    //             var linep = result.split(' ');
    //             Ext.apply(stat, {
    //                 address: linep[0].split(':')[1],
    //                 mask: linep[2].split(':')[1]
    //             });

    //             Rpc.asyncData('rpc.execManager.execOutput', command3).then(function (result) {
    //                 statusView.setLoading(false);
    //                 Ext.apply(stat, {
    //                     v6Addr: result
    //                 });
    //                 vm.set('siStatus', stat);
    //             });
    //         });
    //     });
    // },

    // getInterfaceArp: function () {
    //     var vm = this.getViewModel(),
    //         arpView = this.getView().down('#interfaceArp'),
    //         symbolicDev = vm.get('interfacesGrid.selection').get('symbolicDev'),
    //         arpCommand = 'arp -n -i ' + symbolicDev + ' | grep -v incomplete | tail -n +2';

    //     arpView.setLoading(true);
    //     Rpc.asyncData('rpc.execManager.execOutput', arpCommand).then(function (result) {
    //         var lines = Ext.isEmpty(result) ? []: result.split('\n');
    //         var lparts, connections = [];
    //         for (var i = 0 ; i < lines.length; i++ ) {
    //             if (!Ext.isEmpty(lines[i])) {
    //                 lparts = lines[i].split(/\s+/);
    //                 connections.push({
    //                     address: lparts[0],
    //                     type: lparts[1],
    //                     macAddress: lparts[2]
    //                 });
    //             }
    //         }
    //         vm.set('siArp', connections);
    //         arpView.setLoading(false);
    //     });
    // },

    // setPortForwardSimples: function(){
    //     /*
    //      * Override the simple flag if protocol and port are not defined.
    //      */
    //     var vm = this.getViewModel(),
    //         portForwardRules = vm.get('settings.portForwardRules');

    //     portForwardRules.list.forEach(function(portForwardRule){
    //         if( portForwardRule.conditions &&
    //             portForwardRule.conditions.list){
    //             var protocol = null;
    //             var port = null;
    //             portForwardRule.conditions.list.forEach(function(condition){
    //                 if(condition.conditionType == 'PROTOCOL'){
    //                     protocol = condition;
    //                 }else if(condition.conditionType == 'DST_PORT'){
    //                     port = condition;
    //                 }
    //             });
    //             portForwardRule.simple = ( protocol != null && port != null ) ? true : false;
    //         }
    //     });
    // },

    // setPortForwardWarnings: function () {
    //     var vm = this.getViewModel(),
    //         interfaces = vm.get('settings.interfaces.list'), intf, i,
    //         portForwardWarningsHtml = [];

    //     for (i = 0; i < interfaces.length; i += 1) {
    //         intf = interfaces[i];
    //         if (intf.v4Address) {
    //             portForwardWarningsHtml.push(Ext.String.format('<b>{0}:{1}</b> ', intf.v4Address, vm.get('settings.httpsPort')) + 'for HTTPS services.'.t() + '<br/>');
    //         }
    //     }
    //     for (i = 0; i < interfaces.length ; i += 1) {
    //         intf = interfaces[i];
    //         if (intf.v4Address && !intf.isWan) {
    //             portForwardWarningsHtml.push(Ext.String.format('<b>{0}:{1}</b> ', intf.v4Address, vm.get('settings.httpPort')) + 'for HTTP services.'.t() + '<br/>');
    //         }
    //     }
    //     for (i = 0; i < interfaces.length ; i += 1) {
    //         intf = interfaces[i];
    //         if (intf.v4Address && intf.isWan) {
    //             for (var j = 0; j < interfaces.length; j++) {
    //                 var sub_intf = interfaces[j];
    //                 if (sub_intf.configType === 'BRIDGED' && sub_intf.bridgedTo === intf.interfaceId) {
    //                     portForwardWarningsHtml.push(Ext.String.format('<b>{0}:{1}</b> ', intf.v4Address, vm.get('settings.httpPort')) +
    //                                                     'on'.t() +
    //                                                     Ext.String.format(' {2} ', sub_intf.name) +
    //                                                     'for HTTP services.'.t() + '<br/>');
    //                 }
    //             }
    //         }
    //     }
    //     vm.set('portForwardWarnings', portForwardWarningsHtml.join(''));
    // },

    // refreshRoutes: function (cmp) {
    //     var view = cmp.isXType('button') ? cmp.up('panel') : cmp;
    //     view.down('textarea').setValue('');
    //     view.setLoading(true);
    //     Rpc.asyncData('rpc.execManager.exec', '/usr/share/untangle/bin/ut-routedump.sh')
    //         .then(function (result) {
    //             view.down('textarea').setValue(result.output);
    //         }).always(function () {
    //             view.setLoading(false);
    //         });
    // },

    // refreshQosStatistics: function (cmp) {
    //     var view = cmp.isXType('button') ? cmp.up('grid') : cmp,
    //         vm = this.getViewModel();
    //     view.setLoading(true);
    //     Rpc.asyncData('rpc.execManager.execOutput', '/usr/share/untangle-netd/bin/qos-status.py')
    //         .then(function (result) {
    //             var list = [];
    //             try {
    //                 list = eval(result);
    //                 list.forEach(function(entry){
    //                     entry['sent'] = parseInt(entry['sent'],10);
    //                 });
    //                 vm.set('qosStatistics', Ext.create('Ext.data.Store', {
    //                     fields: [
    //                         'tokens',
    //                         'priority',
    //                         'rate',
    //                         'burst',
    //                         'ctokens',
    //                         'interface_name',
    //                         'sent'
    //                     ],
    //                     sorters: [{
    //                         property: 'interface_name',
    //                         direction: 'ASC'
    //                     }],
    //                     data: list
    //                 }) );
    //             } catch (e) {
    //                 Util.handleException('Unable to get QoS statistics');
    //                 console.error('Could not execute /usr/share/untangle-netd/bin/qos-status.py output: ', result, e);
    //             }
    //         }).always(function () {
    //             view.setLoading(false);
    //         });
    // },

    // refreshUpnpStatus: function (cmp) {
    //     var view = cmp.isXType('button') ? cmp.up('grid') : cmp;
    //     var vm = this.getViewModel();
    //     Rpc.asyncData('rpc.networkManager.getUpnpManager', '--status', '')
    //         .then(function(result) {
    //             vm.set('upnpStatus', Ext.create('Ext.data.Store', {
    //                 fields: [
    //                     'upnp_client_ip_address',
    //                     'upnp_destination_port',
    //                     'upnp_protocol',
    //                     'upnp_client_port',
    //                     'bytes'
    //                 ],
    //                 sorters: [{
    //                     property: 'upnp_client_ip_address',
    //                     direction: 'ASC'
    //                 }],
    //                 data: Ext.decode(result)["active"]
    //             }) );
    //         }).always(function () {
    //             view.setLoading(false);
    //         });
    // },

    // deleteUpnp: function(view, u1, u2, u3, u4, record){
    //     var me = this;
    //     Rpc.asyncData('rpc.networkManager.getUpnpManager', '--delete', "'" + Ext.encode(record.data) + "'")
    //         .then(function(result) {
    //             me.refreshUpnpStatus(view);
    //         });
    // },

    // refreshDhcpLeases: function (cmp) {
    //     var view = cmp.isXType('button') ? cmp.up('grid') : cmp;
    //     view.setLoading(true);
    //     Rpc.asyncData('rpc.execManager.execOutput', 'cat /var/lib/misc/dnsmasq.leases')
    //         .then(function (result) {
    //             var lines = result.split('\n'),
    //                 leases = [], lineparts, i;
    //             for (i = 0 ; i < lines.length ; i++) {
    //                 if (lines[i] === null || lines[i] === '' ) {
    //                     continue;
    //                 }
    //                 lineparts = lines[i].split(/\s+/);
    //                 if (lineparts.length == 5 ) {
    //                     leases.push({
    //                         date: lineparts[0],
    //                         macAddress: lineparts[1],
    //                         address: lineparts[2],
    //                         hostname: lineparts[3],
    //                         clientId: lineparts[4]
    //                     });
    //                 }
    //             }
    //             view.getStore().loadData(leases);
    //         }).always(function () {
    //             view.setLoading(false);
    //         });

    // },
    // addStaticDhcpLease: function (view, rowIndex, colIndex, item, e, record) {
    //     var me = this; staticDhcpGrid = me.getView().down('#dhcpEntries');
    //     var newDhcpEntry = {
    //         macAddress: record.get('macAddress'),
    //         address: record.get('address'),
    //         description: record.get('hostname'),
    //         javaClass: 'com.untangle.uvm.network.DhcpStaticEntry'
    //     };
    //     staticDhcpGrid.getStore().add(newDhcpEntry);
    // },

    // // Network Tests
    // networkTestRender: function (view) {
    //     view.down('form').insert(0, view.commandFields);
    // },
    // runTest: function (btn) {
    //     var v = btn.up('networktest'),
    //         vm = v.getViewModel(),
    //         output = v.down('textarea'),
    //         text = [],
    //         me = this;

    //     btn.setDisabled(true);

    //     text.push(output.getValue());
    //     text.push('' + (new Date()) + ' - ' + 'Test Started'.t() + '\n');

    //     rpc.execManager.execEvil(function (result, ex) {
    //         if (ex) { console.error(ex); Util.handleException(ex); return; }
    //         // Save the filename.
    //         me.readOutput(result, text, output, btn, vm);
    //     }, v.getViewModel().get('command'));

    // },
    // readOutput: function (resultReader, text, output, btn, vm) {
    //     var me = this;

    //     if (!resultReader) {
    //         return;
    //     }

    //     resultReader.readFromOutput(function (res, ex) {
    //         if (ex) { console.error(ex); Util.handleException(ex); return; }
    //         if (res !== null) {
    //             text.push(res);
    //             Ext.Function.defer(me.readOutput, 1000, me, [resultReader, text, output, btn, vm]);
    //         } else {
    //             btn.setDisabled(false);
    //             text.push('' + (new Date()) + ' - ' + 'Test Completed'.t());
    //             text.push('\n\n--------------------------------------------------------\n\n');
    //             if(vm.get('exportRunFilename') !== '' ){
    //                 vm.set('exportFilename', vm.get('exportRunFilename'));
    //                 vm.set('exportRunFilename', '');
    //             }
    //         }
    //         output.setValue(text.join(''));
    //         output.getEl().down('textarea').dom.scrollTop = 99999;
    //     });
    // },

    // clearOutput: function (btn) {
    //     var me = this,
    //         vm = btn.up('networktest').getViewModel();

    //     var v = btn.up('networktest');
    //     v.down('textarea').setValue('');
    //     vm.set('exportRunFilename', '');
    //     vm.set('exportFilename', '');
    // },

    // exportOutput: function(btn){
    //     var vm = btn.up('networktest').getViewModel();

    //     Ext.MessageBox.wait( "Exporting Packet Dump...".t(), "Please wait".t());
    //     var downloadForm = document.getElementById('downloadForm');
    //     downloadForm["type"].value = "NetworkTestExport";
    //     downloadForm["arg1"].value = vm.get('exportFilename');
    //     downloadForm.submit();
    //     Ext.MessageBox.hide();
    // },

    // wirelessChannelsMap: {
    //     '-1':  [-1,  'Automatic 2.4 GHz'.t()],
    //     '-2':  [-2,  'Automatic 5 GHz'.t()],
    //     '1':   [1,   '1 - 2.412 GHz'.t()],
    //     '2':   [2,   '2 - 2.417 GHz'.t()],
    //     '3':   [3,   '3 - 2.422 GHz'.t()],
    //     '4':   [4,   '4 - 2.427 GHz'.t()],
    //     '5':   [5,   '5 - 2.432 GHz'.t()],
    //     '6':   [6,   '6 - 2.437 GHz'.t()],
    //     '7':   [7,   '7 - 2.442 GHz'.t()],
    //     '8':   [8,   '8 - 2.447 GHz'.t()],
    //     '9':   [9,   '9 - 2.452 GHz'.t()],
    //     '10':  [10,  '10 - 2.457 GHz'.t()],
    //     '11':  [11,  '11 - 2.462 GHz'.t()],
    //     '12':  [12,  '12 - 2.467 GHz'.t()],
    //     '13':  [13,  '13 - 2.472 GHz'.t()],
    //     '14':  [14,  '14 - 2.484 GHz'.t()],
    //     '36':  [36,  '36 - 5.180 GHz'.t()],
    //     '40':  [40,  '40 - 5.200 GHz'.t()],
    //     '44':  [44,  '44 - 5.220 GHz'.t()],
    //     '48':  [48,  '48 - 5.240 GHz'.t()],
    //     '52':  [52,  '52 - 5.260 GHz'.t()],
    //     '56':  [56,  '56 - 5.280 GHz'.t()],
    //     '60':  [60,  '60 - 5.300 GHz'.t()],
    //     '64':  [64,  '64 - 5.320 GHz'.t()],
    //     '100': [100, '100 - 5.500 GHz'.t()],
    //     '104': [104, '104 - 5.520 GHz'.t()],
    //     '108': [108, '108 - 5.540 GHz'.t()],
    //     '112': [112, '112 - 5.560 GHz'.t()],
    //     '116': [116, '116 - 5.580 GHz'.t()],
    //     '120': [120, '120 - 5.600 GHz'.t()],
    //     '124': [124, '124 - 5.620 GHz'.t()],
    //     '128': [128, '128 - 5.640 GHz'.t()],
    //     '132': [132, '132 - 5.660 GHz'.t()],
    //     '136': [136, '136 - 5.680 GHz'.t()],
    //     '140': [140, '140 - 5.700 GHz'.t()],
    //     '144': [144, '144 - 5.720 GHz'.t()],
    //     '149': [149, '149 - 5.745 GHz'.t()],
    //     '153': [153, '153 - 5.765 GHz'.t()],
    //     '157': [157, '157 - 5.785 GHz'.t()],
    //     '161': [161, '161 - 5.805 GHz'.t()],
    //     '165': [165, '165 - 5.825 GH'.t()]
    // },

    // editInterface: function (cmp, rowIndex, colIndex, item, e, record) {
    //     var me = this;

    //     if (cmp.getXType() === 'button') {
    //         // means adding a VLAN interface
    //         me.editIntf = Ext.create('Ung.model.Interface', {
    //             isVlanInterface: true,
    //             isWirelessInterface: false,
    //             vlanTag: 1,
    //             configType: 'ADDRESSED',
    //             v4ConfigType: 'STATIC',
    //             v6ConfigType: 'DISABLED'
    //         });
    //     } else {
    //         // otherwise means editing existing interface
    //         me.editIntf = record;
    //     }

    //     var configTypesArr,
    //         configTypesRadios = {
    //             ADDRESSED: { boxLabel: '<i class="fa fa-file-text fa-gray"></i> <strong>' + 'Addressed'.t() + '</strong>', inputValue: 'ADDRESSED' },
    //             BRIDGED: { boxLabel: '<i class="fa fa-link fa-gray"></i> <strong>' + 'Bridged'.t() + '</strong>', inputValue: 'BRIDGED' },
    //             DISABLED: { boxLabel: '<i class="fa fa-ban fa-gray"></i> <strong>' + 'Disabled'.t() + '</strong>', inputValue: 'DISABLED' }
    //         }, configTypes = [];


    //     configTypesArr = me.editIntf.get('supportedConfigTypes') || ['ADDRESSED', 'BRIDGED', 'DISABLED'];

    //     Ext.Array.each(configTypesArr, function (confType) {
    //         configTypes.push(configTypesRadios[confType]);
    //     });

    //     // if non-wan set v4, v6 configs to STATIC
    //     if (!me.editIntf.get('isWan')) {
    //         me.editIntf.set('v4ConfigType', 'STATIC');
    //         me.editIntf.set('v6ConfigType', 'STATIC');
    //     }

    //     // fix if missing or null lists (e.g. dhcpOptions)
    //     if (!me.editIntf.get('dhcpOptions')) {
    //         me.editIntf.set('dhcpOptions', { javaClass: 'java.util.LinkedList', list: [] });
    //     }

    //     if (!me.editIntf.get('v4Aliases')) {
    //         me.editIntf.set('v4Aliases', { javaClass: 'java.util.LinkedList', list: [] });
    //     }

    //     if (!me.editIntf.get('v6Aliases')) {
    //         me.editIntf.set('v6Aliases', { javaClass: 'java.util.LinkedList', list: [] });
    //     }

    //     if (!me.editIntf.get('vrrpAliases')) {
    //         me.editIntf.set('vrrpAliases', { javaClass: 'java.util.LinkedList', list: [] });
    //     }


    //     me.dialog = me.getView().add({
    //         xtype: 'config.interface',
    //         configTypesRadios: configTypes, // holds the radio buttons based on interface supported config types
    //         title: me.editIntf.get('interfaceId') > 0 ? 'Edit Interface'.t() : 'Add VLAN Interface'.t(),
    //         viewModel: {
    //             data: {
    //                 // intf: btn.getWidgetRecord().copy(null)
    //                 intf: me.editIntf,
    //                 wirelessChannelsList: [],
    //                 vrrpmaster: false
    //             },
    //             formulas: {
    //                 isAddressed: function (get) { return get('intf.configType') === 'ADDRESSED'; },
    //                 isDisabled: function (get) { return get('intf.configType') === 'DISABLED'; },
    //                 isBridged: function (get) { return get('intf.configType') === 'BRIDGED'; },
    //                 isStaticv4: function (get) { return get('intf.v4ConfigType') === 'STATIC'; },
    //                 isAutov4: function (get) { return get('intf.v4ConfigType') === 'AUTO'; },
    //                 isPPPOEv4: function (get) { return get('intf.v4ConfigType') === 'PPPOE'; },
    //                 isDisabledv6: function (get) { return get('intf.v6ConfigType') === 'DISABLED'; },
    //                 isStaticv6: function (get) { return get('intf.v6ConfigType') === 'STATIC'; },
    //                 isAutov6: function (get) { return get('intf.v6ConfigType') === 'AUTO'; },
    //                 showRouterWarning: function (get) { return get('intf.v6StaticPrefixLength') !== 64; },
    //                 showWireless: function (get) { return get('intf.isWirelessInterface') && get('intf.configType') !== 'DISABLED'; },
    //                 showWirelessPassword: function (get) { return get('intf.wirelessEncryption') !== 'NONE' && get('intf.wirelessEncryption') !== null; }
    //             },
    //             stores: {
    //                 v4Aliases: { data: '{intf.v4Aliases.list}' },
    //                 v6Aliases: { data: '{intf.v6Aliases.list}' },
    //                 dhcpOptions: { data: '{intf.dhcpOptions.list}' },
    //                 vrrpAliases: { data: '{intf.vrrpAliases.list}' }
    //             }
    //         }
    //     });
    //     me.dialog.show();

    //     // wireless channels
    //     var wirelessChannelsArr = [];
    //     if (me.editIntf.get('isWirelessInterface')) {
    //         Rpc.asyncData('rpc.networkManager.getWirelessChannels', me.editIntf.get('systemDev'))
    //             .then(function(result) {
    //                 if (result && result.list) {
    //                     Ext.Array.each(result.list, function (ch) {
    //                         if (me.wirelessChannelsMap[ch]) {
    //                             wirelessChannelsArr.push(me.wirelessChannelsMap[ch]);
    //                         }
    //                     });
    //                     me.dialog.getViewModel().set('wirelessChannelsList', wirelessChannelsArr);
    //                 }
    //             }, function (ex) {
    //                 Util.handleException(ex);
    //             });
    //     }

    //     // check VRRP master
    //     if (me.editIntf.get('vrrpEnabled') && me.editIntf.get('interfaceId') > 0) {
    //         Rpc.asyncData('rpc.networkManager.isVrrpMaster', me.editIntf.get('interfaceId'))
    //             .then(function(result) {
    //                 me.dialog.getViewModel().set('vrrpmaster', result);
    //             }, function (ex) {
    //                 Util.handleException(ex);
    //             });
    //     }
    // },
    // cancelEdit: function (button) {
    //     this.editIntf.reject();
    //     this.dialog.close();
    // },

    // doneEdit: function (btn) {
    //     var me = this,
    //         view = me.getView(),
    //         dialogVm = me.dialog.getViewModel(),
    //         intf = dialogVm.get('intf');

    //     if (!Util.validateForms(view)) {
    //         return;
    //     }

    //     this.dialog.query('ungrid').forEach(function (grid) {
    //         var store = grid.getStore();
    //         if (store.getModifiedRecords().length > 0 ||
    //             store.getNewRecords().length > 0 ||
    //             store.getRemovedRecords().length > 0) {
    //             store.each(function (record) {
    //                 if (record.get('markedForDelete')) {
    //                     record.drop();
    //                 }
    //             });
    //             if (grid.listProperty) {
    //                 // console.log(intf);
    //                 intf.set(grid.listProperty, {
    //                     javaClass: 'java.util.LinkedList',
    //                     list: Ext.Array.pluck(store.getRange(), 'data')
    //                 });
    //             }
    //         }
    //     });

    //     // new VLAN interface
    //     if (intf.get('interfaceId') === -1) {
    //         me.lookup('interfacesGrid').getStore().add(intf);
    //     }

    //     this.dialog.close();
    // },

    // onBridgedInteface: function( combo ){
    //     var me = this,
    //         vm = me.getViewModel();

    //     var record = combo.up('window').getViewModel().get('intf');

    //     var fields = [];
    //     vm.get('settings').interfaces.list.forEach( function(interface){
    //         if( ( interface.interfaceId == record.get('interfaceId') ) ||
    //             ( interface.bridged !== false ) ||
    //             ( interface.disabled !== false ) ||
    //             ( interface.configType != 'ADDRESSED') ){
    //             return;
    //         }
    //         fields.push([interface.interfaceId, interface.name]);
    //     });

    //     combo.setStore(Ext.create('Ext.data.ArrayStore', {
    //         fields: [ 'id', 'name' ],
    //         sorters: [{
    //             property: 'name',
    //             direction: 'ASC'
    //         }],
    //         data: fields
    //     }));
    // },

    // // used to set available parent interfaces
    // onParentInterface: function (combo) {
    //     var data = [];
    //     var record = combo.up('window').getViewModel().get('intf');
    //     Ext.Array.each(rpc.networkSettings.interfaces.list, function (intf) {
    //         if (intf.interfaceId !== record.get('interfaceId') && !intf.isVlanInterface) {
    //             data.push([intf.interfaceId, intf.name]);
    //         }
    //     });
    //     combo.setStore(data);
    // },

    // onRenewDhcpLease: function () {
    //     var me = this,
    //         dialogVm = me.dialog.getViewModel(),
    //         intf = dialogVm.get('intf');

    //     me.dialog.setLoading(true);
    //     Ext.Deferred.sequence([
    //         Rpc.asyncPromise('rpc.networkManager.renewDhcpLease', intf.get('interfaceId')),
    //         Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
    //     ], this).then(function (result) {
    //         var intfStatus = Ext.Array.findBy(result[1].list, function (intfSt) {
    //             return intfSt.interfaceId === intf.get('interfaceId');
    //         });

    //         if (intfStatus != null) {
    //             delete intfStatus.javaClass;
    //             delete intfStatus.interfaceId;

    //             intf.set(intfStatus); // update interface with the new values
    //         }
    //     }, function (ex) {
    //         console.error(ex);
    //         Util.handleException(ex);
    //     }).always(function () {
    //         me.dialog.setLoading(false);
    //     });
    // },

    // remapInterfaces: function () {
    //     var me = this, vm = this.getViewModel(), physicalDevsStore = [], intfOrderArr = [];
    //     // create array store of physicalDevs for the editor combo
    //     // create an array with initial interfaces order, used for drag&drop remapping
    //     vm.get('devInterfaces').each(function (intf) {
    //         physicalDevsStore.push([intf.get('physicalDev'), intf.get('physicalDev')]);
    //         intfOrderArr.push(intf.copy(null));
    //     });


    //     me.remapDialog = me.getView().add({
    //         xtype: 'window',
    //         title: 'Remap Interfaces'.t(),
    //         modal: true,
    //         width: 900,
    //         height: 400,
    //         layout: 'fit',
    //         closable: false,
    //         onEsc: Ext.emptyFn,
    //         items: [{
    //             xtype: 'ungrid',
    //             border: false,
    //             bodyBorder: false,
    //             bind: '{devInterfaces}',
    //             plugins: {
    //                 ptype: 'cellediting',
    //                 clicksToEdit: 1
    //             },
    //             viewConfig: {
    //                 plugins: {
    //                     ptype: 'gridviewdragdrop',
    //                     dragText: 'Drag and drop to reorganize'.t(),
    //                     dragZone: {
    //                         onBeforeDrag: function (data, e) {
    //                             return Ext.get(e.target).hasCls('fa-arrows');
    //                         }
    //                     }
    //                 },
    //                 listeners: {
    //                     drop: function (app, data, overModel, dropPosition, eOpts) {
    //                         var i = 0;
    //                         vm.get('devInterfaces').each(function( currentRow ) {
    //                             var intf = intfOrderArr[i];
    //                             currentRow.set({
    //                                 interfaceId: intf.get('interfaceId'),
    //                                 name: intf.get('name')
    //                             });
    //                             i++;
    //                         });
    //                     }
    //                 }
    //             },
    //             enableColumnHide: false,
    //             sortableColumns: false,
    //             columns: [{
    //                 dataIndex: 'connected',
    //                 width: Renderer.iconWidth,
    //                 align: 'center',
    //                 resizable: false,
    //                 sortable: false,
    //                 menuEnabled: false,
    //                 renderer: Ung.config.network.MainController.connectedIconRenderer
    //             }, {
    //                 header: 'Name'.t(),
    //                 dataIndex: 'name',
    //                 width: Renderer.messageWidth,
    //             },
    //             Column.reorder,
    //             {
    //                 header: 'Device'.t(),
    //                 dataIndex: 'deviceName',
    //                 width: Renderer.idWidth,
    //                 editor: {
    //                     xtype: 'combo',
    //                     store: physicalDevsStore,
    //                     editable: false,
    //                     valueField: 'physicalDev',
    //                     displayField: 'physicalDev',
    //                     queryMode: 'local',
    //                     listeners: {
    //                         change: 'setMapInterfaces'
    //                     }
    //                 }
    //             }, {
    //                 header: 'Speed'.t(),
    //                 dataIndex: 'mbit',
    //                 width: Renderer.sizeWidth,
    //             }, {
    //                 header: 'Duplex'.t(),
    //                 dataIndex: 'duplex',
    //                 width: Renderer.idWidth,
    //                 renderer: function (value) {
    //                     return (value === 'FULL_DUPLEX') ? 'full-duplex'.t() : (value === 'HALF_DUPLEX') ? 'half-duplex'.t() : 'unknown'.t();
    //                 }
    //             }, {
    //                 header: 'Vendor'.t(),
    //                 dataIndex: 'vendor',
    //                 width: Renderer.messageWidth,
    //                 flex: 1
    //             }, {
    //                 header: 'MAC Address'.t(),
    //                 dataIndex: 'macAddress',
    //                 width: Renderer.macWidth,
    //                 renderer: function(value, metadata, record, rowIndex, colIndex, store, view) {
    //                     var text = '';
    //                     if (value && value.length > 0) {
    //                         // Build the link for the mac address
    //                         text = '<a target="_blank" href="http://standards.ieee.org/cgi-bin/ouisearch?' +
    //                         value.substring(0, 8).replace(/:/g, '') + '">' + value + '</a>';
    //                     }
    //                     return text;
    //                 }
    //             }],
    //             dockedItems: [{
    //                 xtype: 'component',
    //                 ui: 'navigation',
    //                 dock: 'top',
    //                 padding: 10,
    //                 border: false,
    //                 html: '<strong>' + 'How to map Devices with Interfaces'.t() + '</strong><br/><br/>' +
    //                     '<b>Method 1:</b> <b>Drag and Drop</b> the Device to the desired Interface<br/><b>Method 2:</b> <b>Click on a Device</b> to open a combo and choose the desired Device from a list. When another Device is selected the 2 Devices are switched.'.t()
    //             }]
    //         }],
    //         fbar: [{
    //             text: 'Cancel'.t(),
    //             iconCls: 'fa fa-ban',
    //             handler: function (btn) {
    //                 me.loadSettings();
    //                 btn.up('window').close();
    //             }
    //         }, {
    //             text: 'Done'.t(),
    //             iconCls: 'fa fa-check',
    //             handler: function (btn) {
    //                 btn.up('window').close();
    //             }
    //         }]
    //     });
    //     me.remapDialog.show();
    // },

    // setMapInterfaces: function (combo, newValue, oldValue) {
    //     var vm = this.getViewModel(), sourceRecord = null, targetRecord = null;

    //     vm.get('devInterfaces').each( function( currentRow ) {
    //         if (oldValue === currentRow.get('physicalDev')) {
    //             sourceRecord = currentRow;
    //         } else if (newValue === currentRow.get('physicalDev')) {
    //             targetRecord = currentRow;
    //         }
    //     });
    //     // make sure sourceRecord & targetRecord are defined
    //     if (sourceRecord === null || targetRecord === null) {
    //         return;
    //     }

    //     // clone phantom records to manipulate (switch) data properly
    //     var sourceRecordCopy = sourceRecord.copy(null),
    //         targetRecordCopy = targetRecord.copy(null);

    //     // switch data between records (interfaces) - remapping
    //     sourceRecord.set({
    //         deviceName: newValue,
    //         physicalDev: targetRecordCopy.get('physicalDev'),
    //         systemDev:   targetRecordCopy.get('systemDev'),
    //         symbolicDev: targetRecordCopy.get('symbolicDev'),
    //         macAddress:  targetRecordCopy.get('macAddress'),
    //         duplex:      targetRecordCopy.get('duplex'),
    //         vendor:      targetRecordCopy.get('vendor'),
    //         mbit:        targetRecordCopy.get('mbit'),
    //         connected:   targetRecordCopy.get('connected')
    //     });
    //     targetRecord.set({
    //         deviceName: oldValue,
    //         physicalDev: sourceRecordCopy.get('physicalDev'),
    //         systemDev:   sourceRecordCopy.get('systemDev'),
    //         symbolicDev: sourceRecordCopy.get('symbolicDev'),
    //         macAddress:  sourceRecordCopy.get('macAddress'),
    //         duplex:      sourceRecordCopy.get('duplex'),
    //         vendor:      sourceRecordCopy.get('vendor'),
    //         mbit:        sourceRecordCopy.get('mbit'),
    //         connected:   sourceRecordCopy.get('connected')
    //     });
    // },

    // // updates interfaces list from rules conditions
    // setInterfaceConditions: function () {
    //     Ext.Array.each(this.getView().query('ungrid'), function (grid) {
    //         Ext.Array.each(grid.conditions, function (cond) {
    //             if (cond.name === 'SRC_INTF' || cond.name === 'DST_INTF') {
    //                 cond.values = Util.getInterfaceList(true, true);
    //             }
    //         });
    //     });
    // },

    // statics: {
    //     connectedIconRenderer: function(value){
    //         switch (value) {
    //             case 'CONNECTED': return '<i class="fa fa-circle fa-green"></i>';
    //             case 'DISCONNECTED': return '<i class="fa fa-circle fa-gray"></i>';
    //             case 'MISSING': return '<i class="fa fa-exclamation-triangle fa-orange"></i>';
    //             default: return '<i class="fa fa-question-circle fa-gray"></i>';
    //         }
    //     },

    //     interfacetypeRenderer: function(value, meta, record){
    //         var icon_src = '/skins/common/images/intf_nic';
    //         meta.tdCls = 'intf_icon';
    //         if (record.get('isWirelessInterface')) {
    //             icon_src = '/skins/common/images/intf_wifi';
    //         }
    //         if (record.get('isVlanInterface')) {
    //             icon_src = '/skins/common/images/intf_vlan';
    //         }
    //         icon_src += record.get('configType') === 'DISABLED' ? '_disabled.png' : '.png';
    //         return '<img src="' + icon_src + '" />';
    //     },

    //     deviceRenderer: function(value, metadata, record){
    //         if (record.get('isVlanInterface')) {
    //             return record.get('systemDev');
    //         }
    //         return value;
    //     },

    //     speedRenderer: function( value ){
    //         if( value >= 1000 ){
    //             return (value / 1000) + ' ' + 'Gbit'.t();
    //         }
    //         return value + '' + 'Mbit'.t();
    //     },

    //     duplexRenderer: function(value){
    //         switch (value) {
    //             case 'FULL_DUPLEX': return 'Full-duplex'.t();
    //             case 'HALF_DUPLEX': return 'Half-duplex'.t();
    //             default: return 'Unknown'.t();
    //         }
    //     },

    //     addressedRenderer: function(value){
    //         switch (value) {
    //             case 'ADDRESSED': return 'Addressed'.t();
    //             case 'BRIDGED': return 'Bridged'.t();
    //             case 'DISABLED': return 'Disabled'.t();
    //             default: value.t();
    //         }
    //     },

    //     addressRenderer: function(value, metadata, record){
    //         return Ext.isEmpty(value) ? '' : value + '/' + record.get('v4PrefixLength');
    //     },

    //     iswanRenderer: function(value, metadata, record){
    //         return record.get('configType') === 'ADDRESSED' ? (value ? 'true'.t() : 'false'.t()) : '';
    //     },

    //     isvlanRenderer: function(value, metadata, record){
    //         return value ? 'true'.t() : 'false'.t();
    //     },

    //     connectedRenderer: function(value){
    //         switch (value) {
    //             case 'CONNECTED': return 'Connected'.t();
    //             case 'DISCONNECTED': return 'Disconnected'.t();
    //             case 'MISSING': return 'Missing'.t();
    //             default: return 'Unknown'.t();
    //         }
    //     },

    //     arpTypeRenderer: function(value){
    //         switch(value){
    //             case 'ether': return 'Ethernet'.t();
    //             default: return value;
    //         }
    //     },

    //     natTypeRenderer: function( value ){
    //         return value ? 'Auto'.t() : 'Custom'.t();
    //     },

    //     natNewSourceRenderer: function(value, metadata, record){
    //         return record.get('auto') ? '' : value;
    //     },

    //     routesNextHopRenderer: function(value, metadata, record, rowIndex, colIndex, store, view){
    //         var devMap = Util.getNextHopList(true);
    //         var intRegex = /^\d+$/;
    //         if ( intRegex.test( value ) ) {
    //             return devMap[value] ? devMap[value] : "Local interface".t();
    //         } else {
    //             return value;
    //         }
    //     },

    //     qosBandwidthRenderer: function(value){
    //         return Ext.isEmpty(value) ? 'Not set'.t() : value + ' kbps' + ' (' + value/1000 + ' Mbit' + ')';
    //     },

    //     qosPriorityRenderer: function (value) {
    //         switch (value) {
    //             case 1: return 'Very High'.t();
    //             case 2: return 'High'.t();
    //             case 3: return 'Medium'.t();
    //             case 4: return 'Low'.t();
    //             case 5: return 'Limited'.t();
    //             case 6: return 'Limited More'.t();
    //             case 7: return 'Limited Severely'.t();
    //         }
    //     },

    //     qosPriorityReserverationRenderer: function(value){
    //         return value === 0 ? 'No reservation'.t() : value + '%';
    //     },

    //     qosPriorityLimitRenderer: function(value){
    //         return value === 0 ? 'No limit'.t() : value + '%';
    //     },

    //     upnpAction: function(value){
    //         return value ? 'Allow'.t() : 'Deny'.t();
    //     },

    //     networkMediaRenderer: function(value){
    //         switch (value) {
    //             case 'AUTO': return 'Auto'.t();
    //             case 'M10000_FULL_DUPLEX': return '10000 Mbps, Full Duplex'.t();
    //             case 'M10000_HALF_DUPLEX': return '10000 Mbps, Half Duplex'.t();
    //             case 'M1000_FULL_DUPLEX': return '1000 Mbps, Full Duplex'.t();
    //             case 'M1000_HALF_DUPLEX': return '1000 Mbps, Half Duplex'.t();
    //             case 'M100_FULL_DUPLEX': return '100 Mbps, Full Duplex'.t();
    //             case 'M100_HALF_DUPLEX': return '100 Mbps, Half Duplex'.t();
    //             case 'M10_FULL_DUPLEX': return '10 Mbps, Full Duplex'.t();
    //             case 'M10_HALF_DUPLEX': return '10 Mbps, Half Duplex'.t();
    //             default: return 'Unknown'.t();
    //         }
    //     }

    // }
});