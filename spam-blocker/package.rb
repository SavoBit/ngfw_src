# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
spam = BuildEnv::SRC['untangle-base-spam-blocker']

deps = [smtp['src'], spam['src']]

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spam-blocker', 'spam-blocker', deps, { 'spam-blocker-base' => spam } )
