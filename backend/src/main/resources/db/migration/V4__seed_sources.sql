-- Atlassian (the only Lever source) moved off Lever in V3, leaving the Lever
-- adapter with nothing to ingest. Seed companies with live Lever boards,
-- plus Wealthsimple (moved from Lever to Ashby) and Canadian and US scale-ups.

INSERT INTO core_source (name, platform, board_id, is_active)
VALUES
    ('Fullscript', 'lever', 'fullscript', true),
    ('Spotify', 'lever', 'spotify', true),
    ('Wealthsimple', 'ashby', 'wealthsimple', true),
    ('Cohere', 'ashby', 'cohere', true),
    ('Neo Financial', 'ashby', 'neofinancial', true),
    ('Jobber', 'ashby', 'jobber', true),
    ('Trulioo', 'ashby', 'trulioo', true),
    ('Float', 'ashby', 'float', true),
    ('1Password', 'ashby', '1password', true),
    ('PolicyMe', 'lever', 'policyme', true),
    ('Dialpad', 'greenhouse', 'dialpad', true),
    ('Mercury', 'greenhouse', 'mercury', true),
    ('Vanta', 'ashby', 'vanta', true),
    ('Hex', 'ashby', 'hex', true),
    ('Watershed', 'ashby', 'watershed', true),
    ('Persona', 'ashby', 'persona', true),
    ('Supabase', 'ashby', 'supabase', true),
    ('Modal', 'ashby', 'modal', true),
    ('Replit', 'ashby', 'replit', true),
    ('Sierra', 'ashby', 'sierra', true),
    ('Webflow', 'greenhouse', 'webflow', true),
    ('Harvey', 'ashby', 'harvey', true),
    ('Brex', 'greenhouse', 'brex', true),
    ('Lattice', 'greenhouse', 'lattice', true),
    ('PostHog', 'ashby', 'posthog', true),
    ('Zapier', 'ashby', 'zapier', true)
ON CONFLICT (platform, board_id) DO NOTHING;
