-- Normalizace a doplnění záznamů v katalogu typů komponent
-- Pozn.: Používáme UPSERT, aby bylo možné bezpečně opravit existující záznamy.


INSERT INTO component_type (key, name, default_lifespan, default_service_interval, unit) VALUES
                                                                                             ('chain', 'Řetěz', 2000, NULL, 'km'),
                                                                                             ('cassette', 'Kazeta', 8000, NULL, 'km'),
                                                                                             ('chainring', 'Převodník', 10000, NULL, 'km'),
                                                                                             ('brake_pads', 'Brzdové destičky', 600, NULL, 'km'),
                                                                                             ('brake_rotor', 'Brzdový kotouč', 12000, NULL, 'km'),
                                                                                             ('tyre', 'Plášť', 3000, NULL, 'km'),
                                                                                             ('rear_derailleur_cable', 'Lanko+bowden přehazovačky', 2000, NULL, 'km'),
                                                                                             ('bottom_bracket', 'Středové složení', 10000, NULL, 'km'),
                                                                                             ('headset', 'Hlavové složení', 15000, NULL, 'km'),
                                                                                             ('fork_service', 'Servis přední vidlice', NULL, 100, 'hours'),
                                                                                             ('shock_service', 'Servis tlumiče', NULL, 100, 'hours'),
                                                                                             ('chain_lube', 'Mazání řetězu', NULL, 150, 'km')
    ON CONFLICT (key) DO UPDATE SET
    name = EXCLUDED.name,
                             default_lifespan = EXCLUDED.default_lifespan,
                             default_service_interval = EXCLUDED.default_service_interval,
                             unit = EXCLUDED.unit;