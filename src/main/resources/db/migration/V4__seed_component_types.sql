INSERT INTO component_type (key, name, default_lifespan, unit) VALUES
                                                                   ('chain',                 'Řetěz',                       2000,  'km'),
                                                                   ('cassette',              'Kazeta',                      8000,  'km'),
                                                                   ('chainring',             'Převodník',                  10000,  'km'),
                                                                   ('brake_pads',            'Brzdové destičky',             600,  'km'),
                                                                   ('rotor',                 'Brzdový kotouč',            12000,  'km'),
                                                                   ('tires',                 'Plášť',                       3000,  'km'),
                                                                   ('rear_derailleur_cable', 'Lanko+bowden přehazovačky',   2000,  'km'),
                                                                   ('bottom_bracket',        'Středové složení',           10000,  'km'),
                                                                   ('headset',               'Hlavové složení',            15000,  'km'),
                                                                   ('fork_service',          'Servis přední vidlice',        100,  'hours'),
                                                                   ('rear_shock_service',    'Servis tlumiče',               100,  'hours'),
                                                                   ('chain_lube',            'Mazání řetězu',                150,  'km')
    ON CONFLICT (key) DO NOTHING;
