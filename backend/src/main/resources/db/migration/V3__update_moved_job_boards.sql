UPDATE core_source SET board_id = 'hubspotjobs', updated_at = now()
WHERE platform = 'greenhouse' AND board_id = 'hubspot'
  AND NOT EXISTS (SELECT 1 FROM core_source
                  WHERE platform = 'greenhouse' AND board_id = 'hubspotjobs');

UPDATE core_source SET platform = 'greenhouse', updated_at = now()
WHERE platform = 'ashby' AND board_id = 'vercel'
  AND NOT EXISTS (SELECT 1 FROM core_source
                  WHERE platform = 'greenhouse' AND board_id = 'vercel');

UPDATE core_source SET is_active = false, updated_at = now()
WHERE (platform = 'lever' AND board_id = 'atlassian')
   OR (platform = 'ashby' AND board_id = 'deel');
